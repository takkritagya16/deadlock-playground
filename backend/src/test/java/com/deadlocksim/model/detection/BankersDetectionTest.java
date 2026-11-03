package com.deadlocksim.model.detection;

import com.deadlocksim.model.Graph;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankersDetectionTest {

    @Test
    void multiInstanceDeadlockWhereProcessesWaitOnEachOtherAndCannotFinishReturnsDeadlockedTrueWithProcessesListed() {
        Scenario scenario = new Scenario("Multi-instance Deadlock");
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

        BankersDetection detection = new BankersDetection();
        DetectionResult result = detection.detect(new Graph(scenario));

        assertTrue(result.isDeadlocked());
        assertEquals(List.of("P1", "P2"), result.getProcessIds());
        assertTrue(result.getProcessIds().contains("P1"));
        assertTrue(result.getProcessIds().contains("P2"));
        assertTrue(p1.isDeadlocked());
        assertTrue(p2.isDeadlocked());
    }

    @Test
    void requestCycleExistsButOneProcessCanFinishAndReleaseSoNoDeadlock() {
        Scenario scenario = new Scenario("Cycle With Non-deadlocked Releaser");
        Resource r1 = new Resource("R1", 2, false);
        Resource r2 = new Resource("R2", 2, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Process p3 = new Process("P3", 3);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 1);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 1);

        r1.grant(1);
        r2.grant(1);
        p3.holdInstances("R1", 1);
        p3.holdInstances("R2", 1);
        p3.setState(ProcessState.RUNNING);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addProcess(p3);

        BankersDetection detection = new BankersDetection();
        DetectionResult result = detection.detect(new Graph(scenario));

        assertFalse(result.isDeadlocked());
        assertEquals(DetectionResult.none(), result);
        assertTrue(result.getProcessIds().isEmpty());
    }

    @Test
    void stateWithNoWaitingProcessesReturnsDetectionResultNone() {
        Scenario scenario = new Scenario("No Waiting Processes");
        Resource r1 = new Resource("R1", 2, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.RUNNING);

        scenario.addResource(r1);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        BankersDetection detection = new BankersDetection();
        DetectionResult result = detection.detect(new Graph(scenario));

        assertFalse(result.isDeadlocked());
        assertEquals(DetectionResult.none(), result);
        assertTrue(result.getProcessIds().isEmpty());
    }

    @Test
    void deadlockedFlagsOnProcessesMatchResultAfterDetectRuns() {
        Scenario scenario = new Scenario("Flag Matching Test");
        Resource r1 = new Resource("R1", 2, false);
        Resource r2 = new Resource("R2", 2, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Process p3 = new Process("P3", 3);

        r1.grant(1);
        p1.holdInstances("R1", 1);
        p1.setState(ProcessState.WAITING);
        p1.setWaitingFor("R2", 2);

        r2.grant(1);
        p2.holdInstances("R2", 1);
        p2.setState(ProcessState.WAITING);
        p2.setWaitingFor("R1", 2);

        p3.setDeadlocked(true);

        scenario.addResource(r1);
        scenario.addResource(r2);
        scenario.addProcess(p1);
        scenario.addProcess(p2);
        scenario.addProcess(p3);

        BankersDetection detection = new BankersDetection();
        DetectionResult result = detection.detect(new Graph(scenario));

        assertTrue(result.isDeadlocked());
        assertTrue(p1.isDeadlocked());
        assertTrue(p2.isDeadlocked());
        assertFalse(p3.isDeadlocked());

        p1.clearWaitingFor();
        p1.setState(ProcessState.RUNNING);
        DetectionResult resolvedResult = detection.detect(new Graph(scenario));

        assertFalse(resolvedResult.isDeadlocked());
        assertFalse(p1.isDeadlocked());
        assertFalse(p2.isDeadlocked());
        assertFalse(p3.isDeadlocked());
    }

    @Test
    void nullGraphThrowsIllegalArgumentException() {
        BankersDetection detection = new BankersDetection();
        assertThrows(IllegalArgumentException.class, () -> detection.detect(null));
    }
}
