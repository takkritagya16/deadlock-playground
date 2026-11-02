package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTest {

    @Test
    void validResourceStartsAvailableEqualToTotal() {
        Resource resource = new Resource("R1", 5, false);
        assertEquals("R1", resource.getId());
        assertEquals("R1", resource.getName());
        assertEquals(5, resource.getTotal());
        assertEquals(5, resource.getAvailable());
        assertFalse(resource.isShareable());
        assertFalse(resource.getShareable());

        Resource shareableResource = new Resource("R2", 2, true);
        assertEquals("R2", shareableResource.getId());
        assertEquals("R2", shareableResource.getName());
        assertEquals(2, shareableResource.getTotal());
        assertEquals(2, shareableResource.getAvailable());
        assertTrue(shareableResource.isShareable());
        assertTrue(shareableResource.getShareable());
    }

    @Test
    void zeroTotalThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Resource("R1", 0, false));
    }

    @Test
    void negativeTotalThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Resource("R1", -1, false));
        assertThrows(IllegalArgumentException.class, () -> new Resource("R1", -5, true));
    }

    @Test
    void processStateEnumValuesMatchSpecification() {
        ProcessState[] states = ProcessState.values();
        assertEquals(4, states.length);
        List<String> names = Arrays.stream(states).map(Enum::name).toList();
        assertTrue(names.contains("RUNNING"));
        assertTrue(names.contains("WAITING"));
        assertTrue(names.contains("COMPLETED"));
        assertTrue(names.contains("TERMINATED"));
        assertFalse(names.contains("READY"));
    }

    @Test
    void eventTypeEnumValuesMatchSpecification() {
        EventType[] types = EventType.values();
        assertEquals(12, types.length);
        List<String> names = Arrays.stream(types).map(Enum::name).toList();
        List<String> expected = List.of(
                "REQUEST",
                "GRANT",
                "BLOCK",
                "RELEASE",
                "STATE_CHANGE",
                "AVOIDANCE_CHECK",
                "DETECT",
                "VICTIM_SELECTED",
                "TERMINATE",
                "PREEMPT",
                "ROLLBACK",
                "RESOLVED"
        );
        assertEquals(expected, names);
    }
}
