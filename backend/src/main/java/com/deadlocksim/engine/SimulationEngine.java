package com.deadlocksim.engine;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Graph;
import com.deadlocksim.model.Operation;
import com.deadlocksim.model.OperationType;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.SafetyChecker;
import com.deadlocksim.model.SafetyResult;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.Snapshot;
import com.deadlocksim.model.detection.BankersDetection;
import com.deadlocksim.model.detection.CycleDetection;
import com.deadlocksim.model.detection.DetectionResult;
import com.deadlocksim.model.history.History;
import com.deadlocksim.model.history.HistoryEvent;
import com.deadlocksim.model.recovery.Preempt;
import com.deadlocksim.model.recovery.Terminate;
import com.deadlocksim.model.victim.VictimPolicy;

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

    public StepResult step() {
        if (scenario == null) {
            throw new IllegalStateException("No scenario loaded");
        }

        Process targetProcess = null;
        List<Operation> targetOps = null;
        for (Process p : scenario.getProcesses()) {
            List<Operation> ops = scenario.getOperations(p.getId());
            if (p.getState() == ProcessState.RUNNING && p.getNextOperationIndex() < ops.size()) {
                targetProcess = p;
                targetOps = ops;
                break;
            }
        }

        if (targetProcess == null) {
            boolean allFinished = scenario.getProcesses().stream()
                    .allMatch(p -> p.getNextOperationIndex() >= scenario.getOperations(p.getId()).size());
            if (allFinished) {
                return new StepResult("Scenario finished", true);
            } else {
                return new StepResult("No runnable process; deadlock or waiting", false);
            }
        }

        Operation op = targetOps.get(targetProcess.getNextOperationIndex());
        String description;

        if (op.getType() == OperationType.REQUEST) {
            RequestResult rr = request(targetProcess.getId(), op.getResourceId(), op.getCount());
            if (rr.outcome() == RequestOutcome.GRANTED) {
                targetProcess.advanceOperation();
                description = targetProcess.getId() + " requested and was granted " + op.getCount() + " of " + op.getResourceId();
            } else {
                description = targetProcess.getId() + " request " + op.getResourceId() + " (" + op.getCount() + "): " + rr.outcome();
            }
        } else {
            release(targetProcess.getId(), op.getResourceId(), op.getCount());
            targetProcess.advanceOperation();
            description = targetProcess.getId() + " released " + op.getCount() + " of " + op.getResourceId();
        }

        for (Process p : scenario.getProcesses()) {
            if (p.getState() == ProcessState.RUNNING
                    && p.getNextOperationIndex() >= scenario.getOperations(p.getId()).size()
                    && !p.isWaiting()) {
                p.setState(ProcessState.COMPLETED);
                history.add(EventType.STATE_CHANGE, p.getId() + " completed", Snapshot.capture(scenario));
            }
        }

        boolean finished = scenario.getProcesses().stream()
                .allMatch(p -> p.getNextOperationIndex() >= scenario.getOperations(p.getId()).size());

        return new StepResult(description, finished);
    }

    public DetectReport detect() {
        if (scenario == null) {
            throw new IllegalStateException("No scenario loaded");
        }
        boolean hasWaiting = scenario.getProcesses().stream().anyMatch(Process::isWaiting);
        if (!hasWaiting) {
            throw new IllegalStateException("No waiting process to check");
        }

        boolean allSingle = scenario.getResources().stream().allMatch(r -> r.getTotal() == 1);
        String strategyName = allSingle ? "CYCLE" : "BANKERS";
        Graph g = new Graph(scenario);
        DetectionResult result = allSingle
                ? new CycleDetection().detect(g)
                : new BankersDetection().detect(g);

        String desc = result.isDeadlocked()
                ? "Deadlock detected: " + result.getProcessIds()
                : "No deadlock detected";
        history.add(EventType.DETECT, desc, Snapshot.capture(scenario));

        return new DetectReport(strategyName, result);
    }

    public RecoveryReport recover(VictimPolicy policy, boolean usePreempt) {
        if (scenario == null) {
            throw new IllegalStateException("No scenario loaded");
        }
        if (policy == null) {
            throw new IllegalArgumentException("Victim policy must not be null");
        }

        DetectReport initialReport = detect();
        if (!initialReport.result().isDeadlocked()) {
            throw new IllegalStateException("No deadlock to recover from");
        }

        List<RecoveryRound> rounds = new ArrayList<>();
        DetectionResult currentResult = initialReport.result();
        int maxRounds = scenario.getProcesses().size();
        int roundCount = 0;
        boolean resolved = false;

        while (!currentResult.getProcessIds().isEmpty()) {
            if (roundCount >= maxRounds) {
                history.add(EventType.RESOLVED, "Recovery failed", Snapshot.capture(scenario));
                break;
            }
            roundCount++;

            List<String> deadlockedIds = currentResult.getProcessIds();
            List<Process> deadlockedProcesses = new ArrayList<>();
            for (Process p : scenario.getProcesses()) {
                if (deadlockedIds.contains(p.getId())) {
                    deadlockedProcesses.add(p);
                }
            }
            Process victim = policy.selectVictim(deadlockedProcesses);
            history.add(EventType.VICTIM_SELECTED, "Victim selected: " + victim.getId(), Snapshot.capture(scenario));

            String strategyName;
            if (usePreempt) {
                if (!scenario.getPreventionToggles().isNoPreemption()) {
                    throw new IllegalStateException("Preemption is disabled");
                }
                strategyName = "PREEMPT";
                String resId = victim.getHeld().keySet().stream()
                        .min(String::compareTo)
                        .orElseThrow(() -> new IllegalStateException("Victim holds no resources to preempt"));
                new Preempt(resId).apply(scenario, victim);
                history.add(EventType.PREEMPT, "Preempted resource " + resId + " from victim " + victim.getId(), Snapshot.capture(scenario));
                history.add(EventType.ROLLBACK, "Rolled back victim " + victim.getId(), Snapshot.capture(scenario));
            } else {
                strategyName = "TERMINATE";
                new Terminate().apply(scenario, victim);
                history.add(EventType.TERMINATE, "Terminated victim " + victim.getId(), Snapshot.capture(scenario));
            }

            wakeWaitingProcesses();

            boolean hasWaiting = scenario.getProcesses().stream().anyMatch(Process::isWaiting);
            DetectReport nextReport;
            if (!hasWaiting) {
                for (Process p : scenario.getProcesses()) {
                    p.setDeadlocked(false);
                }
                boolean allSingle = scenario.getResources().stream().allMatch(r -> r.getTotal() == 1);
                history.add(EventType.DETECT, "No deadlock detected", Snapshot.capture(scenario));
                nextReport = new DetectReport(allSingle ? "CYCLE" : "BANKERS", DetectionResult.none());
            } else {
                nextReport = detect();
            }

            currentResult = nextReport.result();
            boolean roundResolved = !currentResult.isDeadlocked();
            rounds.add(new RecoveryRound(victim.getId(), strategyName, roundResolved));

            if (roundResolved) {
                history.add(EventType.RESOLVED, "Deadlock resolved", Snapshot.capture(scenario));
                resolved = true;
                break;
            }
        }

        return new RecoveryReport(rounds, resolved);
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
