package com.deadlocksim.model;

import java.util.Objects;

public class PreventionToggles {
    private boolean mutualExclusion = false;
    private boolean holdAndWait = false;
    private boolean noPreemption = false;
    private boolean circularWait = false;

    public PreventionToggles() {
    }

    public boolean isMutualExclusion() {
        return mutualExclusion;
    }

    public boolean getMutualExclusion() {
        return mutualExclusion;
    }

    public void setMutualExclusion(boolean mutualExclusion) {
        this.mutualExclusion = mutualExclusion;
    }

    public boolean isHoldAndWait() {
        return holdAndWait;
    }

    public boolean getHoldAndWait() {
        return holdAndWait;
    }

    public void setHoldAndWait(boolean holdAndWait) {
        this.holdAndWait = holdAndWait;
    }

    public boolean isNoPreemption() {
        return noPreemption;
    }

    public boolean getNoPreemption() {
        return noPreemption;
    }

    public void setNoPreemption(boolean noPreemption) {
        this.noPreemption = noPreemption;
    }

    public boolean isCircularWait() {
        return circularWait;
    }

    public boolean getCircularWait() {
        return circularWait;
    }

    public void setCircularWait(boolean circularWait) {
        this.circularWait = circularWait;
    }

    public PreventionToggles copy() {
        PreventionToggles clone = new PreventionToggles();
        clone.mutualExclusion = this.mutualExclusion;
        clone.holdAndWait = this.holdAndWait;
        clone.noPreemption = this.noPreemption;
        clone.circularWait = this.circularWait;
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PreventionToggles that = (PreventionToggles) o;
        return mutualExclusion == that.mutualExclusion &&
                holdAndWait == that.holdAndWait &&
                noPreemption == that.noPreemption &&
                circularWait == that.circularWait;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutualExclusion, holdAndWait, noPreemption, circularWait);
    }
}
