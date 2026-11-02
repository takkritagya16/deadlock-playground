package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationTest {

    @Test
    void validOperationStoresAllFourValues() {
        Operation operation = new Operation("P1", OperationType.REQUEST, "R1", 2);
        assertEquals("P1", operation.getProcessId());
        assertEquals(OperationType.REQUEST, operation.getType());
        assertEquals("R1", operation.getResourceId());
        assertEquals(2, operation.getCount());
    }

    @Test
    void invalidInputsThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Operation(null, OperationType.REQUEST, "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> new Operation("", OperationType.REQUEST, "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> new Operation("   ", OperationType.REQUEST, "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> new Operation("P1", null, "R1", 1));
        assertThrows(IllegalArgumentException.class, () -> new Operation("P1", OperationType.REQUEST, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Operation("P1", OperationType.REQUEST, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new Operation("P1", OperationType.REQUEST, "   ", 1));
        assertThrows(IllegalArgumentException.class, () -> new Operation("P1", OperationType.REQUEST, "R1", 0));
        assertThrows(IllegalArgumentException.class, () -> new Operation("P1", OperationType.REQUEST, "R1", -1));
    }

    @Test
    void identicalOperationsAreEqualAndHaveSameHashCode() {
        Operation op1 = new Operation("P1", OperationType.REQUEST, "R1", 2);
        Operation op2 = new Operation("P1", OperationType.REQUEST, "R1", 2);
        assertEquals(op1, op2);
        assertEquals(op1.hashCode(), op2.hashCode());
    }

    @Test
    void operationsWithDifferentCountsAreNotEqual() {
        Operation op1 = new Operation("P1", OperationType.REQUEST, "R1", 1);
        Operation op2 = new Operation("P1", OperationType.REQUEST, "R1", 2);
        assertNotEquals(op1, op2);
    }

    @Test
    void toStringContainsRequiredFields() {
        Operation op = new Operation("P1", OperationType.REQUEST, "R1", 2);
        String str = op.toString();
        assertTrue(str.contains("P1"));
        assertTrue(str.contains("REQUEST"));
        assertTrue(str.contains("R1"));
        assertTrue(str.contains("2"));
    }
}
