package com.deadlocksim.engine;

import java.util.Collections;
import java.util.List;

public record ReleaseResult(List<String> unblockedProcessIds) {
    public ReleaseResult {
        unblockedProcessIds = unblockedProcessIds != null
                ? Collections.unmodifiableList(unblockedProcessIds)
                : Collections.emptyList();
    }
}
