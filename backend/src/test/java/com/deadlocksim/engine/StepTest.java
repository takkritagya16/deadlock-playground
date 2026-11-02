package com.deadlocksim.engine;

import com.deadlocksim.model.Operation;
import com.deadlocksim.model.OperationType;
import com.deadlocksim.model.Process;
import com.deadlocksim.model.ProcessState;
import com.deadlocksim.model.Resource;
import com.deadlocksim.model.Scenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StepTest {

    @Test
    void stepBeforeConfigureThrowsIllegalStateException() {
        SimulationEngine engine = new SimulationEngine();
        assertThrows(IllegalStateException.class, engine::step);
    }

    @Test
    void steppingSimpleRequestReleaseScenarioRunsOperationsInOrder() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Step Simple");
        Process p1 = new Process("P1", 1);
        Resource r1 = new Resource("R1", 2, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);
        scenario.addOperation(new Operation("P1", OperationType.REQUEST, "R1", 1));
        scenario.addOperation(new Operation("P1", OperationType.RELEASE, "R1", 1));

        engine.configure(scenario);

        StepResult s1 = engine.step();
        assertFalse(s1.finished());
        assertEquals(1, p1.getNextOperationIndex());
        assertEquals(1, p1.totalHeld());
        assertEquals(1, r1.getAvailable());

        StepResult s2 = engine.step();
        assertTrue(s2.finished());
        assertEquals(2, p1.getNextOperationIndex());
        assertEquals(0, p1.totalHeld());
        assertEquals(2, r1.getAvailable());
        assertEquals(ProcessState.COMPLETED, p1.getState());
    }

    @Test
    void blockedRequestLeavesProcessWaitingAndCursorOnThatOperation() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Blocked Step");
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 2);
        Resource r1 = new Resource("R1", 1, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);
        scenario.addProcess(p2);

        scenario.addOperation(new Operation("P1", OperationType.REQUEST, "R1", 1));
        scenario.addOperation(new Operation("P2", OperationType.REQUEST, "R1", 1));

        engine.configure(scenario);

        // Step 1: P1 requests R1 (granted). Since P1 has no further operations, P1 completes.
        StepResult s1 = engine.step();
        assertFalse(s1.finished());
        assertEquals(1, p1.getNextOperationIndex());
        assertEquals(ProcessState.COMPLETED, p1.getState());

        // Step 2: P1 is COMPLETED, so P2 requests R1 (blocked because P1 holds R1)
        StepResult s2 = engine.step();
        assertFalse(s2.finished());
        assertEquals(0, p2.getNextOperationIndex()); // cursor not advanced
        assertEquals(ProcessState.WAITING, p2.getState());
        assertTrue(p2.isWaiting());
    }

    @Test
    void releaseThatWakesWaitingProcessAllowsNextStepToContinue() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Wake Test");
        Process pWait = new Process("P_Wait", 2);
        Process pHolder = new Process("P_Holder", 1);
        Resource r1 = new Resource("R1", 1, false);
        scenario.addProcess(pWait);
        scenario.addProcess(pHolder);
        scenario.addResource(r1);

        // P_Wait operations: request R1, then release R1
        scenario.addOperation(new Operation("P_Wait", OperationType.REQUEST, "R1", 1));
        scenario.addOperation(new Operation("P_Wait", OperationType.RELEASE, "R1", 1));

        // P_Holder operations: release R1
        scenario.addOperation(new Operation("P_Holder", OperationType.RELEASE, "R1", 1));

        engine.configure(scenario);

        // P_Holder already holds R1
        r1.grant(1);
        pHolder.holdInstances("R1", 1);

        // Step 1: P_Wait runs op 0 (request R1) -> BLOCKED
        StepResult s1 = engine.step();
        assertFalse(s1.finished());
        assertEquals(ProcessState.WAITING, pWait.getState());
        assertEquals(0, pWait.getNextOperationIndex());

        // Step 2: P_Wait is WAITING, so P_Holder runs op 0 (release R1)
        // This wakes P_Wait, which advances P_Wait's cursor to 1 and sets P_Wait to RUNNING
        StepResult s2 = engine.step();
        assertFalse(s2.finished());
        assertEquals(ProcessState.RUNNING, pWait.getState());
        assertEquals(1, pWait.getNextOperationIndex());
        assertEquals(ProcessState.COMPLETED, pHolder.getState());

        // Step 3: P_Wait is now RUNNING and continues with its next operation (release R1)
        StepResult s3 = engine.step();
        assertTrue(s3.finished());
        assertEquals(2, pWait.getNextOperationIndex());
        assertEquals(ProcessState.COMPLETED, pWait.getState());
        assertEquals(1, r1.getAvailable());
    }

    @Test
    void whenAllOperationsAreDoneStepReturnsFinishedTrue() {
        SimulationEngine engine = new SimulationEngine();
        Scenario scenario = new Scenario("Finished Test");
        Process p1 = new Process("P1", 1);
        Resource r1 = new Resource("R1", 1, false);
        scenario.addProcess(p1);
        scenario.addResource(r1);
        scenario.addOperation(new Operation("P1", OperationType.REQUEST, "R1", 1));

        engine.configure(scenario);

        StepResult s1 = engine.step();
        assertTrue(s1.finished());
        assertEquals(ProcessState.COMPLETED, p1.getState());

        StepResult s2 = engine.step();
        assertTrue(s2.finished());
        assertEquals("Scenario finished", s2.description());
    }
}
