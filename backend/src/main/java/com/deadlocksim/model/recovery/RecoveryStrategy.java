package com.deadlocksim.model.recovery;

import com.deadlocksim.model.Process;
import com.deadlocksim.model.Scenario;

public interface RecoveryStrategy {
    void apply(Scenario s, Process victim);
}
