package com.deadlocksim.engine;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.PreventionToggles;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.history.HistoryEvent;
import com.deadlocksim.model.victim.RecoveryCostPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectRecoverTest {

    @Test
    void twoProcessDeadlockDetectedWithCycleStrategyAndRightProcesses() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Two Process Deadlock");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addResource(r1);
        scenario.addResource(r2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);

        engine.configure(scenario);

        DetectReport report = engine.detect();
        assertEquals("CYCLE", report.strategy());
        assertTrue(report.result().isDeadlocked());
        assertEquals(List.of("P1", "P2"), report.result().getProcessIds());
    }

    @Test
    void threeResourceBankersCaseDetectedWithBankersStrategy() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Bankers Multi-Instance Deadlock");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 2, false);
        Resource r2 = new Resource("R2", 2, false);
        Resource r3 = new Resource("R3", 2, false);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addResource(r3);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 2);

        r2.grant(2);
        p2.holdInstances("R2", 2);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 2);

        engine.configure(scenario);

        DetectReport report = engine.detect();
        assertEquals("BANKERS", report.strategy());
        assertTrue(report.result().isDeadlocked());
        assertEquals(List.of("P1", "P2"), report.result().getProcessIds());
    }

    @Test
    void detectWithNoWaitingProcessThrowsIllegalStateException() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("No Waiting");
        scenario.addProcess(new Process("P1", 1));
        scenario.addResource(new Resource("R1", 1, false));
        engine.configure(scenario);

        IllegalStateException ex = assertThrows(IllegalStateException.class, engine::detect);
        assertEquals("No waiting process to check", ex.getMessage());
    }

    @Test
    void detectWithNoDeadlockReturnsNoDeadlockAndRecordsDetect() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Waiting But No Deadlock");
        Process p1 = new Process("P1", 1);
        Resource r1 = new Resource("R1", 1, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);

        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R1", 1);

        engine.configure(scenario);

        DetectReport report = engine.detect();
        assertFalse(report.result().isDeadlocked());
        assertTrue(report.result().getProcessIds().isEmpty());

        List<HistoryEvent> events = engine.getHistoryEvents();
        assertEquals(EventType.DETECT, events.get(events.size() - 1).getType());
    }

    @Test
    void recoverWithTerminateResolvesDeadlockInOneRoundVictimTerminatedHistoryEndsWithResolved() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Recover Terminate");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addResource(r1);
        scenario.addResource(r2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);

        engine.configure(scenario);

        RecoveryReport report = engine.recover(new RecoveryCostPolicy(), false);

        assertTrue(report.resolved());
        assertEquals(1, report.rounds().size());
        assertEquals("TERMINATE", report.rounds().get(0).strategy());

        String victimId = report.rounds().get(0).victimId();
        Process victim = scenario.getProcesses().stream()
                .filter(p -> p.getId().equals(victimId))
                .findFirst()
                .orElseThrow();
        assertEquals(ProcessState.TERMINATED, victim.getState());
        assertEquals(0, victim.totalHeld());

        List<HistoryEvent> events = engine.getHistoryEvents();
        assertEquals(EventType.RESOLVED, events.get(events.size() - 1).getType());
    }

    @Test
    void recoverWithDefaultRecoveryCostPolicyPicksVictimThatPolicyChooses() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Victim Policy Check");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addResource(r1);
        scenario.addResource(r2);

        // P1 cost: 1 (held) + 5 (instructions) = 6
        r1.grant(1);
        p1.holdInstances("R1", 1);
        for (int i = 0; i < 5; i++) {
            p1.recordInstruction();
        }
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);

        // P2 cost: 1 (held) + 1 (instruction) = 2
        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.recordInstruction();
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);

        engine.configure(scenario);

        RecoveryReport report = engine.recover(new RecoveryCostPolicy(), false);
        assertTrue(report.resolved());
        assertEquals("P2", report.rounds().get(0).victimId());
    }

    @Test
    void recoverWithUsePreemptAndNoPreemptionToggleOffThrowsIllegalStateException() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Preempt Disabled");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addResource(r1);
        scenario.addResource(r2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);

        PreventionToggles toggles = scenario.getPreventionToggles();
        toggles.setNoPreemption(false);
        scenario.setPreventionToggles(toggles);

        engine.configure(scenario);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> engine.recover(new RecoveryCostPolicy(), true));
        assertEquals("Preemption is disabled", ex.getMessage());
    }

    @Test
    void recoverWithUsePreemptAndToggleOnResolvesDeadlockAndVictimIsRunningWithNoHoldings() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Preempt Enabled");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addResource(r1);
        scenario.addResource(r2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);

        PreventionToggles toggles = scenario.getPreventionToggles();
        toggles.setNoPreemption(true);
        scenario.setPreventionToggles(toggles);

        engine.configure(scenario);

        RecoveryReport report = engine.recover(new RecoveryCostPolicy(), true);
        assertTrue(report.resolved());
        assertEquals("PREEMPT", report.rounds().get(0).strategy());

        String victimId = report.rounds().get(0).victimId();
        Process victim = scenario.getProcesses().stream()
                .filter(p -> p.getId().equals(victimId))
                .findFirst()
                .orElseThrow();
        assertEquals(ProcessState.RUNNING, victim.getState());
        assertEquals(0, victim.totalHeld());
        assertTrue(victim.getHeld().isEmpty());
    }

    @Test
    void recoverWithNoDeadlockThrowsIllegalStateException() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("No Deadlock Recover");
        Process p1 = new Process("P1", 1);
        Resource r1 = new Resource("R1", 1, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);

        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R1", 1);

        engine.configure(scenario);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> engine.recover(new RecoveryCostPolicy(), false));
        assertEquals("No deadlock to recover from", ex.getMessage());
    }

    @Test
    void historyRecordsDetectVictimSelectedTerminateDetectAndResolvedInThatOrder() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("History Sequence Check");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addResource(r1);
        scenario.addResource(r2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);

        engine.configure(scenario);

        engine.recover(new RecoveryCostPolicy(), false);

        List<EventType> sequence = engine.getHistoryEvents().stream()
                .map(HistoryEvent::getType)
                .filter(t -> t == EventType.DETECT
                        || t == EventType.VICTIM_SELECTED
                        || t == EventType.TERMINATE
                        || t == EventType.RESOLVED)
                .toList();

        assertEquals(List.of(
                EventType.DETECT,
                EventType.VICTIM_SELECTED,
                EventType.TERMINATE,
                EventType.DETECT,
                EventType.RESOLVED
        ), sequence);
    }
}

