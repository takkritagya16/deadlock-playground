# 6. Design Decisions

## Avoidance

The Banker's safety check and resource-request algorithm are a core feature. A request is tentatively granted, the safety check runs, and the result is either a safe sequence or the reason the state is unsafe. If the state is unsafe, the request is denied and the process waits.

## Prevention Toggles

Each Coffman condition has a live switch on the current scenario:

- Mutual exclusion: marks a resource type as shareable, so grants never block on it.
- Hold and wait: a process must request all its resources up front, and it holds nothing if it cannot get them all.
- No preemption: allows the system to take a resource from a process by force.
- Circular wait: enforces a global resource ordering. A process may only request a resource ranked above the ones it holds. Out-of-order requests are rejected with an explanation.

## Victim Policies

There are three policies, and recovery cost is the default:

- Priority: the lowest-priority deadlocked process is the victim.
- Resources held: the process holding the fewest resources is the victim, since it loses the least.
- Recovery cost (default): the process with the lowest cost is the victim.

Recovery cost formula: cost = resourcesHeld + instructionsExecuted

Ties go to the lower process ID.

## Preemption Rules

- Any resource instance held by a deadlocked process can be preempted, except a shareable resource, which cannot cause a deadlock.
- The preempted process is rolled back to its initial state. It releases everything and restarts its request list from the beginning.
- Checkpoint-based rollback is explained in the docs but not simulated.
- Preemption is used only during recovery, or when the no-preemption toggle is off.

## Execution History

- Event types: REQUEST, GRANT, BLOCK, RELEASE, STATE_CHANGE, AVOIDANCE_CHECK (safe or unsafe, with the sequence), DETECT, VICTIM_SELECTED, TERMINATE, PREEMPT, ROLLBACK, RESOLVED.
- Navigation: users can step backward and forward, or click any event to jump to that state.
- Storage: each event stores a snapshot of the system. At this scale that is cheap, and it makes stepping back a simple restore.
- The history itself is read-only.

## Process States

- Running: able to proceed.
- Waiting: blocked on a request.
- Completed: all operations are finished.
- Terminated: killed during recovery.

Ready is not needed, because there is no CPU scheduler. "Deadlocked" is a flag on a process, not a state. Rollback returns a process to Running.

## Deferred

- In-app docs page list: decided at the end of the project.
