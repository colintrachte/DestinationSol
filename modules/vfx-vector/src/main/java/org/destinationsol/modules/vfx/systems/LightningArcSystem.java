// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.UpdateAwareSystem;
import org.destinationsol.game.asteroid.Asteroid;
import org.destinationsol.game.attributes.RegisterUpdateSystem;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;
import org.destinationsol.game.drawables.DrawableObject;
import org.destinationsol.modules.vfx.LightningDrawable;
import org.destinationsol.modules.vfx.MetalLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// A deliberately loud, visible demo so you can SEE the vfx module working: every
// fraction of a second it makes the nearest on-screen asteroid crackle a
// lightning arc to its nearest neighbour, with forks that chase metal. It is a
// self-contained module system - @RegisterUpdateSystem means the engine wires it
// automatically, and it spawns its effect as a normal DrawableObject, so there
// is no engine edit. Delete this class (or set seekMetalDemo off) once you have
// confirmed the effect and want lightning driven by real gameplay instead.
@RegisterUpdateSystem(priority = 2)
public class LightningArcSystem implements UpdateAwareSystem {

    private static final Logger logger = LoggerFactory.getLogger(LightningArcSystem.class);

    private static final float SPAWN_INTERVAL = 0.15f;
    private static final float SCAN_RANGE = 12f;
    private static final float BOLT_LIFE = 0.18f;
    private static final float BOLT_WIDTH = 0.06f;
    private static final float SEGMENT_LENGTH = 0.35f;
    private static final float ANGLE_TOLERANCE = 35f;
    private static final float LOG_INTERVAL = 2f;
    // Seconds for charge to swell from 0 to 1 and reset - the asteroids visibly
    // build up and discharge, so the arcs pulse brighter over time.
    private static final float CHARGE_PERIOD = 3.5f;

    private final Color color = new Color(0.65f, 0.8f, 1f, 1f);
    private final Random rng = new Random();
    private final List<Asteroid> nearby = new ArrayList<>();

    private float spawnTimer;
    private float logTimer;
    private float charge;
    private int boltsSpawned;

    @Inject
    public LightningArcSystem() {
    }

    @Override
    public void update(SolGame game, float timeStep) {
        spawnTimer -= timeStep;
        logTimer += timeStep;
        if (spawnTimer > 0) {
            return;
        }
        spawnTimer = SPAWN_INTERVAL;

        // Advance the charge oscillator (0..1, wrapping) so arc intensity pulses.
        charge += SPAWN_INTERVAL / CHARGE_PERIOD;
        if (charge > 1f) {
            charge -= 1f;
        }

        Vector2 cam = game.getCam().getPosition();
        collectNearbyAsteroids(game, cam);
        if (nearby.size() >= 2) {
            Asteroid source = nearestTo(cam, null);
            Asteroid target = nearestTo(source.getPosition(), source);
            if (source != target && target != null) {
                spawnArc(game, source.getPosition(), target.getPosition());
                boltsSpawned++;
            }
        }

        if (logTimer >= LOG_INTERVAL) {
            logTimer = 0;
            logger.debug("vfx demo: nearbyAsteroids={}, boltsSpawnedTotal={}", nearby.size(), boltsSpawned);
        }
    }

    private void collectNearbyAsteroids(SolGame game, Vector2 cam) {
        nearby.clear();
        for (SolObject obj : game.getObjectManager().getObjects()) {
            if (obj instanceof Asteroid && obj.getPosition().dst(cam) <= SCAN_RANGE) {
                nearby.add((Asteroid) obj);
            }
        }
    }

    private Asteroid nearestTo(Vector2 point, Asteroid exclude) {
        Asteroid best = null;
        float bestDist = Float.MAX_VALUE;
        for (Asteroid a : nearby) {
            if (a == exclude) {
                continue;
            }
            float d = a.getPosition().dst(point);
            if (d < bestDist) {
                bestDist = d;
                best = a;
            }
        }
        return best;
    }

    private void spawnArc(SolGame game, Vector2 a, Vector2 b) {
        Vector2 mid = new Vector2(a).add(b).scl(0.5f);

        // Snapshot the nearby asteroid positions so forks can chase the nearest
        // "metal" deterministically, independent of the live object list.
        List<Vector2> metalPoints = new ArrayList<>(nearby.size());
        for (Asteroid asteroid : nearby) {
            metalPoints.add(new Vector2(asteroid.getPosition()));
        }
        MetalLocator locator = (x, y, radius) -> {
            Vector2 best = null;
            float bestDist = radius;
            for (Vector2 p : metalPoints) {
                float d = p.dst(x, y);
                if (d > 0.01f && d < bestDist) {
                    bestDist = d;
                    best = p;
                }
            }
            return best;
        };

        // Intensity swells with charge (0.5 dim .. 1.5 bright) so the crackle pulses.
        float intensity = 0.5f + charge;
        LightningDrawable bolt = new LightningDrawable(mid, new Vector2(a), new Vector2(b), color,
                BOLT_WIDTH, BOLT_LIFE, SEGMENT_LENGTH, ANGLE_TOLERANCE, 0f, 3, locator, rng.nextLong(),
                intensity, DrawableLevel.PART_FG_1, true);

        List<Drawable> drawables = new ArrayList<>(1);
        drawables.add(bolt);
        DrawableObject host = new DrawableObject(drawables, mid, new Vector2(), null, true, false);
        game.getObjectManager().addObjDelayed(host);
    }
}
