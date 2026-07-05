// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

// Pure translation of draw_trail.gml. Given an ordered polyline (a weapon
// swipe, comet tail, engine wash, or any path), it produces a triangle-strip
// of two vertices per point, offset perpendicular to the path, with the width
// tapering linearly from widthStart at the first point to widthEnd at the last.
// UVs run u = 0..1 along the path and v = 0 / 1 across it, so a single stretched
// texture maps cleanly along the ribbon.
//
// Output is an interleaved float[] of {x, y, u, v} per vertex, ready to feed a
// triangle strip. Two vertices are emitted per input point (left then right).
public final class TrailRibbon {

    public static final int FLOATS_PER_VERTEX = 4;

    private TrailRibbon() {
    }

    public static float[] build(List<Vector2> path, float widthStart, float widthEnd) {
        int n = path == null ? 0 : path.size();
        if (n < 2) {
            return new float[0];
        }

        float halfStart = widthStart * 0.5f;
        float halfEnd = widthEnd * 0.5f;
        float[] verts = new float[n * 2 * FLOATS_PER_VERTEX];

        int v = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            float half = halfStart + (halfEnd - halfStart) * t;

            // Perpendicular to the local path direction.
            Vector2 a = i == 0 ? path.get(0) : path.get(i - 1);
            Vector2 b = i == 0 ? path.get(1) : path.get(i);
            float dir = pointDirection(a.x, a.y, b.x, b.y) + 90f;
            float wx = lengthDirX(half, dir);
            float wy = lengthDirY(half, dir);

            Vector2 p = path.get(i);
            // left vertex (v = 0)
            verts[v++] = p.x + wx;
            verts[v++] = p.y + wy;
            verts[v++] = t;
            verts[v++] = 0f;
            // right vertex (v = 1)
            verts[v++] = p.x - wx;
            verts[v++] = p.y - wy;
            verts[v++] = t;
            verts[v++] = 1f;
        }
        return verts;
    }

    private static float pointDirection(float ax, float ay, float bx, float by) {
        return (float) Math.toDegrees(Math.atan2(-(by - ay), bx - ax));
    }

    private static float lengthDirX(float len, float dirDeg) {
        return len * (float) Math.cos(Math.toRadians(dirDeg));
    }

    private static float lengthDirY(float len, float dirDeg) {
        return -len * (float) Math.sin(Math.toRadians(dirDeg));
    }
}
