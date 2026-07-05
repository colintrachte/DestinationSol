// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LightningPathTest {

    @Test
    public void boltEndsExactlyOnTarget() {
        LightningGeometry geo = LightningPath.generate(0, 0, 100, 0, 5f, 30f, 0f,
                MetalLocator.NONE, new Random(1), 3);
        Vector2 last = geo.points.get(geo.points.size() - 1);
        assertEquals(100f, last.x, 1e-3f);
        assertEquals(0f, last.y, 1e-3f);
    }

    @Test
    public void hasMultipleSegmentsAcrossDistance() {
        LightningGeometry geo = LightningPath.generate(0, 0, 100, 0, 5f, 30f, 0f,
                MetalLocator.NONE, new Random(2), 3);
        assertTrue(geo.points.size() > 5, "a 100px bolt at 5px steps should zig-zag");
    }

    @Test
    public void alphasFadeAndStayInRange() {
        LightningGeometry geo = LightningPath.generate(0, 0, 80, 10, 4f, 25f, 5f,
                MetalLocator.NONE, new Random(3), 2);
        for (float a : geo.alphas) {
            assertTrue(a > 0f && a <= 1f, "alpha out of range: " + a);
        }
    }

    @Test
    public void deterministicForAGivenSeed() {
        LightningGeometry a = LightningPath.generate(0, 0, 60, 40, 4f, 30f, 0f, MetalLocator.NONE, new Random(42), 3);
        LightningGeometry b = LightningPath.generate(0, 0, 60, 40, 4f, 30f, 0f, MetalLocator.NONE, new Random(42), 3);
        assertEquals(a.totalVertices(), b.totalVertices());
        assertEquals(a.points.get(1).x, b.points.get(1).x, 1e-6f);
        assertEquals(a.points.get(1).y, b.points.get(1).y, 1e-6f);
    }

    @Test
    public void noForksWhenDepthIsZero() {
        for (int seed = 0; seed < 50; seed++) {
            LightningGeometry geo = LightningPath.generate(0, 0, 200, 0, 4f, 40f, 0f,
                    MetalLocator.NONE, new Random(seed), 0);
            assertTrue(geo.forks.isEmpty(), "no forks should appear at maxForkDepth 0");
        }
    }

    @Test
    public void zeroSegmentLengthDegradesToStraightLine() {
        LightningGeometry geo = LightningPath.generate(0, 0, 10, 10, 0f, 30f, 0f,
                MetalLocator.NONE, new Random(0), 3);
        assertEquals(2, geo.points.size());
    }

    @Test
    public void everyVertexFinite() {
        LightningGeometry geo = LightningPath.generate(0, 0, 150, -75, 3f, 45f, 10f,
                (x, y, r) -> new Vector2(x + 10, y + 10), new Random(7), 4);
        assertVerticesFinite(geo);
    }

    private void assertVerticesFinite(LightningGeometry geo) {
        for (Vector2 p : geo.points) {
            assertTrue(Float.isFinite(p.x) && Float.isFinite(p.y));
        }
        for (LightningGeometry fork : geo.forks) {
            assertVerticesFinite(fork);
        }
    }
}
