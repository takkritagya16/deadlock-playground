package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerHealthTest {
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
    void getHealthReturns200WithStatusUp() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));

        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(body.get("ok").getAsBoolean());
        assertEquals("UP", body.getAsJsonObject("data").get("status").getAsString());
    }

    @Test
    void postHealthReturns405WithMethodNotAllowed() throws IOException, InterruptedException {
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
    void unknownPathReturns404WithNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/unknown"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        JsonObject body = gson.fromJson(response.body(), JsonObject.class);
        assertFalse(body.get("ok").getAsBoolean());
        assertEquals("NOT_FOUND", body.getAsJsonObject("error").get("code").getAsString());
    }
}
