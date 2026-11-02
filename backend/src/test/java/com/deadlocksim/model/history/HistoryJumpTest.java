package com.deadlocksim.model.history;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.Snapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryJumpTest {

    @Test
    void jumpToReturnsTheSnapshotStoredAtThatEvent() {
        History history = new History();
        Snapshot s0 = Snapshot.capture(new Scenario("S0"));
        Snapshot s1 = Snapshot.capture(new Scenario("S1"));
        Snapshot s2 = Snapshot.capture(new Scenario("S2"));

        history.add(EventType.REQUEST, "Event 0", s0);
        history.add(EventType.GRANT, "Event 1", s1);
        history.add(EventType.RELEASE, "Event 2", s2);

        assertSame(s0, history.jumpTo(0));
        assertSame(s1, history.jumpTo(1));
        assertSame(s2, history.jumpTo(2));
    }

    @Test
    void jumpToOnEarlierEventLeavesLaterEventsInPlace() {
        History history = new History();
        Snapshot s0 = Snapshot.capture(new Scenario("S0"));
        Snapshot s1 = Snapshot.capture(new Scenario("S1"));
        Snapshot s2 = Snapshot.capture(new Scenario("S2"));

        history.add(EventType.REQUEST, "Event 0", s0);
        history.add(EventType.GRANT, "Event 1", s1);
        history.add(EventType.RELEASE, "Event 2", s2);

        Snapshot jumped = history.jumpTo(0);
        assertSame(s0, jumped);

        assertEquals(3, history.size());
        assertEquals(3, history.events().size());
        assertEquals("Event 0", history.get(0).getDescription());
        assertEquals("Event 1", history.get(1).getDescription());
        assertEquals("Event 2", history.get(2).getDescription());
        assertSame(s2, history.get(2).getSnapshot());
    }

    @Test
    void sizeAndEventsAreUnchangedAfterAJump() {
        History history = new History();
        Snapshot s0 = Snapshot.capture(new Scenario("S0"));
        Snapshot s1 = Snapshot.capture(new Scenario("S1"));

        history.add(EventType.REQUEST, "Event 0", s0);
        history.add(EventType.GRANT, "Event 1", s1);

        int initialSize = history.size();
        List<HistoryEvent> initialEvents = new ArrayList<>(history.events());

        history.jumpTo(0);

        assertEquals(initialSize, history.size());
        assertEquals(initialEvents, history.events());

        history.jumpTo(1);

        assertEquals(initialSize, history.size());
        assertEquals(initialEvents, history.events());
    }

    @Test
    void jumpToWithUnknownSequenceThrowsIllegalArgumentException() {
        History history = new History();
        Snapshot s0 = Snapshot.capture(new Scenario("S0"));

        history.add(EventType.REQUEST, "Event 0", s0);

        assertThrows(IllegalArgumentException.class, () -> history.jumpTo(-1));
        assertThrows(IllegalArgumentException.class, () -> history.jumpTo(1));
        assertThrows(IllegalArgumentException.class, () -> history.jumpTo(99));

        History emptyHistory = new History();
        assertThrows(IllegalArgumentException.class, () -> emptyHistory.jumpTo(0));
    }

    @Test
    void snapshotReturnedByJumpToWhenRestoredIntoScenarioMatchesStateAtThatEvent() {
        Scenario scenario = new Scenario("Jump Restore Test");
        Process p1 = new Process("P1", 1);
        Resource r1 = new Resource("R1", 3, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);

        History history = new History();

        // Event 0: initial state
        Snapshot s0 = Snapshot.capture(scenario);
        history.add(EventType.STATE_CHANGE, "Initial state", s0);

        // Event 1: P1 holds 2 of R1
        r1.grant(2);
        p1.holdInstances("R1", 2);
        Snapshot s1 = Snapshot.capture(scenario);
        history.add(EventType.GRANT, "P1 granted 2 of R1", s1);

        // Event 2: P1 blocked waiting for 2 of R1
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R1", 2);
        Snapshot s2 = Snapshot.capture(scenario);
        history.add(EventType.BLOCK, "P1 blocked on R1", s2);

        // Verify current scenario is at state 2
        assertEquals(ProcessState.WAITING, p1.getState());
        assertTrue(p1.isWaiting());
        assertEquals(1, r1.getAvailable());
        assertEquals(2, p1.totalHeld());

        // Jump to Event 1 and restore into scenario
        Snapshot jumpSnap1 = history.jumpTo(1);
        jumpSnap1.restoreInto(scenario);

        assertEquals(ProcessState.RUNNING, p1.getState());
        assertFalse(p1.isWaiting());
        assertNull(p1.getWaitingResourceId());
        assertEquals(0, p1.getWaitingCount());
        assertEquals(2, p1.totalHeld());
        assertEquals(1, r1.getAvailable());

        // Jump to Event 0 and restore into scenario
        Snapshot jumpSnap0 = history.jumpTo(0);
        jumpSnap0.restoreInto(scenario);

        assertEquals(ProcessState.RUNNING, p1.getState());
        assertFalse(p1.isWaiting());
        assertEquals(0, p1.totalHeld());
        assertTrue(p1.getHeld().isEmpty());
        assertEquals(3, r1.getAvailable());
    }
}
