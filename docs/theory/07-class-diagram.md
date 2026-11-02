# 7. Class Diagram Design Notes

The class diagram is stored in docs/uml/source/class-diagram.puml.

## Relationships

- Composition, Scenario to Process and Resource (filled diamond): processes and resources have no meaning outside their scenario. Deleting a scenario removes its parts.
- Association, Process to Resource (0..* to 0..*): a resource exists independently of any process, and many processes can hold or wait for it. Composition would be wrong here.
- Association, SimulationEngine to Scenario: the engine works on the current scenario but does not own it.
- Composition, SimulationEngine to History (filled diamond): the engine creates and owns its history for the session.
- Dependency, SimulationEngine to SafetyChecker, DetectionStrategy, VictimPolicy, and RecoveryStrategy (dashed arrow): the engine uses these, but they are created per operation or injected, so they are not owned parts.
- Generalization, strategy interfaces (hollow triangle arrow): CycleDetection and BankersDetection implement DetectionStrategy. PriorityPolicy, ResourcesHeldPolicy, and RecoveryCostPolicy implement VictimPolicy. Terminate and Preempt implement RecoveryStrategy. Each interface varies independently of the others.
- Aggregation, History to HistoryEvent (hollow diamond): history is made of events, and an event belongs to its history list.
- Association, HistoryEvent to Snapshot (1 to 1): each event stores exactly one snapshot of the state at that point.
- Dependency, DetectionStrategy to Graph (dashed arrow): detection reads the derived graph but does not store it.

## Design Decisions

- Graph has no state of its own. It is derived from Process and Resource on demand. This avoids two sources of truth that could drift apart.
- Strategies are interfaces. Detection, victim selection, and recovery each vary independently, so the engine can swap them at runtime without changing its code.
- SimulationEngine is a Facade. The UI calls one class for all seven use cases, so the frontend never touches the internal classes.
- History stores snapshots. Stepping backward becomes a simple restore, with no need to replay operations.

## Omitted for Readability

- The RequestOutcome, SafetyResult, and DetectionResult value classes.
- The Operation supertype of Request and Release.

These will be added in the final version, because a complete class model needs them.
