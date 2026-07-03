// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.game.asteroid.procedural;

import java.util.Random;

// Pure translation of the asteroid-shape step from Astro Gravity:
//
//   path_random_circle(path, 0, 0, radius*(1-variance), radius, ...)
//
// builds a closed ring of points whose radius wanders between an inner radius
// radius*(1-variance) and the outer radius. We then triangulate that ring as a
// fan from the center so it is a solid mesh, and assign UVs that stretch a
// square texture across the rock.
//
// Deterministic given a Random, so the same seed yields the same rock - handy
// for tests and for regenerating a body from a saved seed.
public final class AsteroidShape {

    private AsteroidShape() {
    }

    public static RockMesh generate(float radius, float variance, int points, Random rng) {
        if (points < 3) {
            points = 3;
        }
        if (variance < 0) {
            variance = 0;
        }
        if (variance > 0.95f) {
            variance = 0.95f;
        }

        int n = points;
        float[] perimeter = new float[n * 2];
        float inner = radius * (1f - variance);
        float spread = radius - inner;

        for (int k = 0; k < n; k++) {
            double ang = (k / (double) n) * Math.PI * 2.0;
            float rr = inner + rng.nextFloat() * spread;
            perimeter[k * 2] = (float) (rr * Math.cos(ang));
            perimeter[k * 2 + 1] = (float) (rr * Math.sin(ang));
        }

        // vertices: center at index 0, then the perimeter.
        float[] vertices = new float[(n + 1) * 2];
        vertices[0] = 0f;
        vertices[1] = 0f;
        System.arraycopy(perimeter, 0, vertices, 2, perimeter.length);

        // UVs: map local space [-radius, radius] -> [0, 1].
        float[] uvs = new float[(n + 1) * 2];
        for (int v = 0; v < n + 1; v++) {
            uvs[v * 2] = clamp01(vertices[v * 2] / (2f * radius) + 0.5f);
            uvs[v * 2 + 1] = clamp01(vertices[v * 2 + 1] / (2f * radius) + 0.5f);
        }

        // Fan: triangle (center, k, k+1) for each edge, wrapping the last to the first.
        short[] indices = new short[n * 3];
        for (int k = 0; k < n; k++) {
            int next = (k + 1) % n;
            indices[k * 3] = 0;
            indices[k * 3 + 1] = (short) (1 + k);
            indices[k * 3 + 2] = (short) (1 + next);
        }

        return new RockMesh(perimeter, vertices, uvs, indices, n, radius);
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
