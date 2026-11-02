package com.deadlocksim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BankersCore {

    record FinishResult(List<String> sequence, List<String> stuck) {
        FinishResult {
            sequence = Collections.unmodifiableList(new ArrayList<>(sequence));
            stuck = Collections.unmodifiableList(new ArrayList<>(stuck));
        }
    }

    private BankersCore() {
    }

    static FinishResult run(Scenario s, Map<String, Integer> workAvailable) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        Map<String, Integer> work = new HashMap<>(workAvailable != null ? workAvailable : Collections.emptyMap());

        List<String> sequence = new ArrayList<>();
        Set<String> finished = new HashSet<>();

        List<String> alreadyFinished = new ArrayList<>();
        for (Process p : s.getProcesses()) {
            boolean isCompletedOrTerminated = p.getState() == ProcessState.COMPLETED
                    || p.getState() == ProcessState.TERMINATED;
            boolean holdsNothingAndNotWaiting = p.totalHeld() == 0 && !p.isWaiting();
            if (isCompletedOrTerminated || holdsNothingAndNotWaiting) {
                alreadyFinished.add(p.getId());
                for (Map.Entry<String, Integer> entry : p.getHeld().entrySet()) {
                    work.put(entry.getKey(), work.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            }
        }
        Collections.sort(alreadyFinished);
        for (String id : alreadyFinished) {
            sequence.add(id);
            finished.add(id);
        }

        boolean progress = true;
        while (progress) {
            progress = false;
            for (Process p : s.getProcesses()) {
                if (finished.contains(p.getId())) {
                    continue;
                }

                boolean canFinish = false;
                if (!p.isWaiting()) {
                    canFinish = true;
                } else {
                    int availableForWait = work.getOrDefault(p.getWaitingResourceId(), 0);
                    if (p.getWaitingCount() <= availableForWait) {
                        canFinish = true;
                    }
                }

                if (canFinish) {
                    for (Map.Entry<String, Integer> entry : p.getHeld().entrySet()) {
                        work.put(entry.getKey(), work.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }
                    sequence.add(p.getId());
                    finished.add(p.getId());
                    progress = true;
                    break;
                }
            }
        }

        List<String> stuck = new ArrayList<>();
        for (Process p : s.getProcesses()) {
            if (!finished.contains(p.getId())) {
                stuck.add(p.getId());
            }
        }

        return new FinishResult(sequence, stuck);
    }
}
