// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity;

import com.badlogic.gdx.math.Vector2;

// A point that attracts gravity-affected bodies. Built each tick from planets,
// suns, and any entity carrying a GravitySource component, then consumed by
// the AstroGravitySystem. Kept deliberately tiny so a few hundred of them can
// be rebuilt every frame without allocation pressure (see the pooled list in
// the system).
public final class GravityWell {

    public final Vector2 position = new Vector2();
    public float radius;
    public float cutoff;

    public void set(float x, float y, float radius, float cutoff) {
        this.position.set(x, y);
        this.radius = radius;
        this.cutoff = cutoff;
    }
}
