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

class TerminateTest {

    @Test
    void terminateReturnsAllHeldInstancesToResourcePools() {
        Scenario scenario = new Scenario("Test");
        Resource r1 = new Resource("R1", 3, false);
        Resource r2 = new Resource("R2", 2, false);
        scenario.addResource(r1);
        scenario.addResource(r2);

        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        r2.grant(1);
        p1.holdInstances("R2", 1);

        assertEquals(1, r1.getAvailable());
        assertEquals(1, r2.getAvailable());
        assertEquals(3, p1.totalHeld());

        Terminate terminate = new Terminate();
        terminate.apply(scenario, p1);

        assertEquals(3, r1.getAvailable());
        assertEquals(2, r2.getAvailable());
        assertTrue(p1.getHeld().isEmpty());
        assertEquals(0, p1.totalHeld());
    }

    @Test
    void terminateWithMutualExclusionOnLeavesAvailableCountsUnchanged() {
        Scenario scenario = new Scenario("Test");
        Resource r1 = new Resource("R1", 3, false);
        Resource r2 = new Resource("R2", 2, false);
        scenario.addResource(r1);
        scenario.addResource(r2);

        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        r2.grant(1);
        p1.holdInstances("R2", 1);

        assertEquals(1, r1.getAvailable());
        assertEquals(1, r2.getAvailable());

        PreventionToggles toggles = scenario.getPreventionToggles();
        toggles.setMutualExclusion(true);
        scenario.setPreventionToggles(toggles);

        Terminate terminate = new Terminate();
        terminate.apply(scenario, p1);

        assertEquals(1, r1.getAvailable());
        assertEquals(1, r2.getAvailable());
        assertTrue(p1.getHeld().isEmpty());
        assertEquals(ProcessState.TERMINATED, p1.getState());
        assertFalse(p1.isDeadlocked());
    }

    @Test
    void terminateSetsVictimToTerminatedAndClearsDeadlockedAndWaitingFor() {
        Scenario scenario = new Scenario("Test");
        Resource r1 = new Resource("R1", 2, false);
        scenario.addResource(r1);

        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        p1.setState(ProcessState.WAITING);
        p1.setDeadlocked(true);
        p1.setWaitingFor("R1", 1);

        Terminate terminate = new Terminate();
        terminate.apply(scenario, p1);

        assertEquals(ProcessState.TERMINATED, p1.getState());
        assertFalse(p1.isDeadlocked());
        assertFalse(p1.isWaiting());
        assertNull(p1.getWaitingResourceId());
        assertEquals(0, p1.getWaitingCount());
    }

    @Test
    void terminateLeavesOtherProcessesUnchanged() {
        Scenario scenario = new Scenario("Test");
        Resource r1 = new Resource("R1", 3, false);
        scenario.addResource(r1);

        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setDeadlocked(true);
        p1.setWaitingFor("R1", 1);
        p1.recordInstruction();

        r1.grant(1);
        p2.holdInstances("R1", 1);
        p2.setState(ProcessState.WAITING);
        p2.setDeadlocked(true);
        p2.setWaitingFor("R1", 1);
        p2.recordInstruction();
        p2.recordInstruction();

        Terminate terminate = new Terminate();
        terminate.apply(scenario, p1);

        assertEquals(ProcessState.WAITING, p2.getState());
        assertTrue(p2.isDeadlocked());
        assertTrue(p2.isWaiting());
        assertEquals("R1", p2.getWaitingResourceId());
        assertEquals(1, p2.getWaitingCount());
        assertEquals(2, p2.getInstructionsExecuted());
        assertEquals(1, p2.totalHeld());
        assertEquals(1, p2.getHeld().get("R1"));
    }

    @Test
    void terminateThrowsIllegalArgumentExceptionForProcessNotInScenario() {
        Scenario scenario = new Scenario("Test");
        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        Process outsideProcess = new Process("P2", 2);
        Terminate terminate = new Terminate();

        assertThrows(IllegalArgumentException.class, () -> terminate.apply(scenario, outsideProcess));
    }

    @Test
    void terminateThrowsIllegalArgumentExceptionForNullScenarioOrVictim() {
        Terminate terminate = new Terminate();
        Scenario scenario = new Scenario("Test");
        Process p1 = new Process("P1", 1);
        scenario.addProcess(p1);

        assertThrows(IllegalArgumentException.class, () -> terminate.apply(null, p1));
        assertThrows(IllegalArgumentException.class, () -> terminate.apply(scenario, null));
    }
}
