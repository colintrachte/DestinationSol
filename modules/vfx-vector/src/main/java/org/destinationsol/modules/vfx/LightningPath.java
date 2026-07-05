// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.math.Vector2;

import java.util.Random;

// Pure translation of draw_lightning_forked.gml. Walks from a start point
// toward a target in fixed-length segments, jittering each segment's heading by
// a tolerance plus a steady curl, fading alpha along the length, and
// occasionally spawning a fork. Half of all forks chase the nearest metallic
// body (via MetalLocator), the rest peel off at a natural angle - exactly the
// behavior of the original.
//
// Everything is deterministic given the same Random, so it is unit-testable and
// can be reproduced from a seed for replays.
public final class LightningPath {

    private LightningPath() {
    }

    // segmentLength: pixels per zig-zag step.
    // angleToleranceDeg: max degrees a segment may deviate from straight.
    // curlDeg: constant bias added each step (a gentle overall curve).
    // maxForkDepth: recursion guard for forks-of-forks.
    public static LightningGeometry generate(float x1, float y1, float x2, float y2,
                                             float segmentLength, float angleToleranceDeg,
                                             float curlDeg, MetalLocator locator,
                                             Random rng, int maxForkDepth) {
        LightningGeometry geo = new LightningGeometry();
        if (segmentLength <= 0) {
            geo.add(x1, y1, 1f);
            geo.add(x2, y2, 1f);
            return geo;
        }

        float xx = x1;
        float yy = y1;
        int i = 0;
        float alpha = 1f;
        float pathLength = dist(xx, yy, x2, y2);

        boolean forked = false;
        float forkStartX = 0;
        float forkStartY = 0;
        float forkEndX = 0;
        float forkEndY = 0;

        while (pathLength > segmentLength) {
            float xprev = xx;
            float yprev = yy;

            float dir = pointDirection(xprev, yprev, x2, y2)
                    + clamp(tolerance(angleToleranceDeg, rng) + curlDeg, -89f, 89f);
            xx += lengthDirX(segmentLength, dir);
            yy += lengthDirY(segmentLength, dir);
            pathLength = dist(xx, yy, x2, y2);

            // fades the lightning with distance, same curve as the GML
            alpha = 0.02f + (9.8f / (i + 10f));
            geo.add(xprev, yprev, alpha);

            // one fork per bolt; forks are less likely when the bolt is faint
            if (!forked && maxForkDepth > 0 && diceTrue(25f / alpha, rng)) {
                forked = true;
                forkStartX = xx;
                forkStartY = yy;
                float forkDist = segmentLength * 3f + rng.nextFloat() * pathLength;
                Vector2 metal = locator == null ? null : locator.nearestMetal(xx, yy, forkDist);
                if (metal != null && rng.nextBoolean()) {
                    forkEndX = metal.x;
                    forkEndY = metal.y;
                } else {
                    float forkDir = dir + tolerance(60f, rng);
                    forkEndX = xx + lengthDirX(forkDist, forkDir);
                    forkEndY = yy + lengthDirY(forkDist, forkDir);
                }
            }
            i++;
        }

        // last computed point, then snap to the exact target
        geo.add(xx, yy, alpha);
        geo.add(x2, y2, alpha);

        if (forked) {
            geo.forks.add(generate(forkStartX, forkStartY, forkEndX, forkEndY,
                    segmentLength, angleToleranceDeg, curlDeg, locator, rng, maxForkDepth - 1));
        }
        return geo;
    }

    // GameMaker helpers, kept faithful (degrees, y-down) so the look matches.

    private static float tolerance(float t, Random rng) {
        return (rng.nextFloat() * 2f - 1f) * t;
    }

    // random_dice(n): true with probability 1/n.
    private static boolean diceTrue(float n, Random rng) {
        return rng.nextFloat() * n < 1f;
    }

    private static float dist(float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float pointDirection(float ax, float ay, float bx, float by) {
        // GameMaker measures angles counter-clockwise with y pointing down.
        return (float) Math.toDegrees(Math.atan2(-(by - ay), bx - ax));
    }

    private static float lengthDirX(float len, float dirDeg) {
        return len * (float) Math.cos(Math.toRadians(dirDeg));
    }

    private static float lengthDirY(float len, float dirDeg) {
        return -len * (float) Math.sin(Math.toRadians(dirDeg));
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
