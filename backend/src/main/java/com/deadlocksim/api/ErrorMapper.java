package com.deadlocksim.api;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ErrorMapper {
    private static final Map<String, ErrorCode> LOOKUP_TABLE = new LinkedHashMap<>();

    static {
        LOOKUP_TABLE.put("No scenario loaded", ErrorCode.NO_SCENARIO);
        LOOKUP_TABLE.put("No deadlock to recover from", ErrorCode.NO_DEADLOCK);
        LOOKUP_TABLE.put("No waiting process to check", ErrorCode.NO_DEADLOCK);
        LOOKUP_TABLE.put("Preemption is disabled", ErrorCode.PREVENTION_VIOLATION);
        LOOKUP_TABLE.put("Confirmation required to reset", ErrorCode.VALIDATION_ERROR);
        LOOKUP_TABLE.put("Circular wait prevention", ErrorCode.PREVENTION_VIOLATION);
        LOOKUP_TABLE.put("Hold and wait prevention", ErrorCode.PREVENTION_VIOLATION);
        LOOKUP_TABLE.put("holds only", ErrorCode.NOT_HELD);
        LOOKUP_TABLE.put("Process holds fewer instances than requested to release", ErrorCode.NOT_HELD);
        LOOKUP_TABLE.put("Victim does not hold resource", ErrorCode.NOT_HELD);
        LOOKUP_TABLE.put("Not held", ErrorCode.NOT_HELD);
        LOOKUP_TABLE.put("Scenario finished", ErrorCode.NO_OPERATIONS);
        LOOKUP_TABLE.put("No operations", ErrorCode.NO_OPERATIONS);
        LOOKUP_TABLE.put("No runnable process", ErrorCode.NO_OPERATIONS);
        LOOKUP_TABLE.put("Unknown policy", ErrorCode.INVALID_POLICY);
        LOOKUP_TABLE.put("Invalid policy", ErrorCode.INVALID_POLICY);
        LOOKUP_TABLE.put("Invalid history index", ErrorCode.INVALID_HISTORY_INDEX);
        LOOKUP_TABLE.put("Process is not RUNNING", ErrorCode.VALIDATION_ERROR);
    }

    public static ErrorCode resolveCode(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return ErrorCode.VALIDATION_ERROR;
        }
        if (t instanceof IllegalStateException) {
            String msg = t.getMessage();
            if (msg != null) {
                if (LOOKUP_TABLE.containsKey(msg)) {
                    return LOOKUP_TABLE.get(msg);
                }
                for (Map.Entry<String, ErrorCode> entry : LOOKUP_TABLE.entrySet()) {
                    if (msg.contains(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    public static JsonObject toJsonObject(ErrorCode code, String message) {
        JsonObject root = new JsonObject();
        root.addProperty("ok", false);
        JsonObject error = new JsonObject();
        error.addProperty("code", code.name());
        error.addProperty("message", message != null ? message : code.name());
        root.add("error", error);
        return root;
    }

    public static JsonObject toJsonObject(Throwable t) {
        ErrorCode code = resolveCode(t);
        String msg = t.getMessage() != null ? t.getMessage() : code.name();
        return toJsonObject(code, msg);
    }

    public static void sendError(HttpExchange exchange, Throwable t) throws IOException {
        ErrorCode code = resolveCode(t);
        String msg = t.getMessage() != null ? t.getMessage() : code.name();
        sendError(exchange, code, msg);
    }

    public static void sendError(HttpExchange exchange, ErrorCode code, String message) throws IOException {
        JsonObject json = toJsonObject(code, message);
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code.getHttpStatus(), bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
