package com.deadlocksim.api;

public enum ErrorCode {
    VALIDATION_ERROR(400),
    PREVENTION_VIOLATION(400),
    NOT_HELD(400),
    NO_OPERATIONS(400),
    NO_DEADLOCK(400),
    INVALID_POLICY(400),
    INVALID_HISTORY_INDEX(400),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    NO_SCENARIO(409),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}

