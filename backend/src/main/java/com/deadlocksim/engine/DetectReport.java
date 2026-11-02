package com.deadlocksim.engine;

import com.deadlocksim.model.detection.DetectionResult;

public record DetectReport(String strategy, DetectionResult result) {
}
