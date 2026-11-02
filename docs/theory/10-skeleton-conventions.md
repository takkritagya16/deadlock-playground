# 10. Project Skeleton and Naming Conventions

## Folder Layout

deadlock-playground/
  backend/
    pom.xml
    src/main/java/com/deadlocksim/
      model/             Scenario, Process, Resource, Snapshot, Graph, SafetyChecker
      model/detection/   DetectionStrategy, CycleDetection, BankersDetection, DetectionResult
      model/recovery/    RecoveryStrategy, Terminate, Preempt
      model/victim/      VictimPolicy, PriorityPolicy, ResourcesHeldPolicy, RecoveryCostPolicy
      model/history/     History, HistoryEvent, EventType
      engine/            SimulationEngine (Facade)
      api/               ApiServer, JsonMapper, one handler per endpoint
    src/main/resources/
    src/test/java/com/deadlocksim/
      model/
      engine/
  frontend/
    index.html
    css/
    js/graph/            Cytoscape setup and styling
    js/panels/           process, resource, and history panels
    js/api/              fetch wrappers for each endpoint
    assets/
  docs/
    theory/              written theory files
    uml/source/          .puml files
    uml/images/          exported .svg files

The model package has no imports from api or engine. That rule keeps the domain testable on its own.

## Naming Conventions

- Java classes: PascalCase (SimulationEngine, RecoveryCostPolicy).
- Java methods and fields: camelCase (selectVictim, totalInstances).
- Java constants: UPPER_SNAKE_CASE (MAX_PROCESSES).
- Enum values: UPPER_SNAKE_CASE (WAITING, CYCLE_DETECTION).
- JSON field names: camelCase, matching the API contract.
- JSON enum values: UPPER_SNAKE_CASE, matching the Java enums.
- Process IDs: P1, P2, and so on. Resource IDs: R1, R2, and so on.
- Frontend files: lowercase with hyphens (graph-view.js). Functions and variables: camelCase.
- CSS classes: lowercase with hyphens (panel-title).
- One public top-level class per Java file, named after the file.
- Tests are named after the class under test, with a Test suffix (CycleDetectionTest).

## Pattern Locations

- Strategy: model/detection, model/victim, model/recovery.
- Facade: engine/SimulationEngine.
- Factory Method: not decided yet. If added, it goes in model/factory.
