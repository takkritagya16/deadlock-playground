package com.deadlocksim.engine;

import java.util.Collections;
import java.util.List;

public record RecoveryReport(List<RecoveryRound> rounds, boolean resolved) {
    public RecoveryReport {
        rounds = rounds != null ? Collections.unmodifiableList(rounds) : Collections.emptyList();
    }
}
