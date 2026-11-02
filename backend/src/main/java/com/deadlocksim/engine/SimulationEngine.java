package com.deadlocksim.engine;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.SafetyChecker;
import com.deadlocksim.model.SafetyResult;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.Snapshot;
import com.deadlocksim.model.history.History;
import com.deadlocksim.model.history.HistoryEvent;

import java.util.ArrayList;
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

    public RequestResult request(String processId, String resourceId, int count) {
        if (scenario == null) {
            throw new IllegalStateException("No scenario loaded");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than zero");
        }
        Process process = scenario.getProcesses().stream()
                .filter(p -> p.getId().equals(processId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown process: " + processId));
        Resource resource = scenario.getResources().stream()
                .filter(r -> r.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown resource: " + resourceId));

        if (process.getState() != ProcessState.RUNNING) {
            throw new IllegalStateException("Process is not RUNNING: " + processId);
        }

        if (scenario.getPreventionToggles().isCircularWait()) {
            List<Resource> resources = scenario.getResources();
            int reqIndex = -1;
            for (int i = 0; i < resources.size(); i++) {
                if (resources.get(i).getId().equals(resourceId)) {
                    reqIndex = i;
                    break;
                }
            }
            int highestHeldIndex = -1;
            for (String heldResId : process.getHeld().keySet()) {
                for (int i = 0; i < resources.size(); i++) {
                    if (resources.get(i).getId().equals(heldResId)) {
                        if (i > highestHeldIndex) {
                            highestHeldIndex = i;
                        }
                        break;
                    }
                }
            }
            if (highestHeldIndex >= 0 && reqIndex <= highestHeldIndex) {
                String reason = "Circular wait prevention: resource " + resourceId + " (index " + reqIndex
                        + ") is not greater than highest held index " + highestHeldIndex;
                history.add(EventType.BLOCK, reason, Snapshot.capture(scenario));
                return new RequestResult(RequestOutcome.REJECTED_PREVENTION, reason);
            }
        }

        if (scenario.getPreventionToggles().isHoldAndWait() && process.totalHeld() > 0) {
            String reason = "Hold and wait prevention: process " + processId + " already holds resources";
            history.add(EventType.BLOCK, reason, Snapshot.capture(scenario));
            return new RequestResult(RequestOutcome.REJECTED_PREVENTION, reason);
        }

        if (isShared()) {
            process.holdInstances(resourceId, count);
            String desc = "Granted " + count + " of shared resource " + resourceId + " to " + processId;
            history.add(EventType.GRANT, desc, Snapshot.capture(scenario));
            return new RequestResult(RequestOutcome.GRANTED, desc);
        }

        if (resource.getAvailable() < count) {
            process.setState(ProcessState.WAITING);
            process.setWaitingFor(resourceId, count);
            String desc = "Process " + processId + " blocked waiting for " + count + " of " + resourceId;
            history.add(EventType.BLOCK, desc, Snapshot.capture(scenario));
            return new RequestResult(RequestOutcome.BLOCKED, desc);
        }

        SafetyResult sr = SafetyChecker.check(scenario, processId, resourceId, count);
        if (!sr.isSafe()) {
            process.setState(ProcessState.WAITING);
            process.setWaitingFor(resourceId, count);
            history.add(EventType.AVOIDANCE_CHECK, sr.getReason(), Snapshot.capture(scenario));
            return new RequestResult(RequestOutcome.DENIED_UNSAFE, sr.getReason());
        }

        resource.grant(count);
        process.holdInstances(resourceId, count);
        history.add(EventType.AVOIDANCE_CHECK, sr.getReason(), Snapshot.capture(scenario));
        String grantDesc = "Granted " + count + " of " + resourceId + " to " + processId;
        history.add(EventType.GRANT, grantDesc, Snapshot.capture(scenario));
        return new RequestResult(RequestOutcome.GRANTED, grantDesc);
    }

    public ReleaseResult release(String processId, String resourceId, int count) {
        if (scenario == null) {
            throw new IllegalStateException("No scenario loaded");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than zero");
        }
        Process process = scenario.getProcesses().stream()
                .filter(p -> p.getId().equals(processId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown process: " + processId));
        Resource resource = scenario.getResources().stream()
                .filter(r -> r.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown resource: " + resourceId));

        int held = process.getHeld().getOrDefault(resourceId, 0);
        if (held < count) {
            throw new IllegalStateException("Process " + processId + " holds only " + held + " of " + resourceId);
        }

        if (!isShared()) {
            resource.release(count);
        }
        process.releaseInstances(resourceId, count);
        history.add(EventType.RELEASE, "Process " + processId + " released " + count + " of " + resourceId, Snapshot.capture(scenario));

        List<String> unblocked = wakeWaitingProcesses();
        return new ReleaseResult(unblocked);
    }

    List<String> wakeWaitingProcesses() {
        List<String> unblocked = new ArrayList<>();
        for (Process wp : scenario.getProcesses()) {
            if (wp.getState() == ProcessState.WAITING && wp.isWaiting()) {
                String wResId = wp.getWaitingResourceId();
                int wCount = wp.getWaitingCount();
                Resource wr = scenario.getResources().stream()
                        .filter(r -> r.getId().equals(wResId))
                        .findFirst()
                        .orElse(null);
                if (wr == null) {
                    continue;
                }

                boolean canSatisfy;
                if (isShared()) {
                    canSatisfy = true;
                } else if (wr.getAvailable() >= wCount) {
                    SafetyResult sr = SafetyChecker.check(scenario, wp.getId(), wResId, wCount);
                    canSatisfy = sr.isSafe();
                } else {
                    canSatisfy = false;
                }

                if (canSatisfy) {
                    if (!isShared()) {
                        wr.grant(wCount);
                    }
                    wp.holdInstances(wResId, wCount);
                    wp.clearWaitingFor();
                    wp.setState(ProcessState.RUNNING);
                    wp.advanceOperation();
                    history.add(EventType.GRANT, "Granted " + wCount + " of " + wResId + " to waiting process " + wp.getId(), Snapshot.capture(scenario));
                    unblocked.add(wp.getId());
                }
            }
        }
        return unblocked;
    }

    private boolean isShared() {
        return scenario != null && scenario.getPreventionToggles().isMutualExclusion();
    }

    public Scenario getScenario() {
        return scenario;
    }

    public List<HistoryEvent> getHistoryEvents() {
        return history.events();
    }
}
