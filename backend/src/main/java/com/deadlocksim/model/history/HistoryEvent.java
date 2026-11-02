package com.deadlocksim.model.history;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Snapshot;

import java.util.Objects;

public final class HistoryEvent {
    private final int sequence;
    private final EventType type;
    private final String description;
    private final Snapshot snapshot;

    public HistoryEvent(int sequence, EventType type, String description, Snapshot snapshot) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Sequence must not be negative");
        }
        if (type == null) {
            throw new IllegalArgumentException("EventType must not be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be null or blank");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot must not be null");
        }
        this.sequence = sequence;
        this.type = type;
        this.description = description;
        this.snapshot = snapshot;
    }

    public int getSequence() {
        return sequence;
    }

    public EventType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HistoryEvent that = (HistoryEvent) o;
        return sequence == that.sequence &&
                type == that.type &&
                Objects.equals(description, that.description) &&
                Objects.equals(snapshot, that.snapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, type, description, snapshot);
    }

    @Override
    public String toString() {
        return "HistoryEvent{" +
                "sequence=" + sequence +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", snapshot=" + snapshot +
                '}';
    }
}
