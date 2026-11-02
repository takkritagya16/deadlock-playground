package com.deadlocksim.model.detection;

import com.deadlocksim.model.Graph;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CycleDetectionTest {

    @Test
    void twoProcessDeadlockReturnsDeadlockedTrueProcessesP1AndP2AndNonEmptyCycle() {
        Scenario scenario = new Scenario("Two Process Deadlock");
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Process p3 = new Process("P3", 3);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setWaitingFor("R1", 1);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addProcess(p3);

        CycleDetection detection = new CycleDetection();
        DetectionResult result = detection.detect(new Graph(scenario));

        assertTrue(result.isDeadlocked());
        assertEquals(List.of("P1", "P2"), result.getProcessIds());
        assertFalse(result.getCycle().isEmpty());
        assertEquals(List.of("P1", "R2", "P2", "R1", "P1"), result.getCycle());
    }

    @Test
    void chainWithNoLoopReturnsNone() {
        Scenario scenario = new Scenario("Chain With No Loop");
        Resource r1 = new Resource("R1", 1, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);

        r1.grant(1);
        p2.holdInstances("R1", 1);
        p1.setWaitingFor("R1", 1);

        scenario.addResource(r1);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        CycleDetection detection = new CycleDetection();
        DetectionResult result = detection.detect(new Graph(scenario));

        assertEquals(DetectionResult.none(), result);
        assertFalse(result.isDeadlocked());
        assertTrue(result.getProcessIds().isEmpty());
        assertTrue(result.getCycle().isEmpty());
    }

    @Test
    void scenarioWithResourceTotalTwoOrMoreThrowsIllegalStateException() {
        Scenario scenario = new Scenario("Multiple Instances");
        Resource r1 = new Resource("R1", 2, false);
        Process p1 = new Process("P1", 1);

        scenario.addResource(r1);
        scenario.addProcess(p1);

        CycleDetection detection = new CycleDetection();
        assertThrows(IllegalStateException.class, () -> detection.detect(new Graph(scenario)));
    }

    @Test
    void deadlockedFlagsOnProcessesMatchResultAfterDetectRuns() {
        Scenario scenario = new Scenario("Flags Test");
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Process p3 = new Process("P3", 3);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setWaitingFor("R1", 1);

        p3.setDeadlocked(true);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addProcess(p3);

        CycleDetection detection = new CycleDetection();
        DetectionResult result = detection.detect(new Graph(scenario));

        assertTrue(result.isDeadlocked());
        assertTrue(p1.isDeadlocked());
        assertTrue(p2.isDeadlocked());
        assertFalse(p3.isDeadlocked());

        p1.clearWaitingFor();
        DetectionResult resolvedResult = detection.detect(new Graph(scenario));
        assertFalse(resolvedResult.isDeadlocked());
        assertFalse(p1.isDeadlocked());
        assertFalse(p2.isDeadlocked());
        assertFalse(p3.isDeadlocked());
    }

    @Test
    void processIdsAndCycleCannotBeModifiedByCaller() {
        DetectionResult result = DetectionResult.of(List.of("P1"), List.of("P1", "R1", "P1"));
        assertThrows(UnsupportedOperationException.class, () -> result.getProcessIds().add("P2"));
        assertThrows(UnsupportedOperationException.class, () -> result.getCycle().add("P2"));

        DetectionResult none = DetectionResult.none();
        assertThrows(UnsupportedOperationException.class, () -> none.getProcessIds().add("P1"));
        assertThrows(UnsupportedOperationException.class, () -> none.getCycle().add("P1"));
    }
}
