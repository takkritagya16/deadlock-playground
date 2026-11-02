package com.deadlocksim.model;

import com.deadlocksim.model.detection.DetectionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SafetyChecker {
    private SafetyChecker() {}

    public static SafetyResult check(Scenario s, String processId, String resourceId, int count) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Process ID must not be null or blank");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Resource ID must not be null or blank");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than zero");
        }

        Process process = s.getProcesses().stream()
                .filter(p -> p.getId().equals(processId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown process: " + processId));

        Resource resource = s.getResources().stream()
                .filter(r -> r.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown resource: " + resourceId));

        if (resource.getAvailable() < count) {
            return SafetyResult.unsafe("Insufficient available instances of resource " + resourceId, Collections.emptyList());
        }

        resource.grant(count);
        process.holdInstances(resourceId, count);
        try {
            Map<String, Integer> work = new HashMap<>();
            for (Resource r : s.getResources()) {
                work.put(r.getId(), r.getAvailable());
            }
            BankersCore.FinishResult finishResult = BankersCore.run(s, work);
            if (finishResult.stuck().isEmpty()) {
                String reason = "Safe sequence: " + finishResult.sequence();
                return SafetyResult.safe(reason, finishResult.sequence());
            } else {
                String reason = "Unsafe state: processes " + finishResult.stuck() + " cannot finish";
                return SafetyResult.unsafe(reason, finishResult.sequence());
            }
        } finally {
            process.releaseInstances(resourceId, count);
            resource.release(count);
        }
    }

    public static DetectionResult detectDeadlock(Scenario s) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        Map<String, Integer> work = new HashMap<>();
        for (Resource r : s.getResources()) {
            work.put(r.getId(), r.getAvailable());
        }
        BankersCore.FinishResult finishResult = BankersCore.run(s, work);
        List<String> stuck = finishResult.stuck();
        if (stuck.isEmpty()) {
            for (Process p : s.getProcesses()) {
                p.setDeadlocked(false);
            }
            return DetectionResult.none();
        }
        for (Process p : s.getProcesses()) {
            p.setDeadlocked(stuck.contains(p.getId()));
        }
        List<String> sortedStuck = new ArrayList<>(stuck);
        Collections.sort(sortedStuck);
        return DetectionResult.of(sortedStuck, Collections.emptyList());
    }
}
