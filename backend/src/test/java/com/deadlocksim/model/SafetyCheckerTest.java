package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyCheckerTest {

    @Test
    void safeGrantReturnsSafeWithSequenceContainingEveryProcess() {
        Scenario scenario = new Scenario("Safe Grant Scenario");
        Resource r1 = new Resource("R1", 3, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        r1.grant(1);
        p2.holdInstances("R1", 1);

        scenario.addResource(r1);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        SafetyResult result = SafetyChecker.check(scenario, "P1", "R1", 1);

        assertTrue(result.isSafe());
        assertTrue(result.safe());
        assertNotNull(result.getReason());
        assertNotNull(result.reason());
        assertEquals(2, result.getSequence().size());
        assertEquals(result.getSequence(), result.sequence());
        assertTrue(result.getSequence().contains("P1"));
        assertTrue(result.getSequence().contains("P2"));
    }

    @Test
    void grantThatLeavesNoSafeOrderReturnsUnsafeWithReasonNamingProcessAndResource() {
        Scenario scenario = new Scenario("Unsafe Grant Scenario");
        Resource r1 = new Resource("R1", 2, false);
        Resource r2 = new Resource("R2", 2, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 2);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 2);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        SafetyResult result = SafetyChecker.check(scenario, "P1", "R1", 1);

        assertFalse(result.isSafe());
        assertFalse(result.safe());
        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("P1") || result.getReason().contains("P2"));
        assertTrue(result.getReason().contains("R1") || result.getReason().contains("R2"));
    }

    @Test
    void checkNeverChangesScenarioAvailableCountsOrProcessStatesOrWaitingForValues() {
        Scenario scenario = new Scenario("Immutability Test");
        Resource r1 = new Resource("R1", 2, false);
        Resource r2 = new Resource("R2", 2, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.RUNNING);
        p1.setWaitingFor("R2", 2);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 2);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        int r1AvailableBefore = r1.getAvailable();
        int r2AvailableBefore = r2.getAvailable();
        ProcessState p1StateBefore = p1.getState();
        ProcessState p2StateBefore = p2.getState();
        String p1WaitingResBefore = p1.getWaitingResourceId();
        int p1WaitingCountBefore = p1.getWaitingCount();
        String p2WaitingResBefore = p2.getWaitingResourceId();
        int p2WaitingCountBefore = p2.getWaitingCount();
        int p1HeldBefore = p1.totalHeld();
        int p2HeldBefore = p2.totalHeld();

        SafetyChecker.check(scenario, "P1", "R1", 1);

        assertEquals(r1AvailableBefore, r1.getAvailable());
        assertEquals(r2AvailableBefore, r2.getAvailable());
        assertEquals(p1StateBefore, p1.getState());
        assertEquals(p2StateBefore, p2.getState());
        assertEquals(p1WaitingResBefore, p1.getWaitingResourceId());
        assertEquals(p1WaitingCountBefore, p1.getWaitingCount());
        assertEquals(p2WaitingResBefore, p2.getWaitingResourceId());
        assertEquals(p2WaitingCountBefore, p2.getWaitingCount());
        assertEquals(p1HeldBefore, p1.totalHeld());
        assertEquals(p2HeldBefore, p2.totalHeld());

        // Also test check on a safe request does not mutate
        Scenario safeScenario = new Scenario("Safe Immutability Test");
        Resource safeR = new Resource("R1", 2, false);
        Process safeP = new Process("P1", 1);
        safeScenario.addResource(safeR);
        safeScenario.addProcess(safeP);

        SafetyChecker.check(safeScenario, "P1", "R1", 1);
        assertEquals(2, safeR.getAvailable());
        assertEquals(0, safeP.totalHeld());
        assertEquals(ProcessState.RUNNING, safeP.getState());
    }

    @Test
    void safetyResultUnsafeRejectsBlankReasonWithIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SafetyResult.unsafe("", Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> SafetyResult.unsafe("   ", Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> SafetyResult.unsafe(null, Collections.emptyList()));

        assertThrows(IllegalArgumentException.class, () -> SafetyResult.safe("", Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> SafetyResult.safe("   ", Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> SafetyResult.safe(null, Collections.emptyList()));
    }

    @Test
    void checkThrowsOnInvalidArguments() {
        Scenario scenario = new Scenario("Argument Test");
        Resource r1 = new Resource("R1", 2, false);
        Process p1 = new Process("P1", 1);
        scenario.addResource(r1);
        scenario.addProcess(p1);

        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(null, "P1", "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, null, "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, "  ", "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, "P1", null, 1));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, "P1", "  ", 1));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, "P1", "R1", 0));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, "P1", "R1", -1));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, "P99", "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> SafetyChecker.check(scenario, "P1", "R99", 1));
    }

    @Test
    void insufficientAvailableInstancesReturnsUnsafe() {
        Scenario scenario = new Scenario("Insufficient Test");
        Resource r1 = new Resource("R1", 1, false);
        Process p1 = new Process("P1", 1);
        r1.grant(1);
        scenario.addResource(r1);
        scenario.addProcess(p1);

        SafetyResult result = SafetyChecker.check(scenario, "P1", "R1", 1);
        assertFalse(result.isSafe());
        assertTrue(result.getReason().contains("Insufficient available"));
    }

    @Test
    void safetyResultEqualsAndHashCode() {
        SafetyResult r1 = SafetyResult.safe("Safe", List.of("P1"));
        SafetyResult r2 = SafetyResult.safe("Safe", List.of("P1"));
        SafetyResult r3 = SafetyResult.unsafe("Unsafe", List.of("P1"));

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
        assertNotEquals(r1, null);
        assertNotEquals(r1, new Object());
    }
}
