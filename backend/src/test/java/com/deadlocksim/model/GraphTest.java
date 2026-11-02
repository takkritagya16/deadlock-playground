package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTest {

    @Test
    void emptyScenarioGivesNoNodesAndNoEdges() {
        Scenario scenario = new Scenario("Empty");
        Graph graph = new Graph(scenario);
        assertTrue(graph.nodes().isEmpty());
        assertTrue(graph.edges().isEmpty());
        assertTrue(graph.findCycles().isEmpty());
    }

    @Test
    void waitingProcessProducesOneRequestEdgeWithRightResourceAndCount() {
        Scenario scenario = new Scenario("Waiting Process");
        Resource r1 = new Resource("R1", 2, false);
        Process p1 = new Process("P1", 1);
        p1.setWaitingFor("R1", 2);

        scenario.addResource(r1);
        scenario.addProcess(p1);

        Graph graph = new Graph(scenario);
        List<Graph.Edge> edges = graph.edges();
        assertEquals(1, edges.size());

        Graph.Edge edge = edges.get(0);
        assertEquals("P1", edge.getFrom());
        assertEquals("R1", edge.getTo());
        assertEquals(Graph.EdgeKind.REQUEST, edge.getKind());
        assertEquals(2, edge.getCount());
    }

    @Test
    void heldResourceProducesOneAllocationEdgeFromResourceToProcess() {
        Scenario scenario = new Scenario("Held Resource");
        Resource r1 = new Resource("R1", 3, false);
        Process p1 = new Process("P1", 1);
        r1.grant(2);
        p1.holdInstances("R1", 2);

        scenario.addResource(r1);
        scenario.addProcess(p1);

        Graph graph = new Graph(scenario);
        List<Graph.Edge> edges = graph.edges();
        assertEquals(1, edges.size());

        Graph.Edge edge = edges.get(0);
        assertEquals("R1", edge.getFrom());
        assertEquals("P1", edge.getTo());
        assertEquals(Graph.EdgeKind.ALLOCATION, edge.getKind());
        assertEquals(2, edge.getCount());
    }

    @Test
    void classicTwoProcessDeadlockProducesExactlyOneCycle() {
        Scenario scenario = new Scenario("Classic Deadlock");
        Resource r1 = new Resource("R1", 1, false);
        Resource r2 = new Resource("R2", 1, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 1);

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

        Graph graph = new Graph(scenario);
        List<List<String>> cycles = graph.findCycles();
        assertEquals(1, cycles.size());

        List<String> cycle = cycles.get(0);
        assertEquals(List.of("P1", "R2", "P2", "R1", "P1"), cycle);
        assertTrue(cycle.contains("P1"));
        assertTrue(cycle.contains("R1"));
        assertTrue(cycle.contains("P2"));
        assertTrue(cycle.contains("R2"));
    }

    @Test
    void chainWithNoLoopProducesNoCycles() {
        Scenario scenario = new Scenario("Chain");
        Resource r1 = new Resource("R1", 1, false);
        Process p1 = new Process("P1", 1);
        Process p2 = new Process("P2", 1);

        p1.setWaitingFor("R1", 1);
        r1.grant(1);
        p2.holdInstances("R1", 1);

        scenario.addResource(r1);
        scenario.addProcess(p1);
        scenario.addProcess(p2);

        Graph graph = new Graph(scenario);
        assertTrue(graph.findCycles().isEmpty());
    }

    @Test
    void edgesAndNodesCannotBeModifiedByCaller() {
        Scenario scenario = new Scenario("Test");
        scenario.addResource(new Resource("R1", 1, false));
        scenario.addProcess(new Process("P1", 1));

        Graph graph = new Graph(scenario);
        assertThrows(UnsupportedOperationException.class, () -> graph.nodes().add("P2"));
        assertThrows(UnsupportedOperationException.class, () ->
                graph.edges().add(new Graph.Edge("P1", "R1", Graph.EdgeKind.REQUEST, 1)));
    }

    @Test
    void getScenarioReturnsSameInstance() {
        Scenario scenario = new Scenario("Test");
        Graph graph = new Graph(scenario);
        assertSame(scenario, graph.getScenario());
    }

    @Test
    void graphConstructorRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Graph(null));
    }

    @Test
    void edgeValidationAndEquality() {
        assertThrows(IllegalArgumentException.class, () -> new Graph.Edge(null, "R1", Graph.EdgeKind.REQUEST, 1));
        assertThrows(IllegalArgumentException.class, () -> new Graph.Edge("", "R1", Graph.EdgeKind.REQUEST, 1));
        assertThrows(IllegalArgumentException.class, () -> new Graph.Edge("P1", null, Graph.EdgeKind.REQUEST, 1));
        assertThrows(IllegalArgumentException.class, () -> new Graph.Edge("P1", "", Graph.EdgeKind.REQUEST, 1));
        assertThrows(IllegalArgumentException.class, () -> new Graph.Edge("P1", "R1", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Graph.Edge("P1", "R1", Graph.EdgeKind.REQUEST, 0));
        assertThrows(IllegalArgumentException.class, () -> new Graph.Edge("P1", "R1", Graph.EdgeKind.REQUEST, -1));

        Graph.Edge e1 = new Graph.Edge("P1", "R1", Graph.EdgeKind.REQUEST, 1);
        Graph.Edge e2 = new Graph.Edge("P1", "R1", Graph.EdgeKind.REQUEST, 1);
        Graph.Edge e3 = new Graph.Edge("P1", "R1", Graph.EdgeKind.REQUEST, 2);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
        assertTrue(e1.toString().contains("P1"));
    }
}
