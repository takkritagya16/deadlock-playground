package com.deadlocksim.model;

import java.util.Objects;

public class Operation {
    private final String processId;
    private final OperationType type;
    private final String resourceId;
    private final int count;

    public Operation(String processId, OperationType type, String resourceId, int count) {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Process ID must not be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Operation type must not be null");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Resource ID must not be null or blank");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than zero");
        }
        this.processId = processId;
        this.type = type;
        this.resourceId = resourceId;
        this.count = count;
    }

    public String getProcessId() {
        return processId;
    }

    public OperationType getType() {
        return type;
    }

    public String getResourceId() {
        return resourceId;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Operation operation = (Operation) o;
        return count == operation.count &&
                Objects.equals(processId, operation.processId) &&
                type == operation.type &&
                Objects.equals(resourceId, operation.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, type, resourceId, count);
    }

    @Override
    public String toString() {
        return "Operation{" +
                "processId='" + processId + '\'' +
                ", type=" + type +
                ", resourceId='" + resourceId + '\'' +
                ", count=" + count +
                '}';
    }
}
