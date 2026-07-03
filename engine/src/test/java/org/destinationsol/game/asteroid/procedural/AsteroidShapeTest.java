// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.game.asteroid.procedural;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsteroidShapeTest {

    @Test
    public void meshHasFanTopology() {
        RockMesh m = AsteroidShape.generate(10f, 0.4f, 12, new Random(1));
        assertEquals(12, m.perimeterCount);
        assertEquals((12 + 1) * 2, m.vertices.length, "center + perimeter vertices");
        assertEquals(12 * 3, m.indices.length, "one triangle per perimeter edge");
        assertEquals(12, m.triangleCount());
        // index 0 is the center vertex for every triangle
        for (int t = 0; t < m.triangleCount(); t++) {
            assertEquals(0, m.indices[t * 3], "each fan triangle starts at the center");
        }
    }

    @Test
    public void perimeterStaysBetweenInnerAndOuterRadius() {
        float radius = 10f;
        float variance = 0.4f;
        RockMesh m = AsteroidShape.generate(radius, variance, 24, new Random(7));
        float inner = radius * (1f - variance);
        for (int k = 0; k < m.perimeterCount; k++) {
            float x = m.perimeter[k * 2];
            float y = m.perimeter[k * 2 + 1];
            float r = (float) Math.sqrt(x * x + y * y);
            assertTrue(r >= inner - 1e-3f && r <= radius + 1e-3f, "radius out of band: " + r);
        }
    }

    @Test
    public void uvsAreNormalized() {
        RockMesh m = AsteroidShape.generate(5f, 0.5f, 16, new Random(3));
        for (float uv : m.uvs) {
            assertTrue(uv >= 0f && uv <= 1f, "uv out of range: " + uv);
        }
    }

    @Test
    public void enclosesPositiveArea() {
        RockMesh m = AsteroidShape.generate(8f, 0.3f, 20, new Random(11));
        double area = 0;
        int n = m.perimeterCount;
        for (int k = 0; k < n; k++) {
            int next = (k + 1) % n;
            area += m.perimeter[k * 2] * m.perimeter[next * 2 + 1]
                    - m.perimeter[next * 2] * m.perimeter[k * 2 + 1];
        }
        area = Math.abs(area) / 2.0;
        assertTrue(area > 0, "polygon area should be positive");
        assertTrue(area < Math.PI * 8f * 8f * 1.05, "area cannot exceed the bounding circle");
    }

    @Test
    public void deterministicForSeed() {
        RockMesh a = AsteroidShape.generate(10f, 0.4f, 12, new Random(42));
        RockMesh b = AsteroidShape.generate(10f, 0.4f, 12, new Random(42));
        assertEquals(a.perimeter[0], b.perimeter[0], 1e-6f);
        assertEquals(a.perimeter[7], b.perimeter[7], 1e-6f);
    }

    @Test
    public void clampsTooFewPoints() {
        RockMesh m = AsteroidShape.generate(4f, 0.2f, 1, new Random(0));
        assertEquals(3, m.perimeterCount, "fewer than 3 points is clamped to a triangle");
    }

    @Test
    public void allValuesFinite() {
        RockMesh m = AsteroidShape.generate(6f, 0.6f, 18, new Random(5));
        for (float v : m.vertices) {
            assertTrue(Float.isFinite(v));
        }
    }

    // Regression guard for the class of bug that crashed Box2D's SetMassData
    // (m_I > 0.0f assertion): every fan triangle (center, k, k+1) must enclose
    // strictly positive area, or a fixture built from it has zero/degenerate mass.
    @Test
    public void everyFanTriangleHasPositiveArea() {
        for (int seed = 0; seed < 50; seed++) {
            RockMesh m = AsteroidShape.generate(1f, 0.9f, 8, new Random(seed));
            for (int t = 0; t < m.triangleCount(); t++) {
                int i0 = m.indices[t * 3] * 2;
                int i1 = m.indices[t * 3 + 1] * 2;
                int i2 = m.indices[t * 3 + 2] * 2;
                float area = triangleArea(
                        m.vertices[i0], m.vertices[i0 + 1],
                        m.vertices[i1], m.vertices[i1 + 1],
                        m.vertices[i2], m.vertices[i2 + 1]);
                assertTrue(area > 1e-9f, "degenerate fixture triangle for seed " + seed + " tri " + t);
            }
        }
    }

    private static float triangleArea(float ax, float ay, float bx, float by, float cx, float cy) {
        return Math.abs((bx - ax) * (cy - ay) - (cx - ax) * (by - ay)) / 2f;
    }
}
