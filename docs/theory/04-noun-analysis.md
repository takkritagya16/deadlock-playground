# 4. Noun Analysis (UC1 to UC7)

| Noun | Classification | Reason |
|---|---|---|
| Scenario | Relevant (Configuration) | Holds the full setup: processes, resources, and operations. |
| Process | Relevant | Has a state, a priority, a request and release sequence, and holdings. |
| Resource type | Relevant (Resource) | Has a name, an instance count, and a shareable flag. |
| Instance / instance count | Fuzzy | Could be an attribute of Resource or its own class. An attribute is simpler, since instances have no behaviour of their own. |
| Instance pool | Fuzzy | Really the available count per Resource. Kept as an attribute unless the allocation logic needs more. |
| Request | Relevant (Operation) | Has a process, a resource, and an outcome (granted or blocked). |
| Release | Relevant (Operation) | Same shape as Request, so they may share a base type. |
| Allocation | Relevant (Association) | Represents held instances. Modelled as a relationship, not a standalone class. |
| Allocation edge / request edge | Fuzzy | Graph rendering concepts. Derived from allocations and waits, not stored. |
| Graph (RAG) | Relevant | A derived view that the UI renders and the detector reads. |
| Prevention toggle | Fuzzy | Four booleans on Scenario, or a PreventionPolicy class. Decided during class design. |
| Prevention rule | Fuzzy | Only the circular-wait ordering check needs its own logic. |
| Banker's safety check | Relevant (SafetyChecker) | An algorithm with its own behaviour, used by Request. |
| Safety result | Relevant (SafetyResult) | Returned by the check. Carries the safe sequence or the reason it failed. |
| Process state | Fuzzy | Better as an enum than a class, since it has four fixed values. |
| Deadlock | Fuzzy | The result of detection, not a live object. Modelled as DetectionResult. |
| Detection result | Relevant (DetectionResult) | Holds the deadlocked processes and the cycle or dependency chain. |
| Detection strategy | Relevant (Strategy interface) | Cycle detection and Banker's-style detection are interchangeable. |
| Cycle / dependency chain | Fuzzy | Part of DetectionResult. Only a class if it needs its own behaviour. |
| Victim | Fuzzy | A role a Process plays after selection, not a separate class. |
| Victim policy | Relevant (Strategy interface) | Priority, resources held, and recovery cost are interchangeable. |
| Recovery cost | Fuzzy | A computed value (resourcesHeld + instructionsExecuted). Computed inside the policy. |
| Recovery strategy | Relevant (Strategy interface) | Terminate and Preempt are interchangeable ways to recover. |
| Preemption | Relevant (Recovery strategy) | Has its own rule (roll back the process), so it earns a class. |
| Rollback | Fuzzy | An action within Preemption, not a class. |
| Operation / step | Relevant (Simulation engine) | The engine executes the next operation and advances the state. |
| Simulation engine | Relevant (Facade) | The single entry point the UI calls for every use case. |
| Snapshot | Relevant | Stores a full copy of the state so the user can step back. |
| History / history event | Relevant (History, HistoryEvent) | Records typed events in order. |
| Event type | Fuzzy | An enum, not a class. |
| Preset scenario | Fuzzy | A way of creating a Scenario. Justifies Factory Method only if the sources truly differ. |
| Validation error | Fuzzy | An exception or a result object. Decided with the validation design. |
| Duplicate IDs, zero instances | Irrelevant | Validation rules, not objects. |
| Application | Irrelevant | Describes the whole system, not a class. |
| User / actor | Irrelevant | An external actor, not a class in the system. |
| Panel / button / form | Irrelevant | UI elements that belong to the view layer. |
| Confirmation | Irrelevant | A UI prompt. |
| Textbook figure | Irrelevant | Example data, not a class. |
| Priority | Irrelevant (attribute) | A field on Process. |
| Tie / lower process ID | Irrelevant | Logic inside the victim policy. |
| Textbook / OS course | Irrelevant | Context only. |

## Resulting Class Candidates

Scenario, Process, Resource, Request, Release, Graph, SafetyChecker, SafetyResult, DetectionResult, DetectionStrategy (CycleDetection, BankersDetection), VictimPolicy (PriorityPolicy, ResourcesHeldPolicy, RecoveryCostPolicy), RecoveryStrategy (Terminate, Preempt), SimulationEngine, Snapshot, History, HistoryEvent.

Enums: ProcessState, EventType.
