package com.deadlocksim.model.recovery;

import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;

import java.util.HashMap;
import java.util.Map;

public class Terminate implements RecoveryStrategy {

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

        boolean shared = s.getPreventionToggles().isMutualExclusion();

        Map<String, Resource> resourceMap = new HashMap<>();
        for (Resource r : s.getResources()) {
            resourceMap.put(r.getId(), r);
        }

        for (Map.Entry<String, Integer> entry : new HashMap<>(victim.getHeld()).entrySet()) {
            String resourceId = entry.getKey();
            int count = entry.getValue();
            Resource r = resourceMap.get(resourceId);
            if (r == null) {
                throw new IllegalStateException("Resource " + resourceId + " held by victim is not in the scenario");
            }
            if (!shared) {
                r.release(count);
            }
            victim.releaseInstances(resourceId, count);
        }

        victim.setState(ProcessState.TERMINATED);
        victim.setDeadlocked(false);
        victim.clearWaitingFor();
    }
}
