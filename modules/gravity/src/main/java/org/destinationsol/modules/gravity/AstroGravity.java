// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity;

import com.badlogic.gdx.math.Vector2;

// Pure, side-effect-free translation of Astro Gravity's math_astrogravity.gml.
//
// The original GML, with the singularity-free softening that makes the whole
// thing stable, was:
//
//   magnitude = point_distance(0, 0, sprite_width, sprite_height)
//   motion_add(point_direction(x, y, other.x, other.y),
//              force * power(magnitude, 3) / (1 + sqr(dist + magnitude)))
//
// Naive gravity uses G*m/dist^2, which blows up to infinity as dist -> 0:
// that is what makes hand-rolled asteroid gravity jitter, fling bodies off
// at absurd speeds, or produce NaN positions. The fix here is twofold:
//   1. the force is sourced from a visual radius R, not true mass, so wells
//      stay a sane strength relative to their on-screen size;
//   2. the denominator is (1 + (dist + R)^2) instead of dist^2, so it is
//      finite everywhere and smoothly tapers to zero pull at the center.
//
// The returned value is an acceleration magnitude (per unit time). Callers
// multiply by the time step to integrate it.
public final class AstroGravity {

    private AstroGravity() {
    }

    // Acceleration magnitude felt by a body at distance dist from a well of
    // gravitational radius R, scaled by the global force constant k.
    // Returns 0 when the body is outside the well's cutoff distance.
    public static float accelerationMagnitude(float k, float radius, float dist, float cutoff) {
        if (dist >= cutoff) {
            return 0f;
        }
        float r = radius;
        float soft = dist + r;
        return k * (r * r * r) / (1f + soft * soft);
    }

    // Convenience: accumulate the velocity change (acceleration * dt) that a
    // well at sourcePos imparts on a body at bodyPos into out. The direction
    // points from the body toward the source (attraction).
    public static void addPull(Vector2 out, Vector2 bodyPos, Vector2 sourcePos,
                               float k, float radius, float cutoff, float dt) {
        float dx = sourcePos.x - bodyPos.x;
        float dy = sourcePos.y - bodyPos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float accel = accelerationMagnitude(k, radius, dist, cutoff);
        if (accel == 0f || dist < 1e-6f) {
            return;
        }
        float scale = accel * dt / dist;
        out.x += dx * scale;
        out.y += dy * scale;
    }
}
