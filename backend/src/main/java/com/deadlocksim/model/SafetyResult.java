package com.deadlocksim.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SafetyResult {
    private final boolean safe;
    private final String reason;
    private final List<String> sequence;

    private SafetyResult(boolean safe, String reason, List<String> sequence) {
        this.safe = safe;
        this.reason = reason;
        this.sequence = sequence != null ? Collections.unmodifiableList(sequence) : Collections.emptyList();
    }

    public static SafetyResult safe(String reason, List<String> sequence) {
        return new SafetyResult(true, reason, sequence);
    }

    public static SafetyResult unsafe(String reason, List<String> sequence) {
        return new SafetyResult(false, reason, sequence);
    }

    public boolean isSafe() {
        return safe;
    }

    public boolean safe() {
        return safe;
    }

    public String getReason() {
        return reason;
    }

    public String reason() {
        return reason;
    }

    public List<String> getSequence() {
        return sequence;
    }

    public List<String> sequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SafetyResult that = (SafetyResult) o;
        return safe == that.safe && Objects.equals(reason, that.reason) && Objects.equals(sequence, that.sequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(safe, reason, sequence);
    }
}
