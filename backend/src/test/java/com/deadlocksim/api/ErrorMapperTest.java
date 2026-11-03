package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import com.deadlocksim.model.Scenario;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorMapperTest {
    private ApiServer server;
    private int boundPort;
    private HttpClient client;
    private Gson gson;

    @BeforeEach
    void setUp() {
        client = HttpClient.newHttpClient();
        gson = new Gson();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    private void startServerWithEngine(SimulationEngine engine) throws IOException {
        server = new ApiServer(engine);
        boundPort = server.start();
    }

    @Test
    void validationErrorTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new IllegalArgumentException("Process ID must not be blank");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(400, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("VALIDATION_ERROR", body.getAsJsonObject("error").get("code").getAsString());
        assertEquals("Process ID must not be blank", body.getAsJsonObject("error").get("message").getAsString());
    }

    @Test
    void preventionViolationTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new IllegalStateException("Preemption is disabled");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(400, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("PREVENTION_VIOLATION", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void notHeldTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new IllegalStateException("Process P1 holds only 0 of R1");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(400, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("NOT_HELD", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void noOperationsTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new IllegalStateException("No operations pending");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(400, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("NO_OPERATIONS", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void noDeadlockTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new IllegalStateException("No deadlock to recover from");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(400, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("NO_DEADLOCK", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void invalidPolicyTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new IllegalStateException("Unknown policy: RANDOM");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(400, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("INVALID_POLICY", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void invalidHistoryIndexTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new IllegalStateException("Invalid history index: 99");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(400, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("INVALID_HISTORY_INDEX", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void notFoundTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine());

        HttpResponse<String> response = sendGet("/api/nonexistent");
        assertEquals(404, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("NOT_FOUND", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void methodNotAllowedTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/health"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("METHOD_NOT_ALLOWED", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void noScenarioTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine());

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(409, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("NO_SCENARIO", body.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void internalErrorTriggeredThroughHttp() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine() {
            @Override
            public Scenario getScenario() {
                throw new RuntimeException("Unexpected database failure");
            }
        });

        HttpResponse<String> response = sendGet("/api/state");
        assertEquals(500, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("INTERNAL_ERROR", body.getAsJsonObject("error").get("code").getAsString());
        assertEquals("Unexpected database failure", body.getAsJsonObject("error").get("message").getAsString());
    }

    @Test
    void errorResponseExactShape() throws IOException, InterruptedException {
        startServerWithEngine(new SimulationEngine());

        HttpResponse<String> response = sendGet("/api/unknown-endpoint");
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);

        assertEquals(Set.of("ok", "error"), body.keySet());
        assertFalse(body.get("ok").getAsBoolean());

        JsonObject errorObj = body.getAsJsonObject("error");
        assertNotNull(errorObj);
        assertEquals(Set.of("code", "message"), errorObj.keySet());
        assertTrue(errorObj.get("code").isJsonPrimitive());
        assertTrue(errorObj.get("message").isJsonPrimitive());
    }

    private HttpResponse<String> sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
