package com.deadlocksim.model.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DetectionResult {
    private final boolean deadlocked;
    private final List<String> processIds;
    private final List<String> cycle;

    private static final DetectionResult NONE = new DetectionResult(
            false,
            Collections.emptyList(),
            Collections.emptyList()
    );

    private DetectionResult(boolean deadlocked, List<String> processIds, List<String> cycle) {
        this.deadlocked = deadlocked;
        this.processIds = processIds;
        this.cycle = cycle;
    }

    public static DetectionResult none() {
        return NONE;
    }

    public static DetectionResult of(List<String> processIds, List<String> cycle) {
        List<String> sortedProcesses = new ArrayList<>(processIds != null ? processIds : Collections.emptyList());
        Collections.sort(sortedProcesses);
        List<String> cycleList = new ArrayList<>(cycle != null ? cycle : Collections.emptyList());

        return new DetectionResult(
                true,
                Collections.unmodifiableList(sortedProcesses),
                Collections.unmodifiableList(cycleList)
        );
    }

    public boolean isDeadlocked() {
        return deadlocked;
    }

    public List<String> getProcessIds() {
        return processIds;
    }

    public List<String> getCycle() {
        return cycle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DetectionResult that = (DetectionResult) o;
        return deadlocked == that.deadlocked &&
                Objects.equals(processIds, that.processIds) &&
                Objects.equals(cycle, that.cycle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deadlocked, processIds, cycle);
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
                "deadlocked=" + deadlocked +
                ", processIds=" + processIds +
                ", cycle=" + cycle +
                '}';
    }
}
