package com.deadlocksim.model.detection;

import com.deadlocksim.model.Graph;
import com.deadlocksim.model.SafetyChecker;
import com.deadlocksim.model.Scenario;

public class BankersDetection implements DetectionStrategy {

    @Override
    public DetectionResult detect(Graph g) {
        if (g == null) {
            throw new IllegalArgumentException("Graph must not be null");
        }
        Scenario scenario = g.getScenario();
        return SafetyChecker.detectDeadlock(scenario);
    }
}

