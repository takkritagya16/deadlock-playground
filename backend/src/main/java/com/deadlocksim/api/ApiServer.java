package com.deadlocksim.api;

import com.deadlocksim.engine.DetectReport;
import com.deadlocksim.engine.RecoveryReport;
import com.deadlocksim.engine.RecoveryRound;
import com.deadlocksim.engine.ReleaseResult;
import com.deadlocksim.engine.RequestResult;
import com.deadlocksim.engine.SimulationEngine;
import com.deadlocksim.engine.StepResult;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.history.HistoryEvent;
import com.deadlocksim.model.victim.PriorityPolicy;
import com.deadlocksim.model.victim.RecoveryCostPolicy;
import com.deadlocksim.model.victim.ResourcesHeldPolicy;
import com.deadlocksim.model.victim.VictimPolicy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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
import java.nio.file.Path;

public class ApiServer {
    private final SimulationEngine engine;
    private final int port;
    private final Path frontendPath;
    private HttpServer server;

    public ApiServer(SimulationEngine engine) {
        this(engine, 0, Path.of("../frontend"));
    }

    public ApiServer(SimulationEngine engine, int port) {
        this(engine, port, Path.of("../frontend"));
    }

    public ApiServer(SimulationEngine engine, int port, Path frontendPath) {
        this.engine = engine;
        this.port = port;
        this.frontendPath = frontendPath != null ? frontendPath : Path.of("../frontend");
    }

    public ApiServer(SimulationEngine engine, int port, String frontendFolder) {
        this(engine, port, frontendFolder != null ? Path.of(frontendFolder) : Path.of("../frontend"));
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public Path getFrontendPath() {
        return frontendPath;
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

            if ("/api/history/jump".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                JsonObject body = parseJsonObject(exchange);
                if (!body.has("seq") || !body.get("seq").isJsonPrimitive()) {
                    throw new IllegalArgumentException("Field 'seq' is required");
                }
                int seq = body.get("seq").getAsInt();
                try {
                    engine.jumpTo(seq);
                } catch (IllegalArgumentException e) {
                    ErrorMapper.sendError(exchange, ErrorCode.INVALID_HISTORY_INDEX, e.getMessage());
                    return;
                }
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                data.add("state", JsonMapper.mapState(engine));
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            if ("/api/request".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                JsonObject body = parseJsonObject(exchange);
                if (!body.has("process") || !body.get("process").isJsonPrimitive()) {
                    throw new IllegalArgumentException("Field 'process' is required");
                }
                if (!body.has("resource") || !body.get("resource").isJsonPrimitive()) {
                    throw new IllegalArgumentException("Field 'resource' is required");
                }
                if (!body.has("count") || !body.get("count").isJsonPrimitive()) {
                    throw new IllegalArgumentException("Field 'count' is required");
                }
                String processId = body.get("process").getAsString();
                String resourceId = body.get("resource").getAsString();
                int count = body.get("count").getAsInt();

                RequestResult rr = engine.request(processId, resourceId, count);
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                data.addProperty("outcome", rr.outcome().name());
                data.addProperty("reason", rr.reason());
                data.add("safety", JsonNull.INSTANCE);
                data.add("state", JsonMapper.mapState(engine));
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            if ("/api/release".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                JsonObject body = parseJsonObject(exchange);
                if (!body.has("process") || !body.get("process").isJsonPrimitive()) {
                    throw new IllegalArgumentException("Field 'process' is required");
                }
                if (!body.has("resource") || !body.get("resource").isJsonPrimitive()) {
                    throw new IllegalArgumentException("Field 'resource' is required");
                }
                if (!body.has("count") || !body.get("count").isJsonPrimitive()) {
                    throw new IllegalArgumentException("Field 'count' is required");
                }
                String processId = body.get("process").getAsString();
                String resourceId = body.get("resource").getAsString();
                int count = body.get("count").getAsInt();

                ReleaseResult rr = engine.release(processId, resourceId, count);
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                JsonArray unblocked = new JsonArray();
                for (String pid : rr.unblockedProcessIds()) {
                    unblocked.add(pid);
                }
                data.add("unblocked", unblocked);
                data.add("state", JsonMapper.mapState(engine));
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            if ("/api/step".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                parseJsonObjectOrEmpty(exchange);
                StepResult sr = engine.step();
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                JsonObject executed = new JsonObject();
                executed.addProperty("description", sr.description());
                data.add("executed", executed);
                data.addProperty("finished", sr.finished());
                data.add("state", JsonMapper.mapState(engine));
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            if ("/api/detect".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                parseJsonObjectOrEmpty(exchange);
                DetectReport report = engine.detect();
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                data.addProperty("strategy", report.strategy());
                data.addProperty("deadlocked", report.result().isDeadlocked());
                JsonArray processes = new JsonArray();
                for (String pid : report.result().getProcessIds()) {
                    processes.add(pid);
                }
                data.add("processes", processes);
                if (report.result().getCycle().isEmpty()) {
                    data.add("cycle", JsonNull.INSTANCE);
                } else {
                    JsonArray cycle = new JsonArray();
                    for (String node : report.result().getCycle()) {
                        cycle.add(node);
                    }
                    data.add("cycle", cycle);
                }
                data.add("state", JsonMapper.mapState(engine));
                root.add("data", data);
                sendJson(exchange, 200, root.toString());
                return;
            }

            if ("/api/recover".equals(path)) {
                if (!"POST".equalsIgnoreCase(method)) {
                    ErrorMapper.sendError(exchange, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed: " + method);
                    return;
                }
                JsonObject body = parseJsonObject(exchange);
                String policyStr = "RECOVERY_COST";
                if (body.has("policy") && !body.get("policy").isJsonNull()) {
                    if (!body.get("policy").isJsonPrimitive() || !body.get("policy").getAsJsonPrimitive().isString()) {
                        ErrorMapper.sendError(exchange, ErrorCode.INVALID_POLICY, "Invalid policy type");
                        return;
                    }
                    policyStr = body.get("policy").getAsString();
                }

                VictimPolicy policy;
                if ("PRIORITY".equalsIgnoreCase(policyStr)) {
                    policy = new PriorityPolicy();
                } else if ("RESOURCES_HELD".equalsIgnoreCase(policyStr)) {
                    policy = new ResourcesHeldPolicy();
                } else if ("RECOVERY_COST".equalsIgnoreCase(policyStr)) {
                    policy = new RecoveryCostPolicy();
                } else {
                    ErrorMapper.sendError(exchange, ErrorCode.INVALID_POLICY, "Unknown policy: " + policyStr);
                    return;
                }

                if (!body.has("strategy") || body.get("strategy").isJsonNull()
                        || !body.get("strategy").isJsonPrimitive()
                        || !body.get("strategy").getAsJsonPrimitive().isString()) {
                    ErrorMapper.sendError(exchange, ErrorCode.INVALID_POLICY, "Strategy is required and must be a string");
                    return;
                }
                String strategyStr = body.get("strategy").getAsString();
                boolean usePreempt;
                if ("TERMINATE".equalsIgnoreCase(strategyStr)) {
                    usePreempt = false;
                } else if ("PREEMPT".equalsIgnoreCase(strategyStr)) {
                    usePreempt = true;
                } else {
                    ErrorMapper.sendError(exchange, ErrorCode.INVALID_POLICY, "Unknown strategy: " + strategyStr);
                    return;
                }

                RecoveryReport rr = engine.recover(policy, usePreempt);
                JsonObject root = new JsonObject();
                root.addProperty("ok", true);
                JsonObject data = new JsonObject();
                JsonArray rounds = new JsonArray();
                for (RecoveryRound round : rr.rounds()) {
                    JsonObject rObj = new JsonObject();
                    rObj.addProperty("victim", round.victimId());
                    rObj.addProperty("strategy", round.strategy());
                    rObj.addProperty("resolvedAfter", round.resolvedAfter());
                    rounds.add(rObj);
                }
                data.add("rounds", rounds);
                data.addProperty("resolved", rr.resolved());
                data.add("state", JsonMapper.mapState(engine));
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
