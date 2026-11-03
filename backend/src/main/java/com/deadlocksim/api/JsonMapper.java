package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import com.deadlocksim.model.Graph;
import com.deadlocksim.model.Operation;
import com.deadlocksim.model.OperationType;
import com.deadlocksim.model.PreventionToggles;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.history.HistoryEvent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonMapper {
    private static final Gson GSON = new Gson();

    public static Gson getGson() {
        return GSON;
    }

    public static Scenario parseScenario(JsonObject body) {
        if (body == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        if (body.has("preset")) {
            throw new IllegalArgumentException("Preset not available yet");
        }
        if (!body.has("name") || body.get("name").getAsString().isBlank()) {
            throw new IllegalArgumentException("Scenario name must not be blank");
        }
        Scenario s = new Scenario(body.get("name").getAsString());

        if (body.has("prevention") && body.get("prevention").isJsonObject()) {
            JsonObject prev = body.getAsJsonObject("prevention");
            PreventionToggles toggles = s.getPreventionToggles();
            if (prev.has("mutualExclusion")) {
                toggles.setMutualExclusion(prev.get("mutualExclusion").getAsBoolean());
            }
            if (prev.has("holdAndWait")) {
                toggles.setHoldAndWait(prev.get("holdAndWait").getAsBoolean());
            }
            if (prev.has("noPreemption")) {
                toggles.setNoPreemption(prev.get("noPreemption").getAsBoolean());
            }
            if (prev.has("circularWait")) {
                toggles.setCircularWait(prev.get("circularWait").getAsBoolean());
            }
            s.setPreventionToggles(toggles);
        }

        if (body.has("resources") && body.get("resources").isJsonArray()) {
            for (JsonElement el : body.getAsJsonArray("resources")) {
                JsonObject rObj = el.getAsJsonObject();
                String id = rObj.get("id").getAsString();
                int total = rObj.get("total").getAsInt();
                boolean shareable = rObj.has("shareable") && rObj.get("shareable").getAsBoolean();
                s.addResource(new Resource(id, total, shareable));
            }
        }

        if (body.has("processes") && body.get("processes").isJsonArray()) {
            for (JsonElement el : body.getAsJsonArray("processes")) {
                JsonObject pObj = el.getAsJsonObject();
                String pid = pObj.get("id").getAsString();
                int priority = pObj.has("priority") ? pObj.get("priority").getAsInt() : 1;
                s.addProcess(new Process(pid, priority));

                if (pObj.has("operations") && pObj.get("operations").isJsonArray()) {
                    for (JsonElement opEl : pObj.getAsJsonArray("operations")) {
                        JsonObject opObj = opEl.getAsJsonObject();
                        String typeStr = opObj.get("type").getAsString();
                        OperationType type = OperationType.valueOf(typeStr.toUpperCase());
                        String resId = opObj.get("resource").getAsString();
                        int count = opObj.has("count") ? opObj.get("count").getAsInt() : 1;
                        s.addOperation(new Operation(pid, type, resId, count));
                    }
                }
            }
        }

        return s;
    }

    public static JsonObject mapState(SimulationEngine engine) {
        if (engine == null || engine.getScenario() == null) {
            throw new IllegalStateException("No scenario loaded");
        }
        Scenario scenario = engine.getScenario();
        JsonObject root = new JsonObject();

        JsonObject scenarioObj = new JsonObject();
        scenarioObj.addProperty("name", scenario.getName());
        root.add("scenario", scenarioObj);

        root.addProperty("step", engine.getViewedSeq());

        JsonObject prevObj = new JsonObject();
        PreventionToggles toggles = scenario.getPreventionToggles();
        prevObj.addProperty("mutualExclusion", toggles.isMutualExclusion());
        prevObj.addProperty("holdAndWait", toggles.isHoldAndWait());
        prevObj.addProperty("noPreemption", toggles.isNoPreemption());
        prevObj.addProperty("circularWait", toggles.isCircularWait());
        root.add("prevention", prevObj);

        JsonArray processesArray = new JsonArray();
        for (Process p : scenario.getProcesses()) {
            JsonObject procObj = new JsonObject();
            procObj.addProperty("id", p.getId());
            procObj.addProperty("priority", p.getPriority());
            procObj.addProperty("state", p.getState().name());
            procObj.addProperty("deadlocked", p.isDeadlocked());

            JsonArray heldArray = new JsonArray();
            List<String> heldKeys = new ArrayList<>(p.getHeld().keySet());
            Collections.sort(heldKeys);
            for (String resId : heldKeys) {
                int count = p.getHeld().getOrDefault(resId, 0);
                if (count > 0) {
                    JsonObject h = new JsonObject();
                    h.addProperty("resource", resId);
                    h.addProperty("count", count);
                    heldArray.add(h);
                }
            }
            procObj.add("held", heldArray);

            if (p.isWaiting()) {
                JsonObject wf = new JsonObject();
                wf.addProperty("resource", p.getWaitingResourceId());
                wf.addProperty("count", p.getWaitingCount());
                procObj.add("waitingFor", wf);
            } else {
                procObj.add("waitingFor", JsonNull.INSTANCE);
            }

            List<Operation> ops = scenario.getOperations(p.getId());
            int totalOps = ops != null ? ops.size() : 0;
            int nextIndex = p.getNextOperationIndex();
            int pending = Math.max(0, totalOps - nextIndex);
            procObj.addProperty("pendingOperations", pending);

            processesArray.add(procObj);
        }
        root.add("processes", processesArray);

        JsonArray resourcesArray = new JsonArray();
        for (Resource r : scenario.getResources()) {
            JsonObject resObj = new JsonObject();
            resObj.addProperty("id", r.getId());
            resObj.addProperty("name", r.getName());
            resObj.addProperty("total", r.getTotal());
            resObj.addProperty("available", r.getAvailable());
            resObj.addProperty("shareable", r.isShareable());
            resourcesArray.add(resObj);
        }
        root.add("resources", resourcesArray);

        Graph graph = new Graph(scenario);
        List<List<String>> cycles = graph.findCycles();
        Set<String> cycleEdges = new HashSet<>();
        for (List<String> cycle : cycles) {
            for (int i = 0; i < cycle.size() - 1; i++) {
                cycleEdges.add(cycle.get(i) + "->" + cycle.get(i + 1));
            }
        }

        JsonObject graphObj = new JsonObject();
        JsonArray nodesArray = new JsonArray();

        for (Process p : scenario.getProcesses()) {
            JsonObject node = new JsonObject();
            JsonObject data = new JsonObject();
            data.addProperty("id", p.getId());
            data.addProperty("label", p.getId());
            data.addProperty("type", "process");
            node.add("data", data);
            nodesArray.add(node);
        }

        for (Resource r : scenario.getResources()) {
            JsonObject node = new JsonObject();
            JsonObject data = new JsonObject();
            data.addProperty("id", r.getId());
            data.addProperty("label", r.getName());
            data.addProperty("type", "resource");
            data.addProperty("instances", r.getTotal());
            node.add("data", data);
            nodesArray.add(node);
        }
        graphObj.add("nodes", nodesArray);

        JsonArray edgesArray = new JsonArray();
        int edgeSeq = 1;
        for (Graph.Edge edge : graph.edges()) {
            JsonObject edgeObj = new JsonObject();
            JsonObject data = new JsonObject();
            data.addProperty("id", "e" + edgeSeq++);
            data.addProperty("source", edge.getFrom());
            data.addProperty("target", edge.getTo());
            data.addProperty("kind", edge.getKind().name().toLowerCase());
            boolean inCycle = cycleEdges.contains(edge.getFrom() + "->" + edge.getTo());
            data.addProperty("inCycle", inCycle);
            edgeObj.add("data", data);
            edgesArray.add(edgeObj);
        }
        graphObj.add("edges", edgesArray);

        root.add("graph", graphObj);

        return root;
    }
}
