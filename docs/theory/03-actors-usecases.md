# 3. Actors and Use Cases

## Actors

- User (student or instructor): configures scenarios, steps through operations, runs detection, and chooses a recovery method.
- System: the simulation engine. It validates requests, updates the graph, and runs detection and recovery. It is internal, and it is listed only to clarify which responsibilities belong where.

## UC1: Configure Scenario

- Actor: User
- Pre-condition: The application is open. No simulation is running, or the user has reset.
- Main flow:
  1. User adds processes and sets each process's priority.
  2. User adds resource types and sets the instance count for each.
  3. User defines each process's request and release sequence.
  4. User sets the prevention toggles, if wanted.
  5. System validates the configuration and draws the initial graph.
- Alternate flows:
  - 5a. Invalid input (duplicate IDs, zero instances, or a process requesting more instances than exist): the system shows an error and keeps the form open.
  - 5b. User loads a preset scenario, such as the textbook figure: the system fills in the configuration from the preset.
- Post-condition: A valid scenario is loaded, all processes are Running, and the initial graph is shown.

## UC2: Request Resource (includes avoidance check)

- Actor: User
- Pre-condition: A scenario is loaded, and the process is Running.
- Main flow:
  1. User selects a process and a resource type, then clicks Request.
  2. System checks the prevention rules, for example global ordering when circular-wait is on.
  3. System runs the Banker's safety check on the tentative grant.
  4. If the state is safe, the resource is granted and the graph updates.
  5. The event is recorded in history.
- Alternate flows:
  - 2a. A prevention rule is violated: the request is rejected, with an explanation.
  - 3a. The state is unsafe: the request is denied, the process moves to Waiting, and the unsafe reasoning is shown.
  - 4a. No instance is free: the process moves to Waiting, and a request edge is drawn.
- Post-condition: The request is granted or blocked, and the graph and history reflect the outcome.

## UC3: Release Resource

- Actor: User
- Pre-condition: The process holds at least one instance of the resource.
- Main flow:
  1. User selects a process and a held resource, then clicks Release.
  2. System removes the allocation edge and returns the instance to the pool.
  3. System checks whether any Waiting process can now be granted its request, and grants it if so.
  4. The graph and history update.
- Alternate flow:
  - 1a. The process does not hold the resource: the system shows an error.
- Post-condition: The instance is available again, and any unblocked process moves to Running.

## UC4: Step Through Simulation

- Actor: User
- Pre-condition: A scenario is loaded.
- Main flow:
  1. User clicks Step.
  2. System executes the next pending operation in scenario order.
  3. System updates the graph, the process states, and the history.
- Alternate flows:
  - 2a. No operations are pending: the system reports that the scenario is finished and disables Step.
  - 2b. Next operation is blocked: the process moves to Waiting. Its request stays pending and is granted automatically when the resource frees up, after which the process continues with its next operation.
  - 2c. User steps back through history: the system restores the snapshot from the previous event.
- Post-condition: One operation has been applied, or the user has moved back to an earlier state.

## UC5: Detect Deadlock

- Actor: User
- Pre-condition: At least one process is Waiting.
- Main flow:
  1. User clicks Detect.
  2. System selects the strategy: cycle detection if every resource has a single instance, and Banker's-style detection otherwise.
  3. System identifies the deadlocked processes and the cycle or dependency chain.
  4. Deadlocked processes are shown in red in the graph and flagged in the panels.
  5. The result is recorded in history.
- Alternate flow:
  - 3a. No deadlock is found: the system reports that the system is deadlock-free and records that result.
- Post-condition: The detection result is displayed, and deadlocked processes are flagged.

## UC6: Recover from Deadlock (includes UC5, extended by Preempt)

- Actor: User
- Pre-condition: A deadlock has been detected.
- Main flow:
  1. User selects a victim policy: priority, resources held, or recovery cost. The default is recovery cost.
  2. System selects the victim and records VICTIM_SELECTED.
  3. The victim is terminated, and its resources are released.
  4. System runs detection again to confirm that the deadlock is resolved.
  5. Waiting processes that can now proceed move to Running.
- Alternate flows:
  - 3a. User chooses preemption: a resource is taken from a deadlocked process, and that process is rolled back to its initial state. This is the Preempt extension.
  - 4a. The deadlock persists: the system repeats steps 2 to 4 with the next victim.
- Post-condition: The deadlock is resolved, or the system reports that recovery failed. History contains RESOLVED.

## UC7: Reset Scenario

- Actor: User
- Pre-condition: A scenario is loaded.
- Main flow:
  1. User clicks Reset.
  2. System restores the configuration to its initial state: all processes Running, no allocations.
  3. History is cleared, and the initial snapshot is recorded as the first entry.
- Alternate flow:
  - 1a. If the history is not empty, the user is asked to confirm, to prevent accidental loss.
- Post-condition: The scenario is back to its initial state.

## Relationships

- UC2 includes the safe-state check (avoidance).
- UC6 includes UC5, because recovery always needs detection first.
- Preempt extends UC6, because it runs only when the user chooses it.
- The User actor is connected to all seven use cases.
