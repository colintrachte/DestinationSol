// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// These tests pin down the properties that make the softened law usable where
// naive G*m/r^2 gravity fails: it is finite at the center, it never produces
// NaN, it falls to zero past the cutoff, and far away it still pulls inward.
public class AstroGravityTest {

    private static final float K = 0.012f;
    private static final float RADIUS = 50f;
    private static final float CUTOFF = 400f;

    @Test
    public void finiteAtCenter() {
        float a = AstroGravity.accelerationMagnitude(K, RADIUS, 0f, CUTOFF);
        assertTrue(Float.isFinite(a), "acceleration must be finite at dist 0");
        assertTrue(a > 0f, "acceleration at the center should be positive, not infinite");
    }

    @Test
    public void neverNaNAcrossRange() {
        for (float d = 0f; d < CUTOFF; d += 0.5f) {
            float a = AstroGravity.accelerationMagnitude(K, RADIUS, d, CUTOFF);
            assertTrue(Float.isFinite(a), "acceleration must stay finite at dist " + d);
        }
    }

    @Test
    public void zeroBeyondCutoff() {
        assertEquals(0f, AstroGravity.accelerationMagnitude(K, RADIUS, CUTOFF, CUTOFF));
        assertEquals(0f, AstroGravity.accelerationMagnitude(K, RADIUS, CUTOFF + 100f, CUTOFF));
    }

    @Test
    public void pullPointsTowardSource() {
        Vector2 deltaV = new Vector2();
        Vector2 bodyPos = new Vector2(0, 0);
        Vector2 sourcePos = new Vector2(100, 0);
        AstroGravity.addPull(deltaV, bodyPos, sourcePos, K, RADIUS, CUTOFF, 1f / 60f);
        assertTrue(deltaV.x > 0f, "a well to the right should pull the body right");
        assertEquals(0f, deltaV.y, 1e-6f, "no sideways pull for a colinear well");
    }

    @Test
    public void strongerWhenCloser() {
        float near = AstroGravity.accelerationMagnitude(K, RADIUS, 60f, CUTOFF);
        float far = AstroGravity.accelerationMagnitude(K, RADIUS, 300f, CUTOFF);
        assertTrue(near > far, "pull should weaken with distance outside the softened core");
    }
}
