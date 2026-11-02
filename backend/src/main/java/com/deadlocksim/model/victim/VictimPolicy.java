package com.deadlocksim.model.victim;

import com.deadlocksim.model.Process;

import java.util.List;

public interface VictimPolicy {
    Process selectVictim(List<Process> deadlocked);
}
