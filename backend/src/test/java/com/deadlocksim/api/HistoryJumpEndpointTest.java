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

class HistoryJumpEndpointTest {
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
    void jumpBackAndCheckState() throws IOException, InterruptedException {
        String setupJson = """
                {
                  "name": "Jump Test",
                  "resources": [
                    { "id": "R1", "total": 1, "shareable": false }
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
        postJson("/api/scenario", setupJson);

        HttpResponse<String> reqResp = postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R1\", \"count\": 1}");
        assertEquals(200, reqResp.statusCode());

        JsonObject stateAfterGrant = gson.fromJson(reqResp.body(), JsonObject.class).getAsJsonObject("data").getAsJsonObject("state");
        JsonObject r1After = stateAfterGrant.getAsJsonArray("resources").get(0).getAsJsonObject();
        assertEquals(0, r1After.get("available").getAsInt());

        HttpResponse<String> jumpResp = postJson("/api/history/jump", "{\"seq\": 0}");
        assertEquals(200, jumpResp.statusCode());
        JsonObject root = gson.fromJson(jumpResp.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());

        JsonObject jumpedState = root.getAsJsonObject("data").getAsJsonObject("state");
        JsonObject r1Jumped = jumpedState.getAsJsonArray("resources").get(0).getAsJsonObject();
        assertEquals(1, r1Jumped.get("available").getAsInt());
        JsonObject p1Jumped = jumpedState.getAsJsonArray("processes").get(0).getAsJsonObject();
        assertEquals(0, p1Jumped.getAsJsonArray("held").size());
    }

    @Test
    void historyListUnchangedAfterJump() throws IOException, InterruptedException {
        String setupJson = """
                {
                  "name": "Jump Unchanged Test",
                  "resources": [
                    { "id": "R1", "total": 1, "shareable": false }
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
        postJson("/api/scenario", setupJson);
        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R1\", \"count\": 1}");

        HttpResponse<String> histRespBefore = sendGet("/api/history");
        JsonObject histBefore = gson.fromJson(histRespBefore.body(), JsonObject.class);
        JsonArray eventsBefore = histBefore.getAsJsonObject("data").getAsJsonArray("events");
        int eventCountBefore = eventsBefore.size();
        assertTrue(eventCountBefore > 1);

        postJson("/api/history/jump", "{\"seq\": 0}");

        HttpResponse<String> histRespAfter = sendGet("/api/history");
        JsonObject histAfter = gson.fromJson(histRespAfter.body(), JsonObject.class);
        JsonArray eventsAfter = histAfter.getAsJsonObject("data").getAsJsonArray("events");

        assertEquals(eventCountBefore, eventsAfter.size());
        for (int i = 0; i < eventCountBefore; i++) {
            JsonObject eb = eventsBefore.get(i).getAsJsonObject();
            JsonObject ea = eventsAfter.get(i).getAsJsonObject();
            assertEquals(eb.get("seq").getAsInt(), ea.get("seq").getAsInt());
            assertEquals(eb.get("type").getAsString(), ea.get("type").getAsString());
            assertEquals(eb.get("description").getAsString(), ea.get("description").getAsString());
        }
    }

    @Test
    void unknownSeqReturnsInvalidHistoryIndex() throws IOException, InterruptedException {
        String setupJson = """
                {
                  "name": "Unknown Seq Test",
                  "resources": [
                    { "id": "R1", "total": 1, "shareable": false }
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
        postJson("/api/scenario", setupJson);

        HttpResponse<String> response = postJson("/api/history/jump", "{\"seq\": 999}");

        assertEquals(400, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(root.get("ok").getAsBoolean());
        JsonObject error = root.getAsJsonObject("error");
        assertEquals("INVALID_HISTORY_INDEX", error.get("code").getAsString());
    }

    @Test
    void endToEndScenarioRequestDetectRecoverHistoryEndsWithResolved() throws IOException, InterruptedException {
        String setupJson = """
                {
                  "name": "End to End Deadlock",
                  "prevention": {
                    "mutualExclusion": false,
                    "holdAndWait": false,
                    "noPreemption": false,
                    "circularWait": false
                  },
                  "resources": [
                    { "id": "R1", "total": 1, "shareable": false },
                    { "id": "R2", "total": 1, "shareable": false }
                  ],
                  "processes": [
                    {
                      "id": "P1",
                      "priority": 1,
                      "operations": [
                        { "type": "REQUEST", "resource": "R1", "count": 1 },
                        { "type": "REQUEST", "resource": "R2", "count": 1 }
                      ]
                    },
                    {
                      "id": "P2",
                      "priority": 2,
                      "operations": [
                        { "type": "REQUEST", "resource": "R2", "count": 1 },
                        { "type": "REQUEST", "resource": "R1", "count": 1 }
                      ]
                    }
                  ]
                }
                """;
        HttpResponse<String> scResp = postJson("/api/scenario", setupJson);
        assertEquals(200, scResp.statusCode());

        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R1\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P2\", \"resource\": \"R2\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R2\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P2\", \"resource\": \"R1\", \"count\": 1}");

        HttpResponse<String> detectResp = postJson("/api/detect", "{}");
        assertEquals(200, detectResp.statusCode());
        JsonObject detectRoot = gson.fromJson(detectResp.body(), JsonObject.class);
        assertTrue(detectRoot.getAsJsonObject("data").get("deadlocked").getAsBoolean());

        HttpResponse<String> recoverResp = postJson("/api/recover", "{\"policy\": \"RECOVERY_COST\", \"strategy\": \"TERMINATE\"}");
        assertEquals(200, recoverResp.statusCode());
        JsonObject recoverRoot = gson.fromJson(recoverResp.body(), JsonObject.class);
        assertTrue(recoverRoot.getAsJsonObject("data").get("resolved").getAsBoolean());

        HttpResponse<String> historyResp = sendGet("/api/history");
        assertEquals(200, historyResp.statusCode());
        JsonObject historyRoot = gson.fromJson(historyResp.body(), JsonObject.class);
        JsonArray events = historyRoot.getAsJsonObject("data").getAsJsonArray("events");
        assertTrue(events.size() > 0);

        JsonObject lastEvent = events.get(events.size() - 1).getAsJsonObject();
        assertEquals("RESOLVED", lastEvent.get("type").getAsString());
    }

    private HttpResponse<String> postJson(String endpoint, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendGet(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + endpoint))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
