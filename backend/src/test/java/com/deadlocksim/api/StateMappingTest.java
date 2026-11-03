package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import com.deadlocksim.model.Operation;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateMappingTest {
    private SimulationEngine engine;
    private ApiServer server;
    private int boundPort;
    private HttpClient client;
    private Gson gson;

    @BeforeEach
    void setUp() throws IOException {
        engine = new SimulationEngine();
        server = new ApiServer(engine);
        boundPort = server.start();
        client = HttpClient.newHttpClient();
        gson = new Gson();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getStateWithoutScenarioReturns409NoScenario() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/state"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(409, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("NO_SCENARIO", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void twoProcessDeadlockStateJsonMatchesContract() throws IOException, InterruptedException {
        Scenario scenario = new Scenario("Two Process Deadlock");
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        Resource r3 = new Resource("R3", 1, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Process p3 = new Process("P3", 3);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addResource(r3);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addProcess(p3);

        scenario.addOperation(new Operation("P1", com.deadlocksim.model.OperationType.REQUEST, "R1", 1));
        scenario.addOperation(new Operation("P1", com.deadlocksim.model.OperationType.REQUEST, "R2", 1));
        scenario.addOperation(new Operation("P2", com.deadlocksim.model.OperationType.REQUEST, "R2", 1));
        scenario.addOperation(new Operation("P2", com.deadlocksim.model.OperationType.REQUEST, "R1", 1));

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);
        p1.advanceOperation();

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);
        p2.advanceOperation();

        r3.grant(1);
        p3.holdInstances("R3", 1);
        p3.setState(ProcessState.RUNNING);

        engine.configure(scenario);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/state"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());

        JsonObject state = root.getAsJsonObject("data").getAsJsonObject("state");
        assertNotNull(state);

        assertEquals("Two Process Deadlock", state.getAsJsonObject("scenario").get("name").getAsString());
        assertEquals(0, state.get("step").getAsInt());

        JsonObject prev = state.getAsJsonObject("prevention");
        assertNotNull(prev);
        assertFalse(prev.get("mutualExclusion").getAsBoolean());
        assertFalse(prev.get("holdAndWait").getAsBoolean());
        assertFalse(prev.get("noPreemption").getAsBoolean());
        assertFalse(prev.get("circularWait").getAsBoolean());

        JsonArray processes = state.getAsJsonArray("processes");
        assertEquals(3, processes.size());

        JsonObject p1Json = findById(processes, "P1");
        assertNotNull(p1Json);
        assertEquals(1, p1Json.get("priority").getAsInt());
        assertEquals("WAITING", p1Json.get("state").getAsString());
        assertFalse(p1Json.get("deadlocked").getAsBoolean());
        assertEquals(1, p1Json.get("pendingOperations").getAsInt());

        JsonArray p1Held = p1Json.getAsJsonArray("held");
        assertEquals(1, p1Held.size());
        assertEquals("R1", p1Held.get(0).getAsJsonObject().get("resource").getAsString());
        assertEquals(1, p1Held.get(0).getAsJsonObject().get("count").getAsInt());

        JsonObject p1WaitingFor = p1Json.getAsJsonObject("waitingFor");
        assertNotNull(p1WaitingFor);
        assertEquals("R2", p1WaitingFor.get("resource").getAsString());
        assertEquals(1, p1WaitingFor.get("count").getAsInt());

        JsonObject p3Json = findById(processes, "P3");
        assertNotNull(p3Json);
        assertTrue(p3Json.get("waitingFor").isJsonNull());

        JsonArray resources = state.getAsJsonArray("resources");
        assertEquals(3, resources.size());
        JsonObject r1Json = findById(resources, "R1");
        assertNotNull(r1Json);
        assertEquals("R1", r1Json.get("name").getAsString());
        assertEquals(1, r1Json.get("total").getAsInt());
        assertEquals(0, r1Json.get("available").getAsInt());
        assertFalse(r1Json.get("shareable").getAsBoolean());

        JsonObject graph = state.getAsJsonObject("graph");
        assertNotNull(graph);
        JsonArray nodes = graph.getAsJsonArray("nodes");
        assertEquals(6, nodes.size());

        for (JsonElement ne : nodes) {
            JsonObject node = ne.getAsJsonObject();
            assertTrue(node.has("data"));
            JsonObject d = node.getAsJsonObject("data");
            assertTrue(d.has("id"));
            assertTrue(d.has("label"));
            assertTrue(d.has("type"));
            String type = d.get("type").getAsString();
            if ("resource".equals(type)) {
                assertTrue(d.has("instances"));
            }
        }

        JsonArray edges = graph.getAsJsonArray("edges");
        assertEquals(5, edges.size());

        int inCycleCount = 0;
        int notInCycleCount = 0;
        for (JsonElement ee : edges) {
            JsonObject edge = ee.getAsJsonObject();
            assertTrue(edge.has("data"));
            JsonObject d = edge.getAsJsonObject("data");
            assertTrue(d.has("id"));
            assertTrue(d.has("source"));
            assertTrue(d.has("target"));
            assertTrue(d.has("kind"));
            assertTrue(d.has("inCycle"));

            String src = d.get("source").getAsString();
            String tgt = d.get("target").getAsString();
            boolean inCycle = d.get("inCycle").getAsBoolean();

            if (inCycle) {
                inCycleCount++;
                assertTrue(isCycleEdge(src, tgt));
            } else {
                notInCycleCount++;
                assertEquals("R3", src);
                assertEquals("P3", tgt);
            }
        }

        assertEquals(4, inCycleCount);
        assertEquals(1, notInCycleCount);
    }

    private static boolean isCycleEdge(String source, String target) {
        return (source.equals("P1") && target.equals("R2"))
                || (source.equals("R2") && target.equals("P2"))
                || (source.equals("P2") && target.equals("R1"))
                || (source.equals("R1") && target.equals("P1"));
    }

    private static JsonObject findById(JsonArray array, String id) {
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            if (id.equals(obj.get("id").getAsString())) {
                return obj;
            }
        }
        return null;
    }
}
