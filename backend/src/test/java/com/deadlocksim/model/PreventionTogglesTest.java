package com.deadlocksim.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreventionTogglesTest {

    @Test
    void allFourTogglesOffByDefault() {
        PreventionToggles toggles = new PreventionToggles();
        assertFalse(toggles.isMutualExclusion());
        assertFalse(toggles.isHoldAndWait());
        assertFalse(toggles.isNoPreemption());
        assertFalse(toggles.isCircularWait());

        assertFalse(toggles.getMutualExclusion());
        assertFalse(toggles.getHoldAndWait());
        assertFalse(toggles.getNoPreemption());
        assertFalse(toggles.getCircularWait());
    }

    @Test
    void eachToggleCanBeSwitchedOnAndOffIndependently() {
        PreventionToggles toggles = new PreventionToggles();

        toggles.setMutualExclusion(true);
        assertTrue(toggles.isMutualExclusion());
        assertFalse(toggles.isHoldAndWait());
        assertFalse(toggles.isNoPreemption());
        assertFalse(toggles.isCircularWait());
        toggles.setMutualExclusion(false);
        assertFalse(toggles.isMutualExclusion());

        toggles.setHoldAndWait(true);
        assertFalse(toggles.isMutualExclusion());
        assertTrue(toggles.isHoldAndWait());
        assertFalse(toggles.isNoPreemption());
        assertFalse(toggles.isCircularWait());
        toggles.setHoldAndWait(false);
        assertFalse(toggles.isHoldAndWait());

        toggles.setNoPreemption(true);
        assertFalse(toggles.isMutualExclusion());
        assertFalse(toggles.isHoldAndWait());
        assertTrue(toggles.isNoPreemption());
        assertFalse(toggles.isCircularWait());
        toggles.setNoPreemption(false);
        assertFalse(toggles.isNoPreemption());

        toggles.setCircularWait(true);
        assertFalse(toggles.isMutualExclusion());
        assertFalse(toggles.isHoldAndWait());
        assertFalse(toggles.isNoPreemption());
        assertTrue(toggles.isCircularWait());
        toggles.setCircularWait(false);
        assertFalse(toggles.isCircularWait());
    }

    @Test
    void copyProducesObjectEqualInValueButDifferentInstance() {
        PreventionToggles original = new PreventionToggles();
        original.setMutualExclusion(true);
        original.setCircularWait(true);

        PreventionToggles copy = original.copy();
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        assertNotSame(original, copy);
    }

    @Test
    void changingCopyDoesNotChangeOriginal() {
        PreventionToggles original = new PreventionToggles();
        PreventionToggles copy = original.copy();

        copy.setMutualExclusion(true);
        copy.setHoldAndWait(true);
        copy.setNoPreemption(true);
        copy.setCircularWait(true);

        assertFalse(original.isMutualExclusion());
        assertFalse(original.isHoldAndWait());
        assertFalse(original.isNoPreemption());
        assertFalse(original.isCircularWait());

        assertTrue(copy.isMutualExclusion());
        assertTrue(copy.isHoldAndWait());
        assertTrue(copy.isNoPreemption());
        assertTrue(copy.isCircularWait());
    }
}
