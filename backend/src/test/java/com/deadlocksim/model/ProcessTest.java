package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessTest {

    @Test
    void newProcessInitialState() {
        Process process = new Process("P1", 2);
        assertEquals("P1", process.getId());
        assertEquals(2, process.getPriority());
        assertEquals(ProcessState.RUNNING, process.getState());
        assertFalse(process.isDeadlocked());
        assertTrue(process.getHeld().isEmpty());
        assertEquals(0, process.getInstructionsExecuted());
        assertEquals(0, process.totalHeld());
        assertFalse(process.isBlocked());
        assertFalse(process.isWaiting());
        assertNull(process.getWaitingResourceId());
        assertEquals(0, process.getWaitingCount());
    }

    @Test
    void blankOrNullIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Process(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Process("", 1));
        assertThrows(IllegalArgumentException.class, () -> new Process("   ", 1));
    }

    @Test
    void setStateNullThrowsIllegalArgumentException() {
        Process process = new Process("P1", 1);
        assertThrows(IllegalArgumentException.class, () -> process.setState(null));
    }

    @Test
    void isBlockedTransitionsCorrectly() {
        Process process = new Process("P1", 1);
        assertFalse(process.isBlocked());

        process.setState(ProcessState.WAITING);
        assertTrue(process.isBlocked());

        process.setState(ProcessState.RUNNING);
        assertFalse(process.isBlocked());

        process.setState(ProcessState.COMPLETED);
        assertFalse(process.isBlocked());

        process.setState(ProcessState.TERMINATED);
        assertFalse(process.isBlocked());
    }

    @Test
    void setWaitingForStoresResourceAndCountAndIsWaitingReturnsTrue() {
        Process process = new Process("P1", 1);
        process.setWaitingFor("R1", 2);
        assertTrue(process.isWaiting());
        assertEquals("R1", process.getWaitingResourceId());
        assertEquals(2, process.getWaitingCount());
    }

    @Test
    void setWaitingForWithBlankResourceIdOrInvalidCountThrowsIllegalArgumentException() {
        Process process = new Process("P1", 1);
        assertThrows(IllegalArgumentException.class, () -> process.setWaitingFor(null, 1));
        assertThrows(IllegalArgumentException.class, () -> process.setWaitingFor("", 1));
        assertThrows(IllegalArgumentException.class, () -> process.setWaitingFor("   ", 1));
        assertThrows(IllegalArgumentException.class, () -> process.setWaitingFor("R1", 0));
        assertThrows(IllegalArgumentException.class, () -> process.setWaitingFor("R1", -1));
    }

    @Test
    void clearWaitingForResetsBothValues() {
        Process process = new Process("P1", 1);
        process.setWaitingFor("R1", 3);
        assertTrue(process.isWaiting());

        process.clearWaitingFor();
        assertFalse(process.isWaiting());
        assertNull(process.getWaitingResourceId());
        assertEquals(0, process.getWaitingCount());
    }

    @Test
    void holdInstancesAddsCountsAndRepeatedHoldsAccumulate() {
        Process process = new Process("P1", 1);
        process.holdInstances("R1", 2);
        assertEquals(1, process.getHeld().size());
        assertEquals(2, process.getHeld().get("R1"));

        process.holdInstances("R1", 3);
        assertEquals(5, process.getHeld().get("R1"));

        process.holdInstances("R2", 1);
        assertEquals(2, process.getHeld().size());
        assertEquals(1, process.getHeld().get("R2"));
    }

    @Test
    void holdInstancesWithZeroOrNegativeCountThrowsIllegalArgumentException() {
        Process process = new Process("P1", 1);
        assertThrows(IllegalArgumentException.class, () -> process.holdInstances("R1", 0));
        assertThrows(IllegalArgumentException.class, () -> process.holdInstances("R1", -1));
        assertTrue(process.getHeld().isEmpty());
    }

    @Test
    void releaseInstancesReducesCountAndRemovesEntryAtZero() {
        Process process = new Process("P1", 1);
        process.holdInstances("R1", 3);
        process.holdInstances("R2", 2);

        process.releaseInstances("R1", 1);
        assertEquals(2, process.getHeld().get("R1"));

        process.releaseInstances("R1", 2);
        assertFalse(process.getHeld().containsKey("R1"));
        assertEquals(1, process.getHeld().size());
        assertEquals(2, process.getHeld().get("R2"));
    }

    @Test
    void releasingMoreThanHeldThrowsIllegalStateExceptionAndChangesNothing() {
        Process process = new Process("P1", 1);
        process.holdInstances("R1", 2);

        assertThrows(IllegalStateException.class, () -> process.releaseInstances("R1", 3));
        assertEquals(1, process.getHeld().size());
        assertEquals(2, process.getHeld().get("R1"));

        assertThrows(IllegalStateException.class, () -> process.releaseInstances("R2", 1));
        assertEquals(1, process.getHeld().size());
        assertEquals(2, process.getHeld().get("R1"));
    }

    @Test
    void releasingZeroOrNegativeThrowsIllegalArgumentException() {
        Process process = new Process("P1", 1);
        process.holdInstances("R1", 2);

        assertThrows(IllegalArgumentException.class, () -> process.releaseInstances("R1", 0));
        assertThrows(IllegalArgumentException.class, () -> process.releaseInstances("R1", -1));
        assertEquals(2, process.getHeld().get("R1"));
    }

    @Test
    void getHeldCannotBeModifiedByCaller() {
        Process process = new Process("P1", 1);
        process.holdInstances("R1", 2);

        Map<String, Integer> held = process.getHeld();
        assertThrows(UnsupportedOperationException.class, () -> held.put("R1", 5));
        assertThrows(UnsupportedOperationException.class, () -> held.remove("R1"));
        assertEquals(2, process.getHeld().get("R1"));
    }

    @Test
    void totalHeldSumsAcrossResources() {
        Process process = new Process("P1", 1);
        assertEquals(0, process.totalHeld());

        process.holdInstances("R1", 3);
        assertEquals(3, process.totalHeld());

        process.holdInstances("R2", 2);
        assertEquals(5, process.totalHeld());

        process.releaseInstances("R1", 1);
        assertEquals(4, process.totalHeld());
    }

    @Test
    void recordInstructionIncrementsCounter() {
        Process process = new Process("P1", 1);
        assertEquals(0, process.getInstructionsExecuted());

        process.recordInstruction();
        assertEquals(1, process.getInstructionsExecuted());

        process.recordInstruction();
        assertEquals(2, process.getInstructionsExecuted());
    }

    @Test
    void setDeadlockedUpdatesStatus() {
        Process process = new Process("P1", 1);
        assertFalse(process.isDeadlocked());

        process.setDeadlocked(true);
        assertTrue(process.isDeadlocked());

        process.setDeadlocked(false);
        assertFalse(process.isDeadlocked());
    }

    @Test
    void rollbackStateResetsStateHoldingsDeadlockedWaitingForAndInstructionCount() {
        Process process = new Process("P1", 1);
        process.setState(ProcessState.WAITING);
        process.setDeadlocked(true);
        process.holdInstances("R1", 2);
        process.holdInstances("R2", 1);
        process.setWaitingFor("R3", 2);
        process.recordInstruction();
        process.recordInstruction();

        assertEquals(ProcessState.WAITING, process.getState());
        assertTrue(process.isDeadlocked());
        assertEquals(3, process.totalHeld());
        assertTrue(process.isWaiting());
        assertEquals("R3", process.getWaitingResourceId());
        assertEquals(2, process.getWaitingCount());
        assertEquals(2, process.getInstructionsExecuted());

        process.rollbackState();

        assertEquals(ProcessState.RUNNING, process.getState());
        assertFalse(process.isDeadlocked());
        assertTrue(process.getHeld().isEmpty());
        assertEquals(0, process.totalHeld());
        assertFalse(process.isWaiting());
        assertNull(process.getWaitingResourceId());
        assertEquals(0, process.getWaitingCount());
        assertEquals(0, process.getInstructionsExecuted());
    }

    @Test
    void rollbackStateDoesNotChangeAnyResource() {
        Resource resource = new Resource("R1", 3, false);
        resource.grant(2);
        assertEquals(1, resource.getAvailable());

        Process process = new Process("P1", 1);
        process.holdInstances("R1", 2);
        process.setState(ProcessState.WAITING);

        process.rollbackState();

        assertEquals(1, resource.getAvailable());
        assertEquals(3, resource.getTotal());
    }
}
