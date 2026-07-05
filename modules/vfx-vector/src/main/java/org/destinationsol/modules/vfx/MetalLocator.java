// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.math.Vector2;

// The "find the nearest piece of metal" hook from draw_lightning_forked.gml,
// where half of all forks chase the closest metallic body via collision_circle.
// Implementations look up the nearest body whose Material isMetallic() within
// the given radius and return its position, or null if none. Kept as an
// interface so the lightning generator stays pure and unit-testable; the live
// implementation queries the game's bodies/entities.
@FunctionalInterface
public interface MetalLocator {

    // Returns the position of the nearest metallic target within radius of
    // (x, y), or null if there is none.
    Vector2 nearestMetal(float x, float y, float radius);

    // A locator that never finds metal - the default, and handy for tests.
    MetalLocator NONE = (x, y, radius) -> null;
}
