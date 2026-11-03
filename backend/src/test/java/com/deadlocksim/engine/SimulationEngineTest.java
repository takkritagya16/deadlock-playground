package com.deadlocksim.engine;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.Snapshot;
import com.deadlocksim.model.history.HistoryEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    @Test
    void configureNullThrowsIllegalArgumentException() {
        SimulationEngine engine = new SimulationEngine();
        assertThrows(IllegalArgumentException.class, () -> engine.configure(null));
        assertNull(engine.getScenario());
        assertTrue(engine.getHistoryEvents().isEmpty());
    }

    @Test
    void configureWithInvalidScenarioThrowsIllegalArgumentExceptionAndLeavesPreviousStateUnchanged() {
        SimulationEngine engine = new SimulationEngine();

        Scenario validScenario = new Scenario("Valid");
        validScenario.addProcess(new Process("P1", 1));
        validScenario.addResource(new Resource("R1", 1, false));

        engine.configure(validScenario);
        assertSame(validScenario, engine.getScenario());
        assertEquals(1, engine.getHistoryEvents().size());
        assertEquals("Scenario loaded", engine.getHistoryEvents().get(0).getDescription());

        Scenario invalidScenario = new Scenario("Invalid");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> engine.configure(invalidScenario));
        assertTrue(ex.getMessage().contains("Scenario must contain at least one process"));
        assertTrue(ex.getMessage().contains("Scenario must contain at least one resource"));

        assertSame(validScenario, engine.getScenario());
        assertEquals(1, engine.getHistoryEvents().size());
        assertEquals("Scenario loaded", engine.getHistoryEvents().get(0).getDescription());
    }

    @Test
    void configureWithValidScenarioAddsExactlyOneHistoryEventOfTypeStateChange() {
        SimulationEngine engine = new SimulationEngine();

        Scenario scenario = new Scenario("Valid");
        scenario.addProcess(new Process("P1", 1));
        scenario.addResource(new Resource("R1", 2, false));

        engine.configure(scenario);

        assertSame(scenario, engine.getScenario());
        List<HistoryEvent> events = engine.getHistoryEvents();
        assertEquals(1, events.size());

        HistoryEvent event = events.get(0);
        assertEquals(EventType.STATE_CHANGE, event.getType());
        assertEquals("Scenario loaded", event.getDescription());
        assertEquals(0, event.getSequence());
        assertNotNull(event.getSnapshot());
    }

    @Test
    void resetBeforeConfigureThrowsIllegalStateException() {
        SimulationEngine engine = new SimulationEngine();
        assertThrows(IllegalStateException.class, () -> engine.reset(false));
        assertThrows(IllegalStateException.class, () -> engine.reset(true));
    }

    @Test
    void resetWithoutConfirmationWhenHistoryHasMoreThanOneEventThrowsIllegalStateExceptionAndChangesNothing() {
        SimulationEngine engine = new SimulationEngine();

        Scenario scenario = new Scenario("Test");
        Process p1 = new Process("P1", 1);
        Resource r1 = new Resource("R1", 3, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);

        engine.configure(scenario);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        p1.setState(ProcessState.WAITING);
        p1.advanceOperation();

        engine.history.add(EventType.REQUEST, "P1 requested R1", Snapshot.capture(scenario));
        assertEquals(2, engine.getHistoryEvents().size());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> engine.reset(false));
        assertEquals("Confirmation required to reset", ex.getMessage());

        assertEquals(2, engine.getHistoryEvents().size());
        assertEquals(ProcessState.WAITING, p1.getState());
        assertEquals(2, p1.totalHeld());
        assertEquals(1, r1.getAvailable());
        assertEquals(1, p1.getNextOperationIndex());
    }

    @Test
    void resetWithConfirmationRestoresInitialStateAndLeavesExactlyOneEventWithScenarioResetDescription() {
        SimulationEngine engine = new SimulationEngine();

        Scenario scenario = new Scenario("Test");
        Process p1 = new Process("P1", 1);
        Resource r1 = new Resource("R1", 3, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);

        engine.configure(scenario);

        r1.grant(2);
        p1.holdInstances("R1", 2);
        p1.setState(ProcessState.WAITING);
        p1.advanceOperation();
        p1.advanceOperation();

        engine.history.add(EventType.GRANT, "Granted 2 of R1 to P1", Snapshot.capture(scenario));
        assertEquals(2, engine.getHistoryEvents().size());

        engine.reset(true);

        assertEquals(ProcessState.RUNNING, p1.getState());
        assertEquals(0, p1.totalHeld());
        assertTrue(p1.getHeld().isEmpty());
        assertFalse(p1.isWaiting());
        assertFalse(p1.isDeadlocked());
        assertEquals(3, r1.getAvailable());
        assertEquals(0, p1.getNextOperationIndex());

        List<HistoryEvent> events = engine.getHistoryEvents();
        assertEquals(1, events.size());
        HistoryEvent event = events.get(0);
        assertEquals(EventType.STATE_CHANGE, event.getType());
        assertEquals("Scenario reset", event.getDescription());
        assertEquals(0, event.getSequence());
    }

    @Test
    void getHistoryEventsCannotBeModifiedByCaller() {
        SimulationEngine engine = new SimulationEngine();

        Scenario scenario = new Scenario("Test");
        scenario.addProcess(new Process("P1", 1));
        scenario.addResource(new Resource("R1", 1, false));

        engine.configure(scenario);

        List<HistoryEvent> events = engine.getHistoryEvents();
        HistoryEvent newEvent = new HistoryEvent(1, EventType.GRANT, "Other", Snapshot.capture(scenario));

        assertThrows(UnsupportedOperationException.class, () -> events.add(newEvent));
        assertThrows(UnsupportedOperationException.class, () -> events.remove(0));
        assertThrows(UnsupportedOperationException.class, events::clear);
    }

    @Test
    void viewedSeqTracksNewestSequenceAndJumps() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Test");
        scenario.addProcess(new Process("P1", 1));
        scenario.addResource(new Resource("R1", 2, false));

        engine.configure(scenario);
        assertEquals(0, engine.getViewedSeq());

        engine.request("P1", "R1", 1);
        engine.release("P1", "R1", 1);
        int newestSeq = engine.getHistoryEvents().get(engine.getHistoryEvents().size() - 1).getSequence();
        assertEquals(newestSeq, engine.getViewedSeq());

        engine.jumpTo(0);
        assertEquals(0, engine.getViewedSeq());

        engine.request("P1", "R1", 1);
        int afterJumpSeq = engine.getHistoryEvents().get(engine.getHistoryEvents().size() - 1).getSequence();
        assertEquals(afterJumpSeq, engine.getViewedSeq());
    }

    @Test
    void jumpBackThenRequestDiscardsFutureEventsAndFollowsJumpPoint() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Test");
        scenario.addProcess(new Process("P1", 1));
        scenario.addResource(new Resource("R1", 2, false));

        engine.configure(scenario);
        engine.request("P1", "R1", 1);
        engine.release("P1", "R1", 1);
        int totalBefore = engine.getHistoryEvents().size();

        engine.jumpTo(0);
        assertEquals(totalBefore, engine.getHistoryEvents().size());
        assertEquals(0, engine.getViewedSeq());

        engine.request("P1", "R1", 1);
        List<HistoryEvent> events = engine.getHistoryEvents();
        assertEquals(3, events.size());
        assertEquals(0, events.get(0).getSequence());
        assertEquals("Scenario loaded", events.get(0).getDescription());
        assertEquals(1, events.get(1).getSequence());
        assertEquals(2, events.get(2).getSequence());
        assertEquals(2, engine.getViewedSeq());
        assertTrue(events.stream().noneMatch(e -> e.getDescription().contains("released")));
    }

    @Test
    void jumpAloneNeverTruncates() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Test");
        scenario.addProcess(new Process("P1", 1));
        scenario.addResource(new Resource("R1", 2, false));

        engine.configure(scenario);
        engine.request("P1", "R1", 1);
        engine.release("P1", "R1", 1);
        int totalBefore = engine.getHistoryEvents().size();

        engine.jumpTo(0);
        assertEquals(totalBefore, engine.getHistoryEvents().size());
        assertEquals(0, engine.getViewedSeq());

        engine.jumpTo(1);
        assertEquals(totalBefore, engine.getHistoryEvents().size());
        assertEquals(1, engine.getViewedSeq());
    }

    @Test
    void actionWithNoJumpDoesNotTruncate() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Test");
        scenario.addProcess(new Process("P1", 1));
        scenario.addResource(new Resource("R1", 2, false));

        engine.configure(scenario);
        engine.request("P1", "R1", 1);
        assertEquals(3, engine.getHistoryEvents().size());

        engine.release("P1", "R1", 1);
        assertEquals(4, engine.getHistoryEvents().size());
        assertEquals(0, engine.getHistoryEvents().get(0).getSequence());
        assertEquals(1, engine.getHistoryEvents().get(1).getSequence());
        assertEquals(2, engine.getHistoryEvents().get(2).getSequence());
        assertEquals(3, engine.getHistoryEvents().get(3).getSequence());
        assertEquals(3, engine.getViewedSeq());
    }
}
