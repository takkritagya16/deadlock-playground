package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.history.HistoryEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
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
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/health".equals(path)) {
                if (!"GET".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                sendJson(exchange, 200, "{\"ok\":true,\"data\":{\"status\":\"UP\"}}");
                return;
            }

            if ("/api/state".equals(path)) {
                if (!"GET".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
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

            if ("/api/scenario".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                JsonObject body = parseJsonObject(exchange);
                Scenario scenario = JsonMapper.parseScenario(body);
                engine.configure(scenario);
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                data.add("state", JsonMapper.mapState(engine));
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            if ("/api/reset".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                JsonObject body = parseJsonObjectOrEmpty(exchange);
                boolean confirmed = body.has("confirmed") && body.get("confirmed").getAsBoolean();
                engine.reset(confirmed);
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                data.add("state", JsonMapper.mapState(engine));
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            if ("/api/history".equals(path)) {
                if (!"GET".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                JsonArray eventsArray = new JsonArray();
                for (HistoryEvent e : engine.getHistoryEvents()) {
                    JsonObject ev = new JsonObject();
                    ev.addProperty("seq", e.getSequence());
                    ev.addProperty("type", e.getType().name());
                    ev.addProperty("description", e.getDescription());
                    eventsArray.add(ev);
                }
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                data.add("events", eventsArray);
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            ErrorMapper.sendError(exchange, ErrorCode.NOT_FOUND, "Not found: " + path);
        } catch (Throwable t) {
            ErrorMapper.sendError(exchange, t);
        }
    }

    private JsonObject parseJsonObject(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (body.isBlank()) {
            throw new IllegalArgumentException("Request body must not be empty");
        }
        try {
            JsonElement el = JsonParser.parseString(body);
            if (!el.isJsonObject()) {
                throw new IllegalArgumentException("Request body must be a JSON object");
            }
            return el.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Malformed JSON body: " + e.getMessage());
        }
    }

    private JsonObject parseJsonObjectOrEmpty(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (body.isBlank()) {
            return new JsonObject();
        }
        try {
            JsonElement el = JsonParser.parseString(body);
            if (!el.isJsonObject()) {
                throw new IllegalArgumentException("Request body must be a JSON object");
            }
            return el.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Malformed JSON body: " + e.getMessage());
        }
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
        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(code);
        } catch (Exception e) {
            errorCode = ErrorCode.INTERNAL_ERROR;
        }
        ErrorMapper.sendError(exchange, errorCode, message);
    }
}
