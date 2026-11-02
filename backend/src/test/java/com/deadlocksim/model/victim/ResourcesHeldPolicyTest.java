package com.deadlocksim.model.victim;

import com.deadlocksim.model.Process;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourcesHeldPolicyTest {

    @Test
    void choosesProcessHoldingFewestInstances() {
        ResourcesHeldPolicy policy = new ResourcesHeldPolicy();
        Process p1 = new Process("P1", 1);
        p1.holdInstances("R1", 3);

        Process p2 = new Process("P2", 1);
        p2.holdInstances("R1", 1);

        Process p3 = new Process("P3", 1);
        p3.holdInstances("R1", 4);

        Process victim = policy.selectVictim(List.of(p1, p2, p3));
        assertSame(p2, victim);
    }

    @Test
    void breaksTieByLowerId() {
        ResourcesHeldPolicy policy = new ResourcesHeldPolicy();
        Process p1 = new Process("P1", 1);
        p1.holdInstances("R1", 2);

        Process p2 = new Process("P2", 1);
        p2.holdInstances("R1", 2);

        Process victim1 = policy.selectVictim(List.of(p2, p1));
        assertSame(p1, victim1);

        Process victim2 = policy.selectVictim(List.of(p1, p2));
        assertSame(p1, victim2);
    }

    @Test
    void nullAndEmptyListThrowsIllegalArgumentException() {
        ResourcesHeldPolicy policy = new ResourcesHeldPolicy();
        assertThrows(IllegalArgumentException.class, () -> policy.selectVictim(null));
        assertThrows(IllegalArgumentException.class, () -> policy.selectVictim(List.of()));
    }

    @Test
    void inputListIsUnchangedAfterCall() {
        ResourcesHeldPolicy policy = new ResourcesHeldPolicy();
        Process p1 = new Process("P1", 1);
        p1.holdInstances("R1", 2);

        Process p2 = new Process("P2", 1);
        p2.holdInstances("R1", 1);

        List<Process> list = new ArrayList<>(List.of(p1, p2));
        policy.selectVictim(list);

        assertEquals(2, list.size());
        assertSame(p1, list.get(0));
        assertSame(p2, list.get(1));
    }
}
