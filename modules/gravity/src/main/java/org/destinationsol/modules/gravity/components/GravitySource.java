// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity.components;

import org.terasology.gestalt.entitysystem.component.Component;

// Turns an entity into a gravity well. The pull is sourced from a visual
// "radius" rather than true mass, exactly as in Astro Gravity's
// math_astrogravity - this keeps planets and suns reasonably close together
// while still producing believable orbits. cutoff caps the distance at which
// this source has any effect, which is what keeps the whole-galaxy simulation
// affordable.
public class GravitySource implements Component<GravitySource> {

    // Effective gravitational radius (the "magnitude" R in the force law).
    public float radius = 1f;

    // Beyond this distance the source is ignored. 0 means "use the global default".
    public float cutoff;

    @Override
    public void copyFrom(GravitySource other) {
        this.radius = other.radius;
        this.cutoff = other.cutoff;
    }
}
