// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity.systems;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.UpdateAwareSystem;
import org.destinationsol.game.asteroid.Asteroid;
import org.destinationsol.game.attributes.RegisterUpdateSystem;
import org.destinationsol.game.planet.Planet;
import org.destinationsol.game.planet.PlanetManager;
import org.destinationsol.game.planet.SolarSystem;
import org.destinationsol.modules.gravity.AstroGravity;
import org.destinationsol.modules.gravity.GravityWell;
import org.destinationsol.modules.gravity.components.GravityAffected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.entity.EntityRef;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

// Per-tick softened N-body gravity, ported from the Step event of Astro
// Gravity's o_asteroid, which called:
//
//   math_astrogravity(p_space_matter, screen.sec * .012, 2000)
//
// Here the wells (the "p_space_matter" of Destination Sol) are the planets and
// the suns of the current solar systems, plus anything a module tags with a
// GravitySource component. Affected bodies are asteroids and anything carrying
// a GravityAffected component. Everything runs against the live Box2D world,
// so it works for both the legacy SolObject asteroid path and the newer ECS
// asteroid entities without caring which is which.
//
// Wiring: the @RegisterUpdateSystem annotation lets the engine discover this
// automatically. SolGame scans the module environment for annotated
// UpdateAwareSystems, instantiates each with its no-arg constructor, and
// dependency-injects it - so no engine edit is required. priority 1 runs it
// just after the priority-0 default systems (PlanetManager, ObjectManager), so
// planet positions and bodies are current when gravity is applied.
@RegisterUpdateSystem(priority = 1)
public class AstroGravitySystem implements UpdateAwareSystem {

    private static final Logger logger = LoggerFactory.getLogger(AstroGravitySystem.class);

    // Emit a debug line at most this often (game seconds). Set this class to
    // DEBUG level to see well/affected counts in destinationsol.log.
    private static final float LOG_INTERVAL = 2f;

    // Global force constant. The GML used screen.sec * .012 as the per-frame
    // multiplier; screen.sec is the delta time, which we factor out and pass
    // as timeStep, leaving .012 as the constant.
    private static final float FORCE_CONSTANT = 0.012f;

    // Planet "gravitational radius" is taken from its full (atmosphere) height.
    // Suns use the solar-system radius. These factors let you tune pull
    // strength without touching the force law.
    private static final float PLANET_RADIUS_FACTOR = 1.0f;
    private static final float SUN_RADIUS_FACTOR = 1.5f;

    // A well stops acting past cutoff = radius * CUTOFF_FACTOR. This is the
    // "maximum distance" argument from math_astrogravity and is what keeps the
    // cost linear in (bodies * nearby wells) rather than (bodies * all wells).
    private static final float CUTOFF_FACTOR = 8f;

    // Reused scratch so the per-frame loop allocates nothing.
    private final Array<Body> bodies = new Array<>(false, 256);
    private final List<GravityWell> wells = new ArrayList<>();
    private int wellCount;
    private final Vector2 deltaVelocity = new Vector2();

    // Debug accounting, reported on the throttled log line.
    private float logTimer;
    private int affectedCount;
    private float maxImpulse;

    @Inject
    public AstroGravitySystem() {
    }

    @Override
    public void update(SolGame game, float timeStep) {
        World world = game.getObjectManager().getWorld();
        if (world == null) {
            return;
        }

        buildWells(game.getPlanetManager());
        if (wellCount == 0) {
            return;
        }

        bodies.clear();
        world.getBodies(bodies);

        affectedCount = 0;
        maxImpulse = 0;
        for (Body body : bodies) {
            if (body.getType() != BodyDef.BodyType.DynamicBody) {
                continue;
            }
            if (!isAffected(body.getUserData())) {
                continue;
            }
            affectedCount++;
            float impulse = applyWellsToBody(body, wells, wellCount, FORCE_CONSTANT, timeStep, deltaVelocity);
            if (impulse > maxImpulse) {
                maxImpulse = impulse;
            }
        }

        logProgress(timeStep);
    }

    // Applies every well to a single body as one combined impulse and returns
    // the impulse magnitude (0 if the body was out of range of all wells).
    // Impulse = mass * deltaVelocity, so the velocity change is mass-independent,
    // matching the GML where m1 cancels out of F = m1 * a.
    //
    // Extracted from the update loop so it can be exercised against a real Box2D
    // body in an integration test without standing up a whole SolGame.
    public static float applyWellsToBody(Body body, List<GravityWell> wells, int wellCount,
                                         float forceConstant, float timeStep, Vector2 scratch) {
        Vector2 center = body.getWorldCenter();
        scratch.set(0, 0);
        for (int i = 0; i < wellCount; i++) {
            GravityWell well = wells.get(i);
            AstroGravity.addPull(scratch, center, well.position,
                    forceConstant, well.radius, well.cutoff, timeStep);
        }
        if (scratch.x == 0 && scratch.y == 0) {
            return 0f;
        }
        scratch.scl(body.getMass());
        body.applyLinearImpulse(scratch, center, true);
        return scratch.len();
    }

    // Throttled heartbeat so you can confirm the system is alive and sane in
    // destinationsol.log without spamming a line every frame. At DEBUG it shows
    // the well/affected counts; if it ever sees a non-finite impulse it shouts
    // at WARN, since that is the classic sign of a gravity blow-up.
    private void logProgress(float timeStep) {
        logTimer += timeStep;
        if (logTimer < LOG_INTERVAL) {
            return;
        }
        logTimer = 0;
        if (!Float.isFinite(maxImpulse)) {
            logger.warn("AstroGravity produced a non-finite impulse - check FORCE_CONSTANT/cutoff tuning");
        } else if (logger.isDebugEnabled()) {
            logger.debug("AstroGravity: wells={}, affectedBodies={}, maxImpulse={}",
                    wellCount, affectedCount, maxImpulse);
        }
    }

    private boolean isAffected(Object userData) {
        if (userData instanceof Asteroid) {
            return true;
        }
        if (userData instanceof EntityRef) {
            return ((EntityRef) userData).hasComponent(GravityAffected.class);
        }
        return false;
    }

    // Rebuild the well list in place. Grows the pool only when a system has
    // more wells than ever seen before.
    private void buildWells(PlanetManager planetManager) {
        wellCount = 0;
        if (planetManager == null) {
            return;
        }
        for (Planet planet : planetManager.getPlanets()) {
            float radius = planet.getFullHeight() * PLANET_RADIUS_FACTOR;
            addWell(planet.getPosition().x, planet.getPosition().y, radius);
        }
        for (SolarSystem system : planetManager.getSystems()) {
            addWell(system.getPosition().x, system.getPosition().y, sunRadius(system));
        }
    }

    private float sunRadius(SolarSystem system) {
        // The sun sits at the system center; give it a strong pull scaled to
        // the system size so deep-system asteroids still feel it.
        return system.getRadius() * 0.02f * SUN_RADIUS_FACTOR;
    }

    private void addWell(float x, float y, float radius) {
        GravityWell well;
        if (wellCount < wells.size()) {
            well = wells.get(wellCount);
        } else {
            well = new GravityWell();
            wells.add(well);
        }
        well.set(x, y, radius, radius * CUTOFF_FACTOR);
        wellCount++;
    }
}
