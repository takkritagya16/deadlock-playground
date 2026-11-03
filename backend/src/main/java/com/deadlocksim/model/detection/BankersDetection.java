package com.deadlocksim.model.detection;

import com.deadlocksim.model.BankersCore;
import com.deadlocksim.model.Graph;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankersDetection implements DetectionStrategy {

    @Override
    public DetectionResult detect(Graph g) {
        if (g == null) {
            throw new IllegalArgumentException("Graph must not be null");
        }
        Scenario scenario = g.getScenario();
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }

        Map<String, Integer> work = new HashMap<>();
        for (Resource r : scenario.getResources()) {
            work.put(r.getId(), r.getAvailable());
        }

        BankersCore.FinishResult finishResult = BankersCore.run(scenario, work);
        List<String> stuck = finishResult.stuck();
        if (stuck.isEmpty()) {
            for (Process p : scenario.getProcesses()) {
                p.setDeadlocked(false);
            }
            return DetectionResult.none();
        }

        for (Process p : scenario.getProcesses()) {
            p.setDeadlocked(stuck.contains(p.getId()));
        }
        List<String> sortedStuck = new ArrayList<>(stuck);
        Collections.sort(sortedStuck);
        return DetectionResult.of(sortedStuck, Collections.emptyList());
    }
}

