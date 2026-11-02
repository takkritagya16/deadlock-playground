package com.deadlocksim.model;

public enum EventType {
    REQUEST,
    GRANT,
    BLOCK,
    RELEASE,
    STATE_CHANGE,
    AVOIDANCE_CHECK,
    DETECT,
    VICTIM_SELECTED,
    TERMINATE,
    PREEMPT,
    ROLLBACK,
    RESOLVED
}
