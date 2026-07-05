// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx.components;

import org.terasology.gestalt.entitysystem.component.Component;

// Attach to anything that should leave a tapering ribbon trail (ship engines,
// projectiles, comet bodies). A trail system samples the owner's position each
// tick into a ring buffer; VectorEffectsDrawer turns that buffer into the
// ribbon. Width tapers from widthHead (at the entity) to widthTail (oldest
// point). textureName is an engine texture URN such as "core:railTrail".
public class Trail implements Component<Trail> {

    public int maxPoints = 24;
    public float widthHead = 0.4f;
    public float widthTail = 0f;
    public String textureName = "engine:fx";
    public String tint = "white";

    @Override
    public void copyFrom(Trail other) {
        this.maxPoints = other.maxPoints;
        this.widthHead = other.widthHead;
        this.widthTail = other.widthTail;
        this.textureName = other.textureName;
        this.tint = other.tint;
    }
}
