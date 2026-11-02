# 8. JSON API Contract

This file defines the HTTP interface between the Java backend (`com.sun.net.httpserver`) and the browser frontend (vanilla JS with Cytoscape.js). Both sides build against this document, so the shapes below are fixed unless this file is changed first.

## General Rules

- Base URL: `http://localhost:8080/api`
- All request and response bodies are JSON (UTF-8).
- Process IDs and resource IDs are strings, such as `"P1"` and `"R2"`.
- Every mutating endpoint returns the full updated state, so the frontend never has to merge partial data.
- Every successful response has `"ok": true`. Failed responses have `"ok": false` and an `error` object.

### Error Shape

```json
{
  "ok": false,
  "error": {
    "code": "PREVENTION_VIOLATION",
    "message": "Request P2 to R1 is out of global order under circular-wait prevention."
  }
}
```

Error codes:

| Code | Used when |
|---|---|
| `VALIDATION_ERROR` | Duplicate IDs, zero instances, or a request larger than the total instances (UC1, alternate 5a). |
| `PREVENTION_VIOLATION` | A prevention rule rejects a request (UC2, alternate 2a). |
| `NOT_HELD` | A release targets a resource the process does not hold (UC3, alternate 1a). |
| `NO_OPERATIONS` | Step is called when the scenario is finished (UC4, alternate 2a). |
| `NO_DEADLOCK` | Recover is called without a detected deadlock (UC6 precondition). |
| `INVALID_POLICY` | An unknown victim policy or recovery strategy name is given. |
| `INVALID_HISTORY_INDEX` | A history jump points outside the recorded events. |

## State Object

Every endpoint returns this object as `data.state`. It is the single source the frontend renders from.

```json
{
  "scenario": { "name": "Textbook figure" },
  "step": 4,
  "prevention": {
    "mutualExclusion": false,
    "holdAndWait": false,
    "noPreemption": false,
    "circularWait": false
  },
  "processes": [
    {
      "id": "P1",
      "priority": 2,
      "state": "WAITING",
      "deadlocked": false,
      "held": [ { "resource": "R2", "count": 1 } ],
      "waitingFor": { "resource": "R1", "count": 1 },
      "pendingOperations": 2
    }
  ],
  "resources": [
    { "id": "R1", "name": "R1", "total": 1, "available": 0, "shareable": false }
  ],
  "graph": {
    "nodes": [
      { "data": { "id": "P1", "label": "P1", "type": "process" } },
      { "data": { "id": "R1", "label": "R1", "type": "resource", "instances": 1 } }
    ],
    "edges": [
      { "data": { "id": "e1", "source": "R2", "target": "P1", "kind": "allocation", "inCycle": false } },
      { "data": { "id": "e2", "source": "P1", "target": "R1", "kind": "request", "inCycle": false } }
    ]
  }
}
```

Field notes:

- `state` is one of `RUNNING`, `WAITING`, `COMPLETED`, `TERMINATED`. `deadlocked` is a separate flag, as decided in the design.
- `graph` follows the Cytoscape.js `data` wrapper, so it can be passed to Cytoscape without conversion.
- `inCycle` is true for edges that belong to a detected cycle. It is false until detection runs.

## Endpoints

### GET /api/state

Returns the current state.

Response:

```json
{ "ok": true, "data": { "state": { } } }
```

### POST /api/scenario (UC1: Configure)

Loads a scenario from the form or a preset.

Request, custom scenario:

```json
{
  "name": "My scenario",
  "prevention": { "mutualExclusion": false, "holdAndWait": false, "noPreemption": false, "circularWait": false },
  "resources": [ { "id": "R1", "total": 1, "shareable": false } ],
  "processes": [
    {
      "id": "P1",
      "priority": 1,
      "operations": [
        { "type": "REQUEST", "resource": "R1", "count": 1 },
        { "type": "RELEASE", "resource": "R1", "count": 1 }
      ]
    }
  ]
}
```

Request, preset:

```json
{ "preset": "TEXTBOOK_FIGURE" }
```

Response: `{ "ok": true, "data": { "state": { } } }`. History is reset, and the initial snapshot becomes the first event.

### POST /api/request (UC2: Request resource)

Request:

```json
{ "process": "P2", "resource": "R1", "count": 1 }
```

Response:

```json
{
  "ok": true,
  "data": {
    "outcome": "GRANTED",
    "reason": "Safe sequence found.",
    "safety": { "safe": true, "sequence": ["P1", "P2", "P3"], "blockingReason": null },
    "state": { }
  }
}
```

`outcome` is `GRANTED` or `BLOCKED`. When the state is unsafe, `safety.safe` is false, `sequence` is null, and `blockingReason` describes the process and resource that cannot finish.

### POST /api/release (UC3: Release resource)

Request:

```json
{ "process": "P2", "resource": "R1", "count": 1 }
```

Response: `{ "ok": true, "data": { "unblocked": ["P1"], "state": { } } }`

`unblocked` lists processes that moved from Waiting to Running because of this release.

### POST /api/step (UC4: Step)

Request: empty body `{}`.

Response:

```json
{
  "ok": true,
  "data": {
    "executed": { "process": "P1", "type": "REQUEST", "resource": "R1", "count": 1 },
    "finished": false,
    "state": { }
  }
}
```

### POST /api/detect (UC5: Detect deadlock)

Request: empty body `{}`.

Response:

```json
{
  "ok": true,
  "data": {
    "strategy": "CYCLE",
    "deadlocked": true,
    "processes": ["P1", "P2"],
    "cycle": ["P1", "R1", "P2", "R2", "P1"],
    "state": { }
  }
}
```

`strategy` is `CYCLE` for single-instance resources and `BANKERS` otherwise. `cycle` is null when no cycle exists, and it is also null for `BANKERS` when the chain is not a simple cycle. In that case `processes` still lists the blocked set.

### POST /api/recover (UC6: Recover)

Request:

```json
{ "policy": "RECOVERY_COST", "strategy": "TERMINATE" }
```

`policy` is `PRIORITY`, `RESOURCES_HELD`, or `RECOVERY_COST` (default). `strategy` is `TERMINATE` or `PREEMPT`.

Response:

```json
{
  "ok": true,
  "data": {
    "rounds": [
      { "victim": "P2", "strategy": "TERMINATE", "resolvedAfter": true }
    ],
    "resolved": true,
    "state": { }
  }
}
```

`rounds` holds one entry per victim, because alternate flow 4a can repeat recovery. `resolved` is false if the system could not clear the deadlock.

### POST /api/reset (UC7: Reset)

Request: `{ "confirmed": true }`

If the history is not empty and `confirmed` is false, the response is `VALIDATION_ERROR` with a message asking for confirmation. This matches alternate flow 1a.

Response: `{ "ok": true, "data": { "state": { } } }`

### GET /api/history

Returns the event list.

Response:

```json
{
  "ok": true,
  "data": {
    "events": [
      { "seq": 0, "type": "REQUEST", "description": "P1 requests R1" },
      { "seq": 1, "type": "GRANT", "description": "R1 granted to P1" }
    ]
  }
}
```

### POST /api/history/jump

Restores the snapshot stored at one event, so the user can step backward or click any event.

Request: `{ "seq": 1 }`

Response: `{ "ok": true, "data": { "state": { } } }`

Jumping does not delete later events. The history stays read-only, as decided in the design.

## Ownership

- Backend: owns every field in the state object. The frontend never computes allocation, detection, or safety results.
- Frontend: sends user actions and renders the returned state. It owns layout and styling only.
