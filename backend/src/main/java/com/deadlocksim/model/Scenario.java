package com.deadlocksim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scenario {
    private final String name;
    private final List<Resource> resources;
    private final List<Process> processes;
    private final Map<String, List<Operation>> operations;
    private PreventionToggles preventionToggles;

    public Scenario(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Scenario name must not be null or blank");
        }
        this.name = name;
        this.resources = new ArrayList<>();
        this.processes = new ArrayList<>();
        this.operations = new LinkedHashMap<>();
        this.preventionToggles = new PreventionToggles();
    }

    public String getName() {
        return name;
    }

    public PreventionToggles getPreventionToggles() {
        return preventionToggles.copy();
    }

    public void setPreventionToggles(PreventionToggles t) {
        if (t == null) {
            throw new IllegalArgumentException("PreventionToggles must not be null");
        }
        this.preventionToggles = t.copy();
    }

    public void addResource(Resource r) {
        if (r == null) {
            throw new IllegalArgumentException("Resource must not be null");
        }
        for (Resource existing : resources) {
            if (existing.getId().equals(r.getId())) {
                throw new IllegalArgumentException("Duplicate resource ID: " + r.getId());
            }
        }
        resources.add(r);
    }

    public void addProcess(Process p) {
        if (p == null) {
            throw new IllegalArgumentException("Process must not be null");
        }
        for (Process existing : processes) {
            if (existing.getId().equals(p.getId())) {
                throw new IllegalArgumentException("Duplicate process ID: " + p.getId());
            }
        }
        processes.add(p);
    }

    public void addOperation(Operation op) {
        if (op == null) {
            throw new IllegalArgumentException("Operation must not be null");
        }
        boolean processExists = processes.stream()
                .anyMatch(p -> p.getId().equals(op.getProcessId()));
        if (!processExists) {
            throw new IllegalArgumentException("Unknown process ID: " + op.getProcessId());
        }

        Resource targetResource = resources.stream()
                .filter(r -> r.getId().equals(op.getResourceId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown resource ID: " + op.getResourceId()));

        if (op.getType() == OperationType.REQUEST && op.getCount() > targetResource.getTotal()) {
            throw new IllegalArgumentException("Request count cannot exceed total instances of resource: " + targetResource.getId());
        }

        operations.computeIfAbsent(op.getProcessId(), k -> new ArrayList<>()).add(op);
    }

    public List<Resource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public List<Process> getProcesses() {
        return Collections.unmodifiableList(processes);
    }

    public List<Operation> getOperations(String processId) {
        List<Operation> list = operations.get(processId);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (processes.isEmpty()) {
            errors.add("Scenario must contain at least one process");
        }
        if (resources.isEmpty()) {
            errors.add("Scenario must contain at least one resource");
        }
        return errors;
    }
}
