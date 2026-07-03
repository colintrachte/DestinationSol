// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.game.asteroid.procedural;

// The computed geometry of one procedural rock, ported from Astro Gravity's
// path_random_circle + triangle-fan fill (draw_space_rock).
//
//  - perimeter: the closed outline, x,y pairs, in local space centered on (0,0).
//  - vertices:  perimeter preceded by the center vertex at index 0, x,y pairs.
//  - uvs:       one u,v per vertex, mapping a square texture across the rock.
//  - indices:   triangle-fan indices (center, i, i+1) - three per perimeter edge.
//
// The same perimeter doubles as the collision outline (see RockCollision), so
// the physics shape and the drawn shape match exactly.
public final class RockMesh {

    public final float[] perimeter;
    public final float[] vertices;
    public final float[] uvs;
    public final short[] indices;
    public final int perimeterCount;
    public final float radius;

    RockMesh(float[] perimeter, float[] vertices, float[] uvs, short[] indices, int perimeterCount, float radius) {
        this.perimeter = perimeter;
        this.vertices = vertices;
        this.uvs = uvs;
        this.indices = indices;
        this.perimeterCount = perimeterCount;
        this.radius = radius;
    }

    public int triangleCount() {
        return indices.length / 3;
    }
}
