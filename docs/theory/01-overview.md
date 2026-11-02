# 1. Project Overview

## Problem

In an operating system, processes compete for shared resources such as printers, files, and memory. A deadlock happens when a group of processes each hold a resource and wait for a resource held by another process in the same group. None of them can move forward, so they wait forever.

Example: P1 holds R1 and requests R2, while P2 holds R2 and requests R1. Neither can finish.

Textbooks usually show deadlock as a static diagram, which makes it hard to see how the circular dependency builds up step by step. This project is meant to fill that gap.

## What the Simulator Does

The user works through a scenario in four steps:

1. Configure: define processes, resource types, instance counts, priorities, and request and release sequences.
2. Simulate: issue requests and releases one at a time and watch the Resource Allocation Graph (RAG) update.
3. Detect: check whether a deadlock exists. Deadlocked processes and the cycle are marked.
4. Recover: terminate or preempt a victim chosen by a policy, release its resources, and check that the deadlock is gone.

The aim is not to run a real operating system. The aim is to make the cause and the fix of a deadlock easy to see and easy to experiment with.

## Users

- Students learning operating systems.
- Instructors demonstrating deadlock in class.

## Scope

This is a complete learning tool. There is no later version planned, because it is a college coursework project with a single submission. All major deadlock concepts are covered, and the implementation stays a focused mini-project.

### Must-have

- Configuration of processes, resource types, instance counts, and resource requirements.
- Resource Allocation Graph with request and allocation edges, updated after each step, with cycles marked.
- Step-by-step simulation with process states.
- Deadlock detection: cycle detection for single-instance resources and Banker's-style detection for multiple-instance resources.
- Deadlock avoidance: Banker's safety check and resource-request algorithm.
- Prevention toggles for each Coffman condition.
- Deadlock recovery: process termination and resource preemption.
- Victim selection policies: priority, resources held, and recovery cost.
- Execution history with backward and forward navigation.
- Interactive UI with the graph, panels, and controls.
- In-app learning docs (page list decided at the end).

### Explained in the docs only, not simulated

- Rollback to a checkpoint, livelock, and starvation.

### Cut

- Performance dashboards and utilisation analysis.
- Auto-play Start and Pause controls.
- Persistence, multi-user support, and real OS integration.
