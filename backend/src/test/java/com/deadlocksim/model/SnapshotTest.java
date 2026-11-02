package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotTest {

    @Test
    void captureRecordsCurrentStatesHoldingsAndAvailableCounts() {
        Scenario scenario = new Scenario("Test Scenario");
        Resource r1 = new Resource("R1", 3, false);
        Resource r2 = new Resource("R2", 2, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        p1.setState(ProcessState.WAITING);
        p1.setDeadlocked(true);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        Snapshot snapshot = Snapshot.capture(scenario);

        assertEquals(ProcessState.WAITING, snapshot.getProcessStates().get("P1"));
        assertEquals(ProcessState.RUNNING, snapshot.getProcessStates().get("P2"));

        assertTrue(snapshot.getProcessDeadlocked().get("P1"));
        assertFalse(snapshot.getProcessDeadlocked().get("P2"));

        assertEquals(Map.of("R1", 2), snapshot.getProcessHeld().get("P1"));
        assertTrue(snapshot.getProcessHeld().get("P2").isEmpty());

        assertEquals(1, snapshot.getAvailable().get("R1"));
        assertEquals(2, snapshot.getAvailable().get("R2"));
    }

    @Test
    void changingScenarioAfterCaptureDoesNotChangeSnapshot() {
        Scenario scenario = new Scenario("Test Scenario");
        Resource r1 = new Resource("R1", 4, false);
        Process p1 = new Process("P1", 1);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        scenario.addResource(r1);
        scenario.addProcess(p1);

        Snapshot snapshot = Snapshot.capture(scenario);

        p1.setState(ProcessState.TERMINATED);
        p1.setDeadlocked(true);
        p1.releaseInstances("R1", 2);
        r1.release(2);

        assertEquals(ProcessState.RUNNING, snapshot.getProcessStates().get("P1"));
        assertFalse(snapshot.getProcessDeadlocked().get("P1"));
        assertEquals(Map.of("R1", 2), snapshot.getProcessHeld().get("P1"));
        assertEquals(2, snapshot.getAvailable().get("R1"));
    }

    @Test
    void restoreIntoPutsScenarioBackToCapturedState() {
        Scenario scenario = new Scenario("Test Scenario");
        Resource r1 = new Resource("R1", 5, false);
        Process p1 = new Process("P1", 1);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        p1.setState(ProcessState.WAITING);
        p1.setDeadlocked(true);
        scenario.addResource(r1);
        scenario.addProcess(p1);

        Snapshot snapshot = Snapshot.capture(scenario);

        p1.setState(ProcessState.RUNNING);
        p1.setDeadlocked(false);
        p1.releaseInstances("R1", 1);
        r1.release(1);

        assertEquals(ProcessState.RUNNING, p1.getState());
        assertFalse(p1.isDeadlocked());
        assertEquals(1, p1.getHeld().get("R1"));
        assertEquals(4, r1.getAvailable());

        snapshot.restoreInto(scenario);

        assertEquals(ProcessState.WAITING, p1.getState());
        assertTrue(p1.isDeadlocked());
        assertEquals(2, p1.getHeld().get("R1"));
        assertEquals(3, r1.getAvailable());
    }

    @Test
    void snapshotCapturedWhileWaitingRestoresWaitingStateAfterScenarioChanges() {
        Scenario scenario = new Scenario("Test Scenario");
        Resource r1 = new Resource("R1", 3, false);
        Resource r2 = new Resource("R2", 3, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);

        p1.setWaitingFor("R1", 2);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        Snapshot snapshot = Snapshot.capture(scenario);
        assertEquals("R1", snapshot.getProcessWaitingResource().get("P1"));
        assertEquals(2, snapshot.getProcessWaitingCount().get("P1"));

        p1.clearWaitingFor();
        p2.setWaitingFor("R2", 1);
        assertFalse(p1.isWaiting());
        assertTrue(p2.isWaiting());

        snapshot.restoreInto(scenario);

        assertTrue(p1.isWaiting());
        assertEquals("R1", p1.getWaitingResourceId());
        assertEquals(2, p1.getWaitingCount());

        assertFalse(p2.isWaiting());
        assertNull(p2.getWaitingResourceId());
        assertEquals(0, p2.getWaitingCount());
    }

    @Test
    void restoreIntoWithMismatchedScenarioThrowsIllegalArgumentException() {
        Scenario scenario1 = new Scenario("Scenario 1");
        scenario1.addResource(new Resource("R1", 3, false));
        scenario1.addProcess(new Process("P1", 1));
        Snapshot snapshot = Snapshot.capture(scenario1);

        Scenario mismatchedProcesses = new Scenario("Mismatched Processes");
        mismatchedProcesses.addResource(new Resource("R1", 3, false));
        mismatchedProcesses.addProcess(new Process("P2", 2));
        assertThrows(IllegalArgumentException.class, () -> snapshot.restoreInto(mismatchedProcesses));

        Scenario mismatchedResources = new Scenario("Mismatched Resources");
        mismatchedResources.addResource(new Resource("R2", 3, false));
        mismatchedResources.addProcess(new Process("P1", 1));
        assertThrows(IllegalArgumentException.class, () -> snapshot.restoreInto(mismatchedResources));
    }

    @Test
    void snapshotMapsCannotBeModifiedFromOutside() {
        Scenario scenario = new Scenario("Test Scenario");
        Resource r1 = new Resource("R1", 3, false);
        Process p1 = new Process("P1", 1);
        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setWaitingFor("R1", 1);
        scenario.addResource(r1);
        scenario.addProcess(p1);

        Snapshot snapshot = Snapshot.capture(scenario);

        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.getProcessStates().put("P1", ProcessState.COMPLETED));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.getProcessDeadlocked().put("P1", true));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.getAvailable().put("R1", 5));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.getProcessHeld().put("P1", Map.of()));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.getProcessHeld().get("P1").put("R1", 3));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.getProcessWaitingResource().put("P1", "R2"));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.getProcessWaitingCount().put("P1", 5));
    }

    @Test
    void packagePrivateRestorersValidateInput() {
        Resource r = new Resource("R1", 3, false);
        assertThrows(IllegalArgumentException.class, () -> r.restoreAvailable(-1));
        assertThrows(IllegalArgumentException.class, () -> r.restoreAvailable(4));

        Process p = new Process("P1", 1);
        assertThrows(IllegalArgumentException.class, () -> p.restoreHeld(null));
    }
}
