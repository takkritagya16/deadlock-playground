package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ApiServer {
    private final SimulationEngine engine;
    private final int port;
    private HttpServer server;

    public ApiServer(SimulationEngine engine) {
        this(engine, 0);
    }

    public ApiServer(SimulationEngine engine, int port) {
        this.engine = engine;
        this.port = port;
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public int start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRoot);
        server.start();
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/health".equals(path)) {
            if (!"GET".equalsIgnoreCase(method)) {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed: " + method);
                return;
            }
            sendJson(exchange, 200, "{\"ok\":true,\"data\":{\"status\":\"UP\"}}");
            return;
        }

        if ("/api/state".equals(path)) {
            if (!"GET".equalsIgnoreCase(method)) {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed: " + method);
                return;
            }
            if (engine.getScenario() == null) {
                sendError(exchange, 409, "NO_SCENARIO", "No scenario loaded");
                return;
            }
            JsonObject stateObj = JsonMapper.mapState(engine);
            JsonObject root = new JsonObject();
            root.addProperty("ok", true);
            JsonObject data = new JsonObject();
            data.add("state", stateObj);
            root.add("data", data);
            sendJson(exchange, 200, root.toString());
            return;
        }

        sendError(exchange, 404, "NOT_FOUND", "Not found: " + path);
    }

    void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    void sendError(HttpExchange exchange, int statusCode, String code, String message) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("ok", false);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        root.add("error", error);
        sendJson(exchange, statusCode, root.toString());
    }
}
