package com.deadlocksim.model.recovery;

import com.deadlocksim.model.Process;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;

import java.util.HashMap;
import java.util.Map;

public class Preempt implements RecoveryStrategy {
    private final String resourceId;

    public Preempt(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Resource ID must not be null or blank");
        }
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    @Override
    public void apply(Scenario s, Process victim) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        if (victim == null) {
            throw new IllegalArgumentException("Victim must not be null");
        }
        if (!s.getProcesses().contains(victim)) {
            throw new IllegalArgumentException("Victim process is not in the scenario");
        }

        int heldCount = victim.getHeld().getOrDefault(resourceId, 0);
        if (heldCount < 1) {
            throw new IllegalStateException("Victim does not hold resource: " + resourceId);
        }

        boolean shared = s.getPreventionToggles().isMutualExclusion();

        Map<String, Resource> resourceMap = new HashMap<>();
        for (Resource r : s.getResources()) {
            resourceMap.put(r.getId(), r);
        }

        Resource targetResource = resourceMap.get(resourceId);
        if (targetResource == null) {
            throw new IllegalStateException("Resource " + resourceId + " is not in the scenario");
        }

        if (!shared) {
            targetResource.release(1);
            for (Map.Entry<String, Integer> entry : victim.getHeld().entrySet()) {
                String resId = entry.getKey();
                int count = entry.getValue();
                if (resId.equals(resourceId)) {
                    int remaining = count - 1;
                    if (remaining > 0) {
                        targetResource.release(remaining);
                    }
                } else {
                    Resource r = resourceMap.get(resId);
                    if (r == null) {
                        throw new IllegalStateException("Resource " + resId + " is not in the scenario");
                    }
                    r.release(count);
                }
            }
        }

        victim.rollbackState();
    }
}

