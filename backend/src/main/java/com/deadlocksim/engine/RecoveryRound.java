package com.deadlocksim.engine;

public record RecoveryRound(String victimId, String strategy, boolean resolvedAfter) {
}
