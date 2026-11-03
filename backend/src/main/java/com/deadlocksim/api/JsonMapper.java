package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import com.deadlocksim.model.Graph;
import com.deadlocksim.model.Operation;
import com.deadlocksim.model.PreventionToggles;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.history.HistoryEvent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

    public static JsonObject mapState(SimulationEngine engine) {
        if (engine == null || engine.getScenario() == null) {
            throw new IllegalStateException("No scenario loaded");
        }
        Scenario scenario = engine.getScenario();
        JsonObject root = new JsonObject();

        JsonObject scenarioObj = new JsonObject();
        scenarioObj.addProperty("name", scenario.getName());
        root.add("scenario", scenarioObj);

        List<HistoryEvent> events = engine.getHistoryEvents();
        int step = events.isEmpty() ? 0 : events.get(events.size() - 1).getSequence();
        root.addProperty("step", step);

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
