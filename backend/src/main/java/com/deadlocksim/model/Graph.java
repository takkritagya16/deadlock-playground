package com.deadlocksim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Graph {
    private final Scenario scenario;

    public Graph(Scenario s) {
        if (s == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        this.scenario = s;
    }

    public enum EdgeKind {
        REQUEST,
        ALLOCATION
    }

    public static class Edge {
        private final String from;
        private final String to;
        private final EdgeKind kind;
        private final int count;

        public Edge(String from, String to, EdgeKind kind, int count) {
            if (from == null || from.isBlank()) {
                throw new IllegalArgumentException("From node must not be null or blank");
            }
            if (to == null || to.isBlank()) {
                throw new IllegalArgumentException("To node must not be null or blank");
            }
            if (kind == null) {
                throw new IllegalArgumentException("EdgeKind must not be null");
            }
            if (count <= 0) {
                throw new IllegalArgumentException("Count must be greater than zero");
            }
            this.from = from;
            this.to = to;
            this.kind = kind;
            this.count = count;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public EdgeKind getKind() {
            return kind;
        }

        public int getCount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Edge edge = (Edge) o;
            return count == edge.count &&
                    Objects.equals(from, edge.from) &&
                    Objects.equals(to, edge.to) &&
                    kind == edge.kind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, kind, count);
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    ", kind=" + kind +
                    ", count=" + count +
                    '}';
        }
    }

    public Scenario getScenario() {
        return scenario;
    }

    public List<String> nodes() {
        List<String> list = new ArrayList<>();
        for (Process p : scenario.getProcesses()) {
            list.add(p.getId());
        }
        for (Resource r : scenario.getResources()) {
            list.add(r.getId());
        }
        return Collections.unmodifiableList(list);
    }

    public List<Edge> edges() {
        List<Edge> list = new ArrayList<>();
        for (Process p : scenario.getProcesses()) {
            if (p.isWaiting()) {
                list.add(new Edge(p.getId(), p.getWaitingResourceId(), EdgeKind.REQUEST, p.getWaitingCount()));
            }
        }
        for (Process p : scenario.getProcesses()) {
            for (Map.Entry<String, Integer> entry : p.getHeld().entrySet()) {
                list.add(new Edge(entry.getKey(), p.getId(), EdgeKind.ALLOCATION, entry.getValue()));
            }
        }
        return Collections.unmodifiableList(list);
    }

    public List<List<String>> findCycles() {
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (String node : nodes()) {
            adj.put(node, new ArrayList<>());
        }
        for (Edge edge : edges()) {
            List<String> neighbors = adj.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>());
            if (!neighbors.contains(edge.getTo())) {
                neighbors.add(edge.getTo());
            }
        }

        List<List<String>> cycles = new ArrayList<>();
        for (String startNode : nodes()) {
            List<String> path = new ArrayList<>();
            Set<String> visitedOnPath = new LinkedHashSet<>();
            findCyclesDfs(startNode, startNode, adj, path, visitedOnPath, cycles);
        }

        cycles.sort((c1, c2) -> {
            int len = Math.min(c1.size(), c2.size());
            for (int i = 0; i < len; i++) {
                int cmp = c1.get(i).compareTo(c2.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(c1.size(), c2.size());
        });

        return Collections.unmodifiableList(cycles);
    }

    private void findCyclesDfs(String startNode, String current,
                               Map<String, List<String>> adj,
                               List<String> path,
                               Set<String> visitedOnPath,
                               List<List<String>> cycles) {
        path.add(current);
        visitedOnPath.add(current);

        List<String> neighbors = adj.getOrDefault(current, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (neighbor.equals(startNode)) {
                if (path.size() >= 2) {
                    List<String> cycle = new ArrayList<>(path);
                    cycle.add(startNode);
                    List<String> normalized = normalizeCycle(cycle);
                    if (!cycles.contains(normalized)) {
                        cycles.add(normalized);
                    }
                }
            } else if (!visitedOnPath.contains(neighbor) && neighbor.compareTo(startNode) > 0) {
                findCyclesDfs(startNode, neighbor, adj, path, visitedOnPath, cycles);
            }
        }

        path.remove(path.size() - 1);
        visitedOnPath.remove(current);
    }

    private List<String> normalizeCycle(List<String> cycle) {
        if (cycle.size() <= 1) {
            return cycle;
        }
        List<String> nodes = cycle.subList(0, cycle.size() - 1);
        int minIndex = 0;
        String minVal = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            if (nodes.get(i).compareTo(minVal) < 0) {
                minVal = nodes.get(i);
                minIndex = i;
            }
        }
        List<String> normalized = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            normalized.add(nodes.get((minIndex + i) % nodes.size()));
        }
        normalized.add(minVal);
        return normalized;
    }
}
