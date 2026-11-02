package com.deadlocksim.model.victim;

import com.deadlocksim.model.Process;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriorityPolicyTest {

    @Test
    void lowestPriorityProcessIsChosenFromThreeProcesses() {
        PriorityPolicy policy = new PriorityPolicy();
        Process p1 = new Process("P1", 5);
        Process p2 = new Process("P2", 2);
        Process p3 = new Process("P3", 4);

        Process victim = policy.selectVictim(List.of(p1, p2, p3));
        assertSame(p2, victim);
    }

    @Test
    void tieOnPriorityChoosesLowerProcessId() {
        PriorityPolicy policy = new PriorityPolicy();
        Process p1 = new Process("P1", 3);
        Process p2 = new Process("P2", 3);

        Process victim1 = policy.selectVictim(List.of(p2, p1));
        assertSame(p1, victim1);

        Process victim2 = policy.selectVictim(List.of(p1, p2));
        assertSame(p1, victim2);
    }

    @Test
    void emptyListThrowsIllegalArgumentException() {
        PriorityPolicy policy = new PriorityPolicy();
        assertThrows(IllegalArgumentException.class, () -> policy.selectVictim(List.of()));
    }

    @Test
    void nullListThrowsIllegalArgumentException() {
        PriorityPolicy policy = new PriorityPolicy();
        assertThrows(IllegalArgumentException.class, () -> policy.selectVictim(null));
    }

    @Test
    void inputListIsUnchangedAfterCall() {
        PriorityPolicy policy = new PriorityPolicy();
        Process p1 = new Process("P1", 2);
        Process p2 = new Process("P2", 1);
        Process p3 = new Process("P3", 3);

        List<Process> list = new ArrayList<>(List.of(p1, p2, p3));
        policy.selectVictim(list);

        assertEquals(3, list.size());
        assertSame(p1, list.get(0));
        assertSame(p2, list.get(1));
        assertSame(p3, list.get(2));
    }
}
