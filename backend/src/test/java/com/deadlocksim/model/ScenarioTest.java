package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioTest {

    @Test
    void blankOrNullNameThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Scenario(null));
        assertThrows(IllegalArgumentException.class, () -> new Scenario(""));
        assertThrows(IllegalArgumentException.class, () -> new Scenario("   "));
    }

    @Test
    void duplicateResourceIdThrowsIllegalArgumentException() {
        Scenario scenario = new Scenario("Test Scenario");
        scenario.addResource(new Resource("R1", 2, false));
        assertThrows(IllegalArgumentException.class, () -> scenario.addResource(new Resource("R1", 1, true)));
    }

    @Test
    void duplicateProcessIdThrowsIllegalArgumentException() {
        Scenario scenario = new Scenario("Test Scenario");
        scenario.addProcess(new Process("P1", 1));
        assertThrows(IllegalArgumentException.class, () -> scenario.addProcess(new Process("P1", 2)));
    }

    @Test
    void operationForUnknownProcessThrowsIllegalArgumentException() {
        Scenario scenario = new Scenario("Test Scenario");
        scenario.addResource(new Resource("R1", 2, false));
        scenario.addProcess(new Process("P1", 1));

        Operation op = new Operation("P2", OperationType.REQUEST, "R1", 1);
        assertThrows(IllegalArgumentException.class, () -> scenario.addOperation(op));
    }

    @Test
    void operationForUnknownResourceThrowsIllegalArgumentException() {
        Scenario scenario = new Scenario("Test Scenario");
        scenario.addResource(new Resource("R1", 2, false));
        scenario.addProcess(new Process("P1", 1));

        Operation op = new Operation("P1", OperationType.REQUEST, "R2", 1);
        assertThrows(IllegalArgumentException.class, () -> scenario.addOperation(op));
    }

    @Test
    void requestLargerThanResourceTotalThrowsIllegalArgumentException() {
        Scenario scenario = new Scenario("Test Scenario");
        scenario.addResource(new Resource("R1", 2, false));
        scenario.addProcess(new Process("P1", 1));

        Operation op = new Operation("P1", OperationType.REQUEST, "R1", 3);
        assertThrows(IllegalArgumentException.class, () -> scenario.addOperation(op));
    }

    @Test
    void operationsForOneProcessReturnedInOrderAdded() {
        Scenario scenario = new Scenario("Test Scenario");
        scenario.addResource(new Resource("R1", 3, false));
        scenario.addResource(new Resource("R2", 2, false));
        scenario.addProcess(new Process("P1", 1));

        Operation op1 = new Operation("P1", OperationType.REQUEST, "R1", 1);
        Operation op2 = new Operation("P1", OperationType.REQUEST, "R2", 1);
        Operation op3 = new Operation("P1", OperationType.RELEASE, "R1", 1);

        scenario.addOperation(op1);
        scenario.addOperation(op2);
        scenario.addOperation(op3);

        List<Operation> p1Ops = scenario.getOperations("P1");
        assertEquals(3, p1Ops.size());
        assertEquals(op1, p1Ops.get(0));
        assertEquals(op2, p1Ops.get(1));
        assertEquals(op3, p1Ops.get(2));
    }

    @Test
    void validateReturnsEmptyListForValidScenario() {
        Scenario scenario = new Scenario("Valid Scenario");
        scenario.addResource(new Resource("R1", 1, false));
        scenario.addProcess(new Process("P1", 1));

        List<String> errors = scenario.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateReportsErrorForScenarioWithNoProcesses() {
        Scenario scenario = new Scenario("No Process Scenario");
        scenario.addResource(new Resource("R1", 1, false));

        List<String> errors = scenario.validate();
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("process")));
    }

    @Test
    void returnedListsCannotBeModifiedByCaller() {
        Scenario scenario = new Scenario("Immutable Views");
        scenario.addResource(new Resource("R1", 2, false));
        scenario.addProcess(new Process("P1", 1));
        scenario.addOperation(new Operation("P1", OperationType.REQUEST, "R1", 1));

        assertThrows(UnsupportedOperationException.class, () ->
                scenario.getResources().add(new Resource("R2", 1, false)));
        assertThrows(UnsupportedOperationException.class, () ->
                scenario.getProcesses().add(new Process("P2", 2)));
        assertThrows(UnsupportedOperationException.class, () ->
                scenario.getOperations("P1").add(new Operation("P1", OperationType.RELEASE, "R1", 1)));
    }
}
