package com.deadlocksim.model.history;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class History {
    private final List<HistoryEvent> events;
    private int nextSequence;

    public History() {
        this.events = new ArrayList<>();
        this.nextSequence = 0;
    }

    public void add(EventType type, String description, Snapshot snapshot) {
        if (type == null) {
            throw new IllegalArgumentException("EventType must not be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be null or blank");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot must not be null");
        }
        HistoryEvent event = new HistoryEvent(nextSequence++, type, description, snapshot);
        events.add(event);
    }

    public HistoryEvent get(int sequence) {
        if (sequence < 0 || sequence >= events.size()) {
            throw new IllegalArgumentException("Event with sequence " + sequence + " does not exist");
        }
        HistoryEvent event = events.get(sequence);
        if (event.getSequence() != sequence) {
            for (HistoryEvent e : events) {
                if (e.getSequence() == sequence) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Event with sequence " + sequence + " does not exist");
        }
        return event;
    }

    public Snapshot jumpTo(int sequence) {
        return get(sequence).getSnapshot();
    }

    public int size() {
        return events.size();
    }

    public List<HistoryEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public void clear() {
        events.clear();
        nextSequence = 0;
    }
}
