package com.deadlocksim.model.detection;

import com.deadlocksim.model.Graph;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CycleDetection implements DetectionStrategy {

    @Override
    public DetectionResult detect(Graph g) {
        if (g == null) {
            throw new IllegalArgumentException("Graph must not be null");
        }
        Scenario scenario = g.getScenario();
        for (Resource resource : scenario.getResources()) {
            if (resource.getTotal() > 1) {
                throw new IllegalStateException("CycleDetection is only valid for single-instance resources, but "
                        + resource.getId() + " has total " + resource.getTotal());
            }
        }

        List<List<String>> cycles = g.findCycles();
        if (cycles.isEmpty()) {
            for (Process process : scenario.getProcesses()) {
                process.setDeadlocked(false);
            }
            return DetectionResult.none();
        }

        Set<String> scenarioProcessIds = new HashSet<>();
        for (Process process : scenario.getProcesses()) {
            scenarioProcessIds.add(process.getId());
        }

        Set<String> deadlockedProcessIds = new HashSet<>();
        for (List<String> cycle : cycles) {
            for (String nodeId : cycle) {
                if (scenarioProcessIds.contains(nodeId)) {
                    deadlockedProcessIds.add(nodeId);
                }
            }
        }

        for (Process process : scenario.getProcesses()) {
            process.setDeadlocked(deadlockedProcessIds.contains(process.getId()));
        }

        List<String> firstCycle = cycles.get(0);
        return DetectionResult.of(new ArrayList<>(deadlockedProcessIds), firstCycle);
    }
}
