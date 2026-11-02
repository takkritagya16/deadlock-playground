package com.deadlocksim.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Snapshot {
    private final Map<String, ProcessState> processStates;
    private final Map<String, Map<String, Integer>> processHeld;
    private final Map<String, Boolean> processDeadlocked;
    private final Map<String, Integer> available;
    private final Map<String, String> processWaitingResource;
    private final Map<String, Integer> processWaitingCount;
    private final Map<String, Integer> processNextIndex;

    private Snapshot(Map<String, ProcessState> processStates,
                     Map<String, Map<String, Integer>> processHeld,
                     Map<String, Boolean> processDeadlocked,
                     Map<String, Integer> available,
                     Map<String, String> processWaitingResource,
                     Map<String, Integer> processWaitingCount,
                     Map<String, Integer> processNextIndex) {
        this.processStates = processStates;
        this.processHeld = processHeld;
        this.processDeadlocked = processDeadlocked;
        this.available = available;
        this.processWaitingResource = processWaitingResource;
        this.processWaitingCount = processWaitingCount;
        this.processNextIndex = processNextIndex;
    }

    public static Snapshot capture(Scenario s) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        Map<String, ProcessState> states = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> held = new LinkedHashMap<>();
        Map<String, Boolean> deadlocked = new LinkedHashMap<>();
        Map<String, Integer> avail = new LinkedHashMap<>();
        Map<String, String> waitingResource = new LinkedHashMap<>();
        Map<String, Integer> waitingCount = new LinkedHashMap<>();
        Map<String, Integer> nextIndex = new LinkedHashMap<>();

        for (Process p : s.getProcesses()) {
            states.put(p.getId(), p.getState());
            held.put(p.getId(), Collections.unmodifiableMap(new LinkedHashMap<>(p.getHeld())));
            deadlocked.put(p.getId(), p.isDeadlocked());
            if (p.isWaiting()) {
                waitingResource.put(p.getId(), p.getWaitingResourceId());
                waitingCount.put(p.getId(), p.getWaitingCount());
            }
            nextIndex.put(p.getId(), p.getNextOperationIndex());
        }

        for (Resource r : s.getResources()) {
            avail.put(r.getId(), r.getAvailable());
        }

        return new Snapshot(
                Collections.unmodifiableMap(states),
                Collections.unmodifiableMap(held),
                Collections.unmodifiableMap(deadlocked),
                Collections.unmodifiableMap(avail),
                Collections.unmodifiableMap(waitingResource),
                Collections.unmodifiableMap(waitingCount),
                Collections.unmodifiableMap(nextIndex)
        );
    }

    public void restoreInto(Scenario s) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        Set<String> scenarioProcessIds = s.getProcesses().stream()
                .map(Process::getId)
                .collect(Collectors.toSet());
        if (!scenarioProcessIds.equals(processStates.keySet())) {
            throw new IllegalArgumentException("Scenario process IDs do not match snapshot process IDs");
        }

        Set<String> scenarioResourceIds = s.getResources().stream()
                .map(Resource::getId)
                .collect(Collectors.toSet());
        if (!scenarioResourceIds.equals(available.keySet())) {
            throw new IllegalArgumentException("Scenario resource IDs do not match snapshot resource IDs");
        }

        for (Process p : s.getProcesses()) {
            String pid = p.getId();
            p.setState(processStates.get(pid));
            p.setDeadlocked(processDeadlocked.get(pid));
            p.restoreHeld(processHeld.get(pid));
            String waitRes = processWaitingResource.get(pid);
            Integer waitCnt = processWaitingCount.get(pid);
            if (waitRes != null && waitCnt != null) {
                p.setWaitingFor(waitRes, waitCnt);
            } else {
                p.clearWaitingFor();
            }
            Integer idx = processNextIndex.get(pid);
            if (idx != null) {
                p.restoreNextOperationIndex(idx);
            }
        }

        for (Resource r : s.getResources()) {
            String rid = r.getId();
            r.restoreAvailable(available.get(rid));
        }
    }

    public Map<String, ProcessState> getProcessStates() {
        return processStates;
    }

    public Map<String, Map<String, Integer>> getProcessHeld() {
        return processHeld;
    }

    public Map<String, Boolean> getProcessDeadlocked() {
        return processDeadlocked;
    }

    public Map<String, Integer> getAvailable() {
        return available;
    }

    public Map<String, String> getProcessWaitingResource() {
        return processWaitingResource;
    }

    public Map<String, Integer> getProcessWaitingCount() {
        return processWaitingCount;
    }

    public Map<String, Integer> getProcessNextIndex() {
        return processNextIndex;
    }
}
