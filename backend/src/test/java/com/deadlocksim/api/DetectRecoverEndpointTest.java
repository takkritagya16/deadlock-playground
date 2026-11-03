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

class DetectRecoverEndpointTest {
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
    void detectOnDeadlock() throws IOException, InterruptedException {
        setupTwoProcessDeadlock(false);

        HttpResponse<String> response = postJson("/api/detect", "{}");

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());
        JsonObject data = root.getAsJsonObject("data");
        assertEquals("CYCLE", data.get("strategy").getAsString());
        assertTrue(data.get("deadlocked").getAsBoolean());

        JsonArray processes = data.getAsJsonArray("processes");
        assertEquals(2, processes.size());
        assertFalse(data.get("cycle").isJsonNull());
        assertTrue(data.getAsJsonArray("cycle").size() > 0);
        assertNotNull(data.getAsJsonObject("state"));
    }

    @Test
    void detectOnSafeState() throws IOException, InterruptedException {
        String setupJson = """
                {
                  "name": "Safe State Test",
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
                    },
                    {
                      "id": "P2",
                      "priority": 2,
                      "operations": [
                        { "type": "REQUEST", "resource": "R1", "count": 1 }
                      ]
                    }
                  ]
                }
                """;
        postJson("/api/scenario", setupJson);
        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R1\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P2\", \"resource\": \"R1\", \"count\": 1}");

        HttpResponse<String> response = postJson("/api/detect", "{}");

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());
        JsonObject data = root.getAsJsonObject("data");
        assertEquals("CYCLE", data.get("strategy").getAsString());
        assertFalse(data.get("deadlocked").getAsBoolean());
        assertEquals(0, data.getAsJsonArray("processes").size());
        assertTrue(data.get("cycle").isJsonNull());
        assertNotNull(data.getAsJsonObject("state"));
    }

    @Test
    void recoverWithPriorityPolicy() throws IOException, InterruptedException {
        setupTwoProcessDeadlock(false);

        String recoverJson = """
                { "policy": "PRIORITY", "strategy": "TERMINATE" }
                """;
        HttpResponse<String> response = postJson("/api/recover", recoverJson);

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());
        JsonObject data = root.getAsJsonObject("data");
        assertTrue(data.get("resolved").getAsBoolean());

        JsonArray rounds = data.getAsJsonArray("rounds");
        assertEquals(1, rounds.size());
        JsonObject round0 = rounds.get(0).getAsJsonObject();
        assertEquals("P1", round0.get("victim").getAsString());
        assertEquals("TERMINATE", round0.get("strategy").getAsString());
        assertTrue(round0.get("resolvedAfter").getAsBoolean());
        assertNotNull(data.getAsJsonObject("state"));
    }

    @Test
    void recoverWithResourcesHeldPolicy() throws IOException, InterruptedException {
        String setupJson = """
                {
                  "name": "Held Policy Deadlock",
                  "resources": [
                    { "id": "R1", "total": 2, "shareable": false },
                    { "id": "R2", "total": 1, "shareable": false }
                  ],
                  "processes": [
                    {
                      "id": "P1",
                      "priority": 10,
                      "operations": [
                        { "type": "REQUEST", "resource": "R1", "count": 2 },
                        { "type": "REQUEST", "resource": "R2", "count": 1 }
                      ]
                    },
                    {
                      "id": "P2",
                      "priority": 20,
                      "operations": [
                        { "type": "REQUEST", "resource": "R2", "count": 1 },
                        { "type": "REQUEST", "resource": "R1", "count": 1 }
                      ]
                    }
                  ]
                }
                """;
        postJson("/api/scenario", setupJson);
        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R1\", \"count\": 2}");
        postJson("/api/request", "{\"process\": \"P2\", \"resource\": \"R2\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R2\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P2\", \"resource\": \"R1\", \"count\": 1}");

        String recoverJson = """
                { "policy": "RESOURCES_HELD", "strategy": "TERMINATE" }
                """;
        HttpResponse<String> response = postJson("/api/recover", recoverJson);

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());
        JsonObject data = root.getAsJsonObject("data");
        assertTrue(data.get("resolved").getAsBoolean());

        JsonArray rounds = data.getAsJsonArray("rounds");
        assertEquals(1, rounds.size());
        JsonObject round0 = rounds.get(0).getAsJsonObject();
        assertEquals("P2", round0.get("victim").getAsString());
        assertEquals("TERMINATE", round0.get("strategy").getAsString());
        assertTrue(round0.get("resolvedAfter").getAsBoolean());
    }

    @Test
    void recoverWithRecoveryCostPolicyDefault() throws IOException, InterruptedException {
        setupTwoProcessDeadlock(false);

        String recoverJson = """
                { "strategy": "TERMINATE" }
                """;
        HttpResponse<String> response = postJson("/api/recover", recoverJson);

        assertEquals(200, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(root.get("ok").getAsBoolean());
        JsonObject data = root.getAsJsonObject("data");
        assertTrue(data.get("resolved").getAsBoolean());
        assertEquals(1, data.getAsJsonArray("rounds").size());
    }

    @Test
    void recoverWithPreemptWhenPreemptionOffReturnsPreemptionError() throws IOException, InterruptedException {
        setupTwoProcessDeadlock(false);

        String recoverJson = """
                { "policy": "RECOVERY_COST", "strategy": "PREEMPT" }
                """;
        HttpResponse<String> response = postJson("/api/recover", recoverJson);

        assertEquals(400, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(root.get("ok").getAsBoolean());
        JsonObject error = root.getAsJsonObject("error");
        assertEquals("PREVENTION_VIOLATION", error.get("code").getAsString());
        assertTrue(error.get("message").getAsString().contains("Preemption is disabled"));
    }

    @Test
    void unknownPolicyReturnsInvalidPolicy() throws IOException, InterruptedException {
        setupTwoProcessDeadlock(false);

        String recoverJson = """
                { "policy": "NON_EXISTENT", "strategy": "TERMINATE" }
                """;
        HttpResponse<String> response = postJson("/api/recover", recoverJson);

        assertEquals(400, response.statusCode());
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(root.get("ok").getAsBoolean());
        JsonObject error = root.getAsJsonObject("error");
        assertEquals("INVALID_POLICY", error.get("code").getAsString());
    }

    private void setupTwoProcessDeadlock(boolean allowPreemption) throws IOException, InterruptedException {
        String setupJson = String.format("""
                {
                  "name": "Deadlock Setup",
                  "prevention": {
                    "mutualExclusion": false,
                    "holdAndWait": false,
                    "noPreemption": %b,
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
                """, allowPreemption);
        postJson("/api/scenario", setupJson);
        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R1\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P2\", \"resource\": \"R2\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P1\", \"resource\": \"R2\", \"count\": 1}");
        postJson("/api/request", "{\"process\": \"P2\", \"resource\": \"R1\", \"count\": 1}");
    }

    private HttpResponse<String> postJson(String endpoint, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
