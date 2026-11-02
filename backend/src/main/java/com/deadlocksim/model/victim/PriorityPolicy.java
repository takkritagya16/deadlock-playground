package com.deadlocksim.model.victim;

import com.deadlocksim.model.Process;

import java.util.List;

public class PriorityPolicy implements VictimPolicy {

    @Override
    public Process selectVictim(List<Process> deadlocked) {
        if (deadlocked == null || deadlocked.isEmpty()) {
            throw new IllegalArgumentException("Deadlocked process list must not be null or empty");
        }

        Process victim = deadlocked.get(0);
        for (int i = 1; i < deadlocked.size(); i++) {
            Process current = deadlocked.get(i);
            if (current.getPriority() < victim.getPriority()) {
                victim = current;
            } else if (current.getPriority() == victim.getPriority()) {
                if (current.getId().compareTo(victim.getId()) < 0) {
                    victim = current;
                }
            }
        }
        return victim;
    }
}
