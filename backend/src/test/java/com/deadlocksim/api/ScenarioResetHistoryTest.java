package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

class ScenarioResetHistoryTest {
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
    void configureCustomScenarioAndCheckState() throws IOException, InterruptedException {
        String json = """
                {
                  "name": "Custom Test Scenario",
                  "prevention": {
                    "mutualExclusion": false,
                    "holdAndWait": false,
                    "noPreemption": false,
                    "circularWait": true
                  },
                  "resources": [
                    { "id": "R1", "total": 2, "shareable": false },
                    { "id": "R2", "total": 1, "shareable": false }
                  ],
                  "processes": [
                    {
                      "id": "P1",
                      "priority": 1,
                      "operations": [
                        { "type": "REQUEST", "resource": "R1", "count": 1 },
                        { "type": "RELEASE", "resource": "R1", "count": 1 }
                      ]
                    }
                  ]
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/scenario"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());

        JsonObject state = root.getAsJsonObject("data").getAsJsonObject("state");
        assertNotNull(state);
        assertEquals("Custom Test Scenario", state.getAsJsonObject("scenario").get("name").getAsString());
        assertTrue(state.getAsJsonObject("prevention").get("circularWait").getAsBoolean());

        JsonArray resources = state.getAsJsonArray("resources");
        assertEquals(2, resources.size());

        JsonArray processes = state.getAsJsonArray("processes");
        assertEquals(1, processes.size());
        JsonObject p1 = processes.get(0).getAsJsonObject();
        assertEquals("P1", p1.get("id").getAsString());
        assertEquals(2, p1.get("pendingOperations").getAsInt());
    }

    @Test
    void presetReturnsValidationError() throws IOException, InterruptedException {
        String json = """
                { "preset": "TEXTBOOK_FIGURE" }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/scenario"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(root.get("ok").getAsBoolean());
        assertEquals("VALIDATION_ERROR", root.getAsJsonObject("error").get("code").getAsString());
        assertEquals("Preset not available yet", root.getAsJsonObject("error").get("message").getAsString());
    }

    @Test
    void invalidJsonBodyReturnsValidationError() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/scenario"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("not-json"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(root.get("ok").getAsBoolean());
        assertEquals("VALIDATION_ERROR", root.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void resetWithConfirmationSucceeds() throws IOException, InterruptedException {
        configureSimpleScenario();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/reset"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"confirmed\": true}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());
        assertNotNull(root.getAsJsonObject("data").getAsJsonObject("state"));
    }

    @Test
    void resetWithoutConfirmationWhenHistoryHasEventsThrowsValidationError() throws IOException, InterruptedException {
        configureSimpleScenario();
        // perform an action so history size > 1
        engine.request("P1", "R1", 1);
        assertTrue(engine.getHistoryEvents().size() > 1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/reset"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"confirmed\": false}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(root.get("ok").getAsBoolean());
        assertEquals("VALIDATION_ERROR", root.getAsJsonObject("error").get("code").getAsString());
        assertEquals("Confirmation required to reset", root.getAsJsonObject("error").get("message").getAsString());
    }

    @Test
    void historyListOrder() throws IOException, InterruptedException {
        configureSimpleScenario();
        engine.request("P1", "R1", 1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());

        JsonArray events = root.getAsJsonObject("data").getAsJsonArray("events");
        assertEquals(3, events.size()); // 0: STATE_CHANGE, 1: AVOIDANCE_CHECK, 2: GRANT

        JsonObject ev0 = events.get(0).getAsJsonObject();
        assertEquals(0, ev0.get("seq").getAsInt());
        assertEquals("STATE_CHANGE", ev0.get("type").getAsString());
        assertEquals("Scenario loaded", ev0.get("description").getAsString());

        JsonObject ev1 = events.get(1).getAsJsonObject();
        assertEquals(1, ev1.get("seq").getAsInt());
        assertEquals("AVOIDANCE_CHECK", ev1.get("type").getAsString());

        JsonObject ev2 = events.get(2).getAsJsonObject();
        assertEquals(2, ev2.get("seq").getAsInt());
        assertEquals("GRANT", ev2.get("type").getAsString());
    }

    private void configureSimpleScenario() throws IOException, InterruptedException {
        String json = """
                {
                  "name": "Simple Scenario",
                  "resources": [
                    { "id": "R1", "total": 2, "shareable": false }
                  ],
                  "processes": [
                    {
                      "id": "P1",
                      "priority": 1,
                      "operations": [
                        { "type": "REQUEST", "resource": "R1", "count": 1 }
                      ]
                    }
                  ]
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/scenario"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
