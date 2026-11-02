package com.deadlocksim.model.history;

import com.deadlocksim.model.EventType;
import com.deadlocksim.model.Scenario;
import com.deadlocksim.model.Snapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryTest {

    @Test
    void firstEventHasSequenceZeroAndLaterEventsCountUpByOne() {
        History history = new History();
        Snapshot s1 = Snapshot.capture(new Scenario("S1"));
        Snapshot s2 = Snapshot.capture(new Scenario("S2"));
        Snapshot s3 = Snapshot.capture(new Scenario("S3"));

        history.add(EventType.REQUEST, "Request 1", s1);
        history.add(EventType.GRANT, "Grant 1", s2);
        history.add(EventType.RELEASE, "Release 1", s3);

        assertEquals(3, history.size());
        assertEquals(0, history.get(0).getSequence());
        assertEquals(1, history.get(1).getSequence());
        assertEquals(2, history.get(2).getSequence());
    }

    @Test
    void eventsReturnsEventsInInsertionOrder() {
        History history = new History();
        Snapshot s1 = Snapshot.capture(new Scenario("S1"));
        Snapshot s2 = Snapshot.capture(new Scenario("S2"));

        history.add(EventType.REQUEST, "First event", s1);
        history.add(EventType.BLOCK, "Second event", s2);

        List<HistoryEvent> list = history.events();
        assertEquals(2, list.size());
        assertEquals("First event", list.get(0).getDescription());
        assertEquals(EventType.REQUEST, list.get(0).getType());
        assertSame(s1, list.get(0).getSnapshot());

        assertEquals("Second event", list.get(1).getDescription());
        assertEquals(EventType.BLOCK, list.get(1).getType());
        assertSame(s2, list.get(1).getSnapshot());
    }

    @Test
    void getWithUnknownSequenceThrowsIllegalArgumentException() {
        History history = new History();
        Snapshot s = Snapshot.capture(new Scenario("S"));
        history.add(EventType.REQUEST, "Event 0", s);

        assertThrows(IllegalArgumentException.class, () -> history.get(-1));
        assertThrows(IllegalArgumentException.class, () -> history.get(1));
        assertThrows(IllegalArgumentException.class, () -> history.get(99));
    }

    @Test
    void blankDescriptionThrowsIllegalArgumentException() {
        History history = new History();
        Snapshot s = Snapshot.capture(new Scenario("S"));

        assertThrows(IllegalArgumentException.class, () -> history.add(EventType.REQUEST, null, s));
        assertThrows(IllegalArgumentException.class, () -> history.add(EventType.REQUEST, "", s));
        assertThrows(IllegalArgumentException.class, () -> history.add(EventType.REQUEST, "   ", s));
        assertEquals(0, history.size());
    }

    @Test
    void nullSnapshotThrowsIllegalArgumentException() {
        History history = new History();

        assertThrows(IllegalArgumentException.class, () -> history.add(EventType.REQUEST, "Valid description", null));
        assertEquals(0, history.size());
    }

    @Test
    void nullEventTypeThrowsIllegalArgumentException() {
        History history = new History();
        Snapshot s = Snapshot.capture(new Scenario("S"));

        assertThrows(IllegalArgumentException.class, () -> history.add(null, "Valid description", s));
        assertEquals(0, history.size());
    }

    @Test
    void clearEmptiesTheListAndNextEventStartsAtSequenceZero() {
        History history = new History();
        Snapshot s = Snapshot.capture(new Scenario("S"));

        history.add(EventType.REQUEST, "Event 0", s);
        history.add(EventType.GRANT, "Event 1", s);
        assertEquals(2, history.size());

        history.clear();
        assertEquals(0, history.size());
        assertTrue(history.events().isEmpty());

        history.add(EventType.DETECT, "Restarted event", s);
        assertEquals(1, history.size());
        assertEquals(0, history.get(0).getSequence());
        assertEquals(EventType.DETECT, history.get(0).getType());
        assertEquals("Restarted event", history.get(0).getDescription());
    }

    @Test
    void eventsCannotBeModifiedByCaller() {
        History history = new History();
        Snapshot s = Snapshot.capture(new Scenario("S"));
        history.add(EventType.REQUEST, "Event 0", s);

        List<HistoryEvent> list = history.events();
        HistoryEvent newEvent = new HistoryEvent(1, EventType.GRANT, "Direct event", s);

        assertThrows(UnsupportedOperationException.class, () -> list.add(newEvent));
        assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
        assertThrows(UnsupportedOperationException.class, list::clear);
    }

    @Test
    void historyEventConstructorValidationsAndGetters() {
        Snapshot s = Snapshot.capture(new Scenario("S"));

        assertThrows(IllegalArgumentException.class, () -> new HistoryEvent(-1, EventType.REQUEST, "desc", s));
        assertThrows(IllegalArgumentException.class, () -> new HistoryEvent(0, null, "desc", s));
        assertThrows(IllegalArgumentException.class, () -> new HistoryEvent(0, EventType.REQUEST, null, s));
        assertThrows(IllegalArgumentException.class, () -> new HistoryEvent(0, EventType.REQUEST, "", s));
        assertThrows(IllegalArgumentException.class, () -> new HistoryEvent(0, EventType.REQUEST, "   ", s));
        assertThrows(IllegalArgumentException.class, () -> new HistoryEvent(0, EventType.REQUEST, "desc", null));

        HistoryEvent event = new HistoryEvent(5, EventType.TERMINATE, "Process terminated", s);
        assertEquals(5, event.getSequence());
        assertEquals(EventType.TERMINATE, event.getType());
        assertEquals("Process terminated", event.getDescription());
        assertSame(s, event.getSnapshot());
    }

    @Test
    void historyEventEqualsAndHashCode() {
        Snapshot s1 = Snapshot.capture(new Scenario("S1"));
        Snapshot s2 = Snapshot.capture(new Scenario("S2"));

        HistoryEvent e1 = new HistoryEvent(0, EventType.REQUEST, "desc", s1);
        HistoryEvent e1Same = new HistoryEvent(0, EventType.REQUEST, "desc", s1);
        HistoryEvent eDiffSeq = new HistoryEvent(1, EventType.REQUEST, "desc", s1);
        HistoryEvent eDiffType = new HistoryEvent(0, EventType.GRANT, "desc", s1);
        HistoryEvent eDiffDesc = new HistoryEvent(0, EventType.REQUEST, "other", s1);
        HistoryEvent eDiffSnap = new HistoryEvent(0, EventType.REQUEST, "desc", s2);

        assertEquals(e1, e1);
        assertEquals(e1, e1Same);
        assertEquals(e1.hashCode(), e1Same.hashCode());

        assertNotEquals(e1, eDiffSeq);
        assertNotEquals(e1, eDiffType);
        assertNotEquals(e1, eDiffDesc);
        assertNotEquals(e1, eDiffSnap);
        assertNotEquals(e1, null);
        assertNotEquals(e1, "not an event");
    }
}
