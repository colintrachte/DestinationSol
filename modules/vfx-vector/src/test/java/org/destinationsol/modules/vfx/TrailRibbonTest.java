// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrailRibbonTest {

    @Test
    public void emitsTwoVerticesPerPoint() {
        List<Vector2> path = Arrays.asList(new Vector2(0, 0), new Vector2(1, 0), new Vector2(2, 0));
        float[] verts = TrailRibbon.build(path, 2f, 2f);
        assertEquals(3 * 2 * TrailRibbon.FLOATS_PER_VERTEX, verts.length);
    }

    @Test
    public void offsetsPerpendicularToAStraightPath() {
        List<Vector2> path = Arrays.asList(new Vector2(0, 0), new Vector2(1, 0), new Vector2(2, 0));
        float[] verts = TrailRibbon.build(path, 2f, 2f);
        // First point: left vertex then right vertex. Half width is 1, the path
        // runs along +x, so the offset is along y.
        assertEquals(-1f, verts[1], 1e-4f, "left vertex sits one half-width to one side");
        assertEquals(0f, verts[2], 1e-4f, "u runs 0..1 along the path");
        assertEquals(0f, verts[3], 1e-4f, "left vertex has v = 0");
        assertEquals(1f, verts[5], 1e-4f, "right vertex sits the other side");
        assertEquals(1f, verts[7], 1e-4f, "right vertex has v = 1");
    }

    @Test
    public void widthTapersFromStartToEnd() {
        List<Vector2> path = Arrays.asList(new Vector2(0, 0), new Vector2(1, 0), new Vector2(2, 0));
        float[] verts = TrailRibbon.build(path, 2f, 4f);
        int stride = TrailRibbon.FLOATS_PER_VERTEX;
        // last point's left vertex y magnitude should be the end half-width (2)
        float lastLeftY = verts[(2 * 2) * stride + 1];
        assertEquals(2f, Math.abs(lastLeftY), 1e-4f);
    }

    @Test
    public void degeneratePathsReturnEmpty() {
        assertEquals(0, TrailRibbon.build(null, 1f, 1f).length);
        assertEquals(0, TrailRibbon.build(Arrays.asList(new Vector2(0, 0)), 1f, 1f).length);
    }

    @Test
    public void allValuesFinite() {
        List<Vector2> path = Arrays.asList(new Vector2(0, 0), new Vector2(3, 4), new Vector2(6, 0));
        for (float f : TrailRibbon.build(path, 1f, 0f)) {
            assertTrue(Float.isFinite(f));
        }
    }
}
