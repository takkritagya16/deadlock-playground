package com.deadlocksim.model.recovery;

import com.deadlocksim.model.PreventionToggles;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreemptTest {

    @Test
    void preemptReturnsOneInstanceOfChosenResourceAndEveryOtherHeldInstance() {
        Scenario scenario = new Scenario("Preempt Test");
        Resource r1 = new Resource("R1", 3, false);
        Resource r2 = new Resource("R2", 4, false);
        scenario.addResource(r1);
        scenario.addResource(r2);

        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        r2.grant(3);
        p1.holdInstances("R2", 3);

        assertEquals(1, r1.getAvailable());
        assertEquals(1, r2.getAvailable());

        Preempt preempt = new Preempt("R1");
        preempt.apply(scenario, p1);

        assertEquals(3, r1.getAvailable());
        assertEquals(4, r2.getAvailable());
        assertTrue(p1.getHeld().isEmpty());
        assertEquals(0, p1.totalHeld());
    }

    @Test
    void preemptSetsVictimToRunningWithNoHoldingsNoWaitingAndNotDeadlocked() {
        Scenario scenario = new Scenario("Preempt Test");
        Resource r1 = new Resource("R1", 2, false);
        scenario.addResource(r1);

        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setDeadlocked(true);
        p1.setWaitingFor("R1", 1);
        p1.recordInstruction();
        p1.advanceOperation();

        Preempt preempt = new Preempt("R1");
        preempt.apply(scenario, p1);

        assertEquals(ProcessState.RUNNING, p1.getState());
        assertFalse(p1.isDeadlocked());
        assertFalse(p1.isWaiting());
        assertNull(p1.getWaitingResourceId());
        assertEquals(0, p1.getWaitingCount());
        assertEquals(0, p1.getInstructionsExecuted());
        assertEquals(0, p1.getNextOperationIndex());
        assertTrue(p1.getHeld().isEmpty());
    }

    @Test
    void preemptWithMutualExclusionOnLeavesAvailableCountsUnchanged() {
        Scenario scenario = new Scenario("Preempt Test");
        Resource r1 = new Resource("R1", 3, false);
        Resource r2 = new Resource("R2", 4, false);
        scenario.addResource(r1);
        scenario.addResource(r2);

        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        r2.grant(3);
        p1.holdInstances("R2", 3);

        assertEquals(1, r1.getAvailable());
        assertEquals(1, r2.getAvailable());

        PreventionToggles toggles = scenario.getPreventionToggles();
        toggles.setMutualExclusion(true);
        scenario.setPreventionToggles(toggles);

        Preempt preempt = new Preempt("R1");
        preempt.apply(scenario, p1);

        assertEquals(1, r1.getAvailable());
        assertEquals(1, r2.getAvailable());
        assertEquals(ProcessState.RUNNING, p1.getState());
        assertTrue(p1.getHeld().isEmpty());
    }

    @Test
    void resourceVictimDoesNotHoldThrowsIllegalStateException() {
        Scenario scenario = new Scenario("Preempt Test");
        Resource r1 = new Resource("R1", 2, false);
        Resource r2 = new Resource("R2", 2, false);
        scenario.addResource(r1);
        scenario.addResource(r2);

        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        r1.grant(1);
        p1.holdInstances("R1", 1);

        Preempt preempt = new Preempt("R2");
        assertThrows(IllegalStateException.class, () -> preempt.apply(scenario, p1));
    }

    @Test
    void victimNotInScenarioThrowsIllegalArgumentException() {
        Scenario scenario = new Scenario("Preempt Test");
        Resource r1 = new Resource("R1", 2, false);
        scenario.addResource(r1);

        Process outside = new Process("P99", 1);
        outside.holdInstances("R1", 1);

        Preempt preempt = new Preempt("R1");
        assertThrows(IllegalArgumentException.class, () -> preempt.apply(scenario, outside));
        assertThrows(IllegalArgumentException.class, () -> preempt.apply(null, outside));
        assertThrows(IllegalArgumentException.class, () -> preempt.apply(scenario, null));
    }

    @Test
    void noOtherProcessChanges() {
        Scenario scenario = new Scenario("Preempt Test");
        Resource r1 = new Resource("R1", 3, false);
        scenario.addResource(r1);

        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        r1.grant(1);
        p1.holdInstances("R1", 1);

        r1.grant(1);
        p2.holdInstances("R1", 1);
        p2.setState(ProcessState.WAITING);
        p2.setDeadlocked(true);
        p2.setWaitingFor("R1", 1);
        p2.recordInstruction();
        p2.advanceOperation();

        Preempt preempt = new Preempt("R1");
        preempt.apply(scenario, p1);

        assertEquals(ProcessState.WAITING, p2.getState());
        assertTrue(p2.isDeadlocked());
        assertTrue(p2.isWaiting());
        assertEquals("R1", p2.getWaitingResourceId());
        assertEquals(1, p2.getWaitingCount());
        assertEquals(1, p2.getInstructionsExecuted());
        assertEquals(1, p2.getNextOperationIndex());
        assertEquals(1, p2.totalHeld());
    }

    @Test
    void invalidConstructorArgumentThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Preempt(null));
        assertThrows(IllegalArgumentException.class, () -> new Preempt(""));
        assertThrows(IllegalArgumentException.class, () -> new Preempt("   "));
        Preempt p = new Preempt("R1");
        assertEquals("R1", p.getResourceId());
    }
}

