package com.deadlocksim.engine;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.Snapshot;
import com.deadlocksim.model.history.History;
import com.deadlocksim.model.history.HistoryEvent;

import java.util.List;

public class SimulationEngine {
    private Scenario scenario;
    final History history;
    private Snapshot initialSnapshot;

    public SimulationEngine() {
        this.scenario = null;
        this.history = new History();
        this.initialSnapshot = null;
    }

    public void configure(Scenario s) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        List<String> errors = s.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        this.scenario = s;
        this.initialSnapshot = Snapshot.capture(s);
        this.history.clear();
        this.history.add(EventType.STATE_CHANGE, "Scenario loaded", this.initialSnapshot);
    }

    public void reset(boolean confirmed) {
        if (this.scenario == null) {
            throw new IllegalStateException("No scenario loaded");
        }
        if (this.history.size() > 1 && !confirmed) {
            throw new IllegalStateException("Confirmation required to reset");
        }

        this.initialSnapshot.restoreInto(this.scenario);
        this.history.clear();
        this.history.add(EventType.STATE_CHANGE, "Scenario reset", this.initialSnapshot);
    }

    public Scenario getScenario() {
        return scenario;
    }

    public List<HistoryEvent> getHistoryEvents() {
        return history.events();
    }
}
