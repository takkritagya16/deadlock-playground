# 5. CRC Cards

## 1. Process

- Responsibilities: Knows its ID, priority, state, held resources, and pending request and release sequence. Reports whether it is blocked. Updates its own state.
- Collaborators: Resource (holds and requests instances), Scenario (belongs to it), SafetyChecker (supplies its need and allocation), RecoveryStrategy (can be terminated or rolled back).

## 2. Resource

- Responsibilities: Knows its name, total instances, shareable flag, and available count. Grants and returns instances while keeping the count consistent.
- Collaborators: Process (the holders), SimulationEngine (requests arrive through it), Graph (exposed for rendering).

## 3. Scenario

- Responsibilities: Holds the full configuration: processes, resources, operations, and prevention toggles. Validates itself. Creates the initial snapshot.
- Collaborators: Process, Resource, SimulationEngine (executes it), Snapshot.

## 4. SimulationEngine (Facade)

- Responsibilities: The single entry point for all use cases (configure, request, release, step, detect, recover, reset). Coordinates the other objects and records history for each action.
- Collaborators: Scenario, Process, Resource, SafetyChecker, DetectionStrategy, VictimPolicy, RecoveryStrategy, History.

## 5. SafetyChecker

- Responsibilities: Runs the Banker's safety algorithm on a tentative grant. Returns a SafetyResult with either a safe sequence or the reason it failed.
- Collaborators: Process (need and allocation), Resource (available counts), SafetyResult.

## 6. DetectionStrategy (interface: CycleDetection, BankersDetection)

- Responsibilities: Given the current state, decides whether a deadlock exists. Returns a DetectionResult with the deadlocked processes and the cycle or dependency chain.
- Collaborators: Graph, Process, Resource, DetectionResult.

## 7. VictimPolicy (interface: PriorityPolicy, ResourcesHeldPolicy, RecoveryCostPolicy)

- Responsibilities: Given the deadlocked processes, returns the one to terminate. Breaks ties by choosing the lower process ID.
- Collaborators: Process, DetectionResult.

## 8. RecoveryStrategy (interface: Terminate, Preempt)

- Responsibilities: Breaks a deadlock using the chosen victim. Terminate releases all of the victim's resources. Preempt takes one resource and rolls the process back to its initial state.
- Collaborators: Process, Resource, History.

## 9. Graph (Resource Allocation Graph)

- Responsibilities: Derives nodes and edges from the current state. Exposes cycles for detection. Produces the data the UI renders. Stores no independent state.
- Collaborators: Process, Resource, DetectionStrategy, the frontend renderer (through the API).

## 10. History

- Responsibilities: Keeps an ordered list of HistoryEvents. Adds events as operations happen. Returns an event or a snapshot when the user steps backward or jumps to a point.
- Collaborators: HistoryEvent, Snapshot, SimulationEngine (the only writer).

## 11. HistoryEvent

- Responsibilities: Stores an event type, a description, a sequence number, and a reference to its Snapshot. Immutable once created.
- Collaborators: EventType, Snapshot, History.

## 12. Snapshot

- Responsibilities: Stores a full copy of the system state at one moment. Restores that state into the engine. Copies deeply, so later changes do not affect it.
- Collaborators: Scenario, Process, Resource, History.

## 13. Request and Release (operations)

- Responsibilities: Describe one pending operation: the process, the resource, and the instance count. Executed by SimulationEngine.
- Collaborators: Process, Resource, SimulationEngine.

## 14. DetectionResult and SafetyResult

- Responsibilities: Carry outputs only, with simple accessors. DetectionResult holds the deadlocked process IDs and the cycle or dependency chain. SafetyResult holds a safe flag plus either the safe sequence or the blocking reason.
- Collaborators: DetectionStrategy and SafetyChecker (producers), SimulationEngine and VictimPolicy (consumers).

## Collaboration Walkthrough: UC6 (Recover from Deadlock)

1. User calls recover(policy) on SimulationEngine.
2. SimulationEngine calls detect(state) on DetectionStrategy, which returns a DetectionResult.
3. SimulationEngine calls selectVictim(result) on VictimPolicy, which returns a Process.
4. SimulationEngine calls apply(victim) on RecoveryStrategy.
5. RecoveryStrategy releases the victim's held instances through Process and Resource.
6. SimulationEngine runs detection again to confirm that the deadlock is resolved.
7. SimulationEngine records RESOLVED, or the next VICTIM_SELECTED, in History.

Enums (ProcessState, EventType) and prevention toggles hold no behaviour, so they have no CRC cards.
