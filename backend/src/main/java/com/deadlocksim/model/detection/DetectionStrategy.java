package com.deadlocksim.model.detection;

import com.deadlocksim.model.Graph;

public interface DetectionStrategy {
    DetectionResult detect(Graph g);
}
