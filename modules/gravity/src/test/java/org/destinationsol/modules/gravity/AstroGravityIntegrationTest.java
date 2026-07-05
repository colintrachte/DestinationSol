// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import org.destinationsol.modules.gravity.systems.AstroGravitySystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Integration test: exercises AstroGravitySystem.applyWellsToBody against a REAL
// Box2D world and body (not just the math). It proves a dynamic body actually
// gains velocity toward a well, that mass cancels (two different masses get the
// same velocity change), and that a body past the cutoff is untouched.
//
// Requires the desktop Box2D natives, which the gravity module pulls in as test
// dependencies; Box2D.init() loads them.
public class AstroGravityIntegrationTest {

    private static final float K = 0.012f;
    private static final float DT = 1f / 60f;

    @BeforeAll
    public static void loadNatives() {
        Box2D.init();
    }

    private World newWorld() {
        // No ambient gravity - the only force is what the system applies.
        return new World(new Vector2(0, 0), true);
    }

    private Body newDynamicBody(World world, float x, float y, float density) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);
        Body body = world.createBody(bd);
        CircleShape shape = new CircleShape();
        shape.setRadius(1f);
        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = density;
        body.createFixture(fd);
        shape.dispose();
        return body;
    }

    private List<GravityWell> wellAt(float x, float y, float radius, float cutoff) {
        GravityWell well = new GravityWell();
        well.set(x, y, radius, cutoff);
        List<GravityWell> wells = new ArrayList<>();
        wells.add(well);
        return wells;
    }

    @Test
    public void bodyAcceleratesTowardWell() {
        World world = newWorld();
        Body body = newDynamicBody(world, 100, 0, 1f);
        List<GravityWell> wells = wellAt(0, 0, 50f, 400f);

        float impulse = AstroGravitySystem.applyWellsToBody(body, wells, 1, K, DT, new Vector2());
        world.step(DT, 6, 2);

        Vector2 v = body.getLinearVelocity();
        assertTrue(impulse > 0, "an in-range well should impart a non-zero impulse");
        assertTrue(v.x < 0, "a well at the origin should pull a body at +x in the -x direction");
        assertEquals(0f, v.y, 1e-4f, "no sideways drift for a colinear well");
        world.dispose();
    }

    @Test
    public void velocityChangeIsMassIndependent() {
        World world = newWorld();
        Body light = newDynamicBody(world, 100, 0, 1f);
        Body heavy = newDynamicBody(world, 100, 0, 25f);
        List<GravityWell> wells = wellAt(0, 0, 50f, 400f);

        AstroGravitySystem.applyWellsToBody(light, wells, 1, K, DT, new Vector2());
        AstroGravitySystem.applyWellsToBody(heavy, wells, 1, K, DT, new Vector2());

        // Different masses, same well: the resulting velocity change should match,
        // because impulse = mass * deltaVelocity.
        assertEquals(light.getLinearVelocity().x, heavy.getLinearVelocity().x, 1e-4f);
        assertTrue(heavy.getMass() > light.getMass() * 5, "sanity: the heavy body really is heavier");
        world.dispose();
    }

    @Test
    public void bodyBeyondCutoffIsUntouched() {
        World world = newWorld();
        Body body = newDynamicBody(world, 1000, 0, 1f);
        List<GravityWell> wells = wellAt(0, 0, 50f, 400f);

        float impulse = AstroGravitySystem.applyWellsToBody(body, wells, 1, K, DT, new Vector2());

        assertEquals(0f, impulse, "a body past the cutoff gets no impulse");
        assertEquals(0f, body.getLinearVelocity().len(), 1e-6f, "and therefore does not move");
        world.dispose();
    }
}
