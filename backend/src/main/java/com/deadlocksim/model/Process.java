package com.deadlocksim.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Process {
    private final String id;
    private final int priority;
    private ProcessState state;
    private boolean deadlocked;
    private final Map<String, Integer> held;
    private int instructionsExecuted;
    private String waitingResourceId;
    private int waitingCount;
    private int nextOperationIndex;

    public Process(String id, int priority) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Process ID must not be null or blank");
        }
        this.id = id;
        this.priority = priority;
        this.state = ProcessState.RUNNING;
        this.deadlocked = false;
        this.held = new LinkedHashMap<>();
        this.instructionsExecuted = 0;
        this.waitingResourceId = null;
        this.waitingCount = 0;
        this.nextOperationIndex = 0;
    }

    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public ProcessState getState() {
        return state;
    }

    public boolean isDeadlocked() {
        return deadlocked;
    }

    public Map<String, Integer> getHeld() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(held));
    }

    public int getInstructionsExecuted() {
        return instructionsExecuted;
    }

    public int getNextOperationIndex() {
        return nextOperationIndex;
    }

    public void advanceOperation() {
        this.nextOperationIndex++;
    }

    void restoreNextOperationIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Next operation index must not be negative");
        }
        this.nextOperationIndex = index;
    }

    public void setState(ProcessState s) {
        if (s == null) {
            throw new IllegalArgumentException("ProcessState must not be null");
        }
        this.state = s;
    }

    public void setDeadlocked(boolean d) {
        this.deadlocked = d;
    }

    public boolean isBlocked() {
        return this.state == ProcessState.WAITING;
    }

    public void setWaitingFor(String resourceId, int count) {
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Resource ID must not be null or blank");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than zero");
        }
        this.waitingResourceId = resourceId;
        this.waitingCount = count;
    }

    public void clearWaitingFor() {
        this.waitingResourceId = null;
        this.waitingCount = 0;
    }

    public String getWaitingResourceId() {
        return waitingResourceId;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public boolean isWaiting() {
        return waitingResourceId != null;
    }

    public void holdInstances(String resourceId, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Instance count must be greater than zero");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Resource ID must not be null or blank");
        }
        held.put(resourceId, held.getOrDefault(resourceId, 0) + n);
    }

    public void releaseInstances(String resourceId, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Instance count must be greater than zero");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Resource ID must not be null or blank");
        }
        int current = held.getOrDefault(resourceId, 0);
        if (current < n) {
            throw new IllegalStateException("Process holds fewer instances than requested to release");
        }
        if (current == n) {
            held.remove(resourceId);
        } else {
            held.put(resourceId, current - n);
        }
    }

    public int totalHeld() {
        int total = 0;
        for (int count : held.values()) {
            total += count;
        }
        return total;
    }

    public void recordInstruction() {
        this.instructionsExecuted++;
    }

    public void rollbackState() {
        this.state = ProcessState.RUNNING;
        this.deadlocked = false;
        this.held.clear();
        this.waitingResourceId = null;
        this.waitingCount = 0;
        this.instructionsExecuted = 0;
        this.nextOperationIndex = 0;
    }

    void restoreHeld(Map<String, Integer> newHeld) {
        if (newHeld == null) {
            throw new IllegalArgumentException("Held map must not be null");
        }
        for (Map.Entry<String, Integer> entry : newHeld.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Resource ID must not be null or blank");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Instance count must be greater than zero");
            }
        }
        this.held.clear();
        this.held.putAll(newHeld);
    }
}
