# 9. Requirements

This file lists what the system must do (functional requirements) and the qualities it must have (non-functional requirements). A feature is complete when every requirement that mentions it is met.

## Functional Requirements

### Configuration (UC1)

- FR-01: The user can add, rename, and remove processes.
- FR-02: The user can set each process's priority.
- FR-03: The user can add resource types and set the instance count and shareable flag for each.
- FR-04: The user can define each process's ordered request and release sequence.
- FR-05: The system rejects invalid input with a clear message: duplicate IDs, zero instances, or a request larger than the total instances.
- FR-06: The user can load a preset scenario, starting with the textbook figure.

### Request and Release (UC2, UC3)

- FR-07: A request is granted only if a free instance exists and the safety check passes.
- FR-08: A request that cannot be granted moves the process to Waiting and draws a request edge.
- FR-09: When the safety check fails, the system shows the unsafe reasoning and denies the request.
- FR-10: A release returns instances to the pool and moves any Waiting process that can now proceed to Running.
- FR-11: A release of a resource the process does not hold is rejected with an error.

### Avoidance and Prevention

- FR-12: The system runs the Banker's safety algorithm and returns either a safe sequence or the blocking reason.
- FR-13: Each of the four prevention toggles can be switched on or off for the current scenario.
- FR-14: With mutual exclusion prevention on, shareable resources never block a grant.
- FR-15: With hold-and-wait prevention on, a process requests all its resources at the start and holds nothing if it cannot get them all.
- FR-16: With no-preemption prevention off, the system can take a resource from a deadlocked process.
- FR-17: With circular-wait prevention on, a request to a resource ranked at or below a held resource is rejected with an explanation.

### Simulation (UC4)

- FR-18: Step executes the next pending operation in scenario order.
- FR-19: Step shows a finished message and disables itself when no operations remain.
- FR-20: If the next operation is blocked, the process moves to Waiting and the following Step continues the scenario.
- FR-21: The process states shown are Running, Waiting, Completed, and Terminated, with a separate deadlocked flag.

### Detection (UC5)

- FR-22: If every resource type has one instance, the system uses cycle detection.
- FR-23: If any resource type has more than one instance, the system uses Banker's-style detection.
- FR-24: Detection returns the deadlocked processes and the cycle or dependency chain.
- FR-25: The graph highlights cycle edges and deadlocked processes. The panels show the same information as text.
- FR-26: When no deadlock exists, the system says so and records that result.

### Recovery (UC6)

- FR-27: The user can choose one of three victim policies: priority, resources held, or recovery cost. Recovery cost is the default.
- FR-28: Recovery cost is computed as resourcesHeld plus instructionsExecuted.
- FR-29: Ties between victims are broken by the lower process ID.
- FR-30: Terminate releases all of the victim's resources and sets its state to Terminated.
- FR-31: Preempt takes one held instance from a deadlocked process and rolls that process back to its initial state, releasing everything it holds.
- FR-32: After each recovery round, detection runs again. If a deadlock remains, the next victim is chosen.
- FR-33: The system reports whether recovery succeeded and records RESOLVED in history.

### Reset (UC7)

- FR-34: Reset restores the scenario to its initial configuration, with all processes Running and no allocations.
- FR-35: If history is not empty, Reset asks for confirmation before clearing it.

### History

- FR-36: Every request, grant, block, release, state change, avoidance check, detection, victim selection, termination, preemption, rollback, and resolution is recorded in order.
- FR-37: The user can step backward and forward through history, or click an event to jump to its state.
- FR-38: The history is read-only. Jumping to an earlier event does not delete later events.

### Visualisation and Interface

- FR-39: The Resource Allocation Graph shows processes as circles, resource types as rectangles, and instances as dots.
- FR-40: Request edges and allocation edges are visually distinct.
- FR-41: The graph updates after every operation.
- FR-42: The interface has controls for Configure, Request, Release, Step, Detect, Recover, Reset, and history navigation.
- FR-43: The interface shows panels for process information, resource information, and the event history.
- FR-44: Error messages from the backend are shown to the user next to the control that caused them.

### Learning Docs

- FR-45: The application includes in-app documentation for each concept listed in the theory files. The final page list is decided at the end of the project.

## Non-Functional Requirements

### Correctness

- NFR-01: Detection and safety results match the textbook algorithms for the same input.
- NFR-02: Unit tests cover cycle detection, Banker's detection, the safety algorithm, each victim policy, and each recovery strategy.

### Design Quality

- NFR-03: The Java model has no dependency on the HTTP layer or the browser.
- NFR-04: The frontend never computes allocation, detection, or safety results. The backend owns them.
- NFR-05: Strategies and policies can be added without changing the SimulationEngine.

### Usability

- NFR-06: A first-time user can load the textbook figure and see a deadlock detected in under five minutes, using only the on-screen controls.
- NFR-07: The interface uses a plain, classic layout: an off-white background, dark text, and a serif font for body text.
- NFR-08: Deadlocked elements are shown in a colour that stands out from the rest of the graph, and the panels also state it in text, so colour is not the only signal.

### Accessibility

- NFR-09: All controls can be reached with the keyboard.
- NFR-10: Text meets a contrast ratio of at least 4.5 to 1 against its background.

### Performance

- NFR-11: A scenario with up to 10 processes and 10 resource types loads, steps, and detects in under one second on a typical student laptop.
- NFR-12: Stepping through history restores a snapshot without re-running earlier operations.

### Portability

- NFR-13: The project runs on Windows, macOS, and Linux with JDK 17 or newer and a modern browser.
- NFR-14: The project builds with Maven and needs no database or external service.

### Maintainability

- NFR-15: Each pattern (Strategy, Facade, and Factory Method, if implemented) is located in a named package, and the README states where each one lives.
- NFR-16: Source files follow one naming convention throughout: PascalCase for classes and camelCase for methods and fields.

## Out of Scope

- Performance dashboards and utilisation analysis.
- Auto-play Start and Pause controls.
- Persistence, multi-user support, and real OS integration.
- Simulation of checkpoint rollback, livelock, and starvation. These are explained in the in-app docs only.
