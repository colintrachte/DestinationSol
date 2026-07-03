// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.game.asteroid.procedural;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

// Builds exact collision for a procedural rock by attaching one Box2D triangle
// fixture per fan triangle. Because each triangle is convex (and small), the
// union reproduces the rock's concave silhouette precisely - the drawn shape and
// the physical shape are literally the same geometry.
//
// Note: this creates triangleCount() fixtures, so keep the perimeter point count
// modest (roughly 8-16) for asteroids. For very small rubble a single circle
// fixture is cheaper; switch on size at the call site.
public final class RockCollision {

    private RockCollision() {
    }

    public static void attachTriangleFixtures(Body body, RockMesh mesh, float scale, float density, float friction) {
        float[] tri = new float[6];
        PolygonShape shape = new PolygonShape();
        for (int t = 0; t < mesh.triangleCount(); t++) {
            int i0 = mesh.indices[t * 3] * 2;
            int i1 = mesh.indices[t * 3 + 1] * 2;
            int i2 = mesh.indices[t * 3 + 2] * 2;

            tri[0] = mesh.vertices[i0] * scale;
            tri[1] = mesh.vertices[i0 + 1] * scale;
            tri[2] = mesh.vertices[i1] * scale;
            tri[3] = mesh.vertices[i1 + 1] * scale;
            tri[4] = mesh.vertices[i2] * scale;
            tri[5] = mesh.vertices[i2 + 1] * scale;

            shape.set(tri);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            // Density is applied after every fixture exists (see below), not here -
            // a nonzero density would make Box2D recompute the body's mass data
            // immediately inside this createFixture() call, against whatever lopsided
            // subset of triangles has been attached so far.
            fd.density = 0f;
            fd.friction = friction;
            body.createFixture(fd);
        }
        shape.dispose();

        // Box2D recomputes mass data (mass, center, rotational inertia) automatically
        // whenever a fixture with density > 0 is created. Each fan triangle has one
        // vertex pinned at the body origin, so a single early triangle's centroid sits
        // far off-center relative to its own tiny size - recomputing mass data against
        // that kind of lopsided, incomplete fixture set can trip Box2D's internal
        // "rotational inertia > 0" assertion via floating-point cancellation. Setting
        // density to 0 above and applying it here, once every triangle is attached,
        // means Box2D only ever computes mass data against the complete, balanced
        // shape.
        for (Fixture fixture : body.getFixtureList()) {
            fixture.setDensity(density);
        }
        body.resetMassData();
    }
}
