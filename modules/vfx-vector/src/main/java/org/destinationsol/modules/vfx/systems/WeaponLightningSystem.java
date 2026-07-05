// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.game.Hero;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.UpdateAwareSystem;
import org.destinationsol.game.asteroid.Asteroid;
import org.destinationsol.game.attributes.RegisterUpdateSystem;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;
import org.destinationsol.game.drawables.DrawableObject;
import org.destinationsol.game.projectile.Projectile;
import org.destinationsol.game.ship.SolShip;
import org.destinationsol.modules.vfx.LightningDrawable;
import org.destinationsol.modules.vfx.MetalLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Gameplay-driven lightning: whenever a new projectile appears near the camera
// (i.e. you fire), it arcs a bolt from the muzzle to the nearest enemy ship or
// asteroid, with forks chasing the nearest metal. This shows the vfx module
// wired to real events rather than a timer. It needs no engine edit - it
// detects fresh Projectile objects by polling the object list each tick.
@RegisterUpdateSystem(priority = 2)
public class WeaponLightningSystem implements UpdateAwareSystem {

    private static final Logger logger = LoggerFactory.getLogger(WeaponLightningSystem.class);

    private static final float NEAR_CAM = 22f;
    private static final float TARGET_RANGE = 16f;
    private static final float FALLBACK_LENGTH = 7f;
    private static final int MAX_BOLTS_PER_TICK = 3;
    private static final float BOLT_LIFE = 0.12f;
    private static final float BOLT_WIDTH = 0.07f;
    private static final float SEGMENT_LENGTH = 0.4f;
    private static final float ANGLE_TOLERANCE = 28f;
    private static final float LOG_INTERVAL = 3f;

    private final Color color = new Color(0.8f, 0.95f, 1f, 1f);
    private final Random rng = new Random();
    private final Set<Projectile> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Projectile> current = new ArrayList<>();

    private float logTimer;
    private int boltsTotal;

    @Inject
    public WeaponLightningSystem() {
    }

    @Override
    public void update(SolGame game, float timeStep) {
        logTimer += timeStep;
        Vector2 cam = game.getCam().getPosition();
        SolShip heroShip = heroShip(game);

        current.clear();
        int spawnedThisTick = 0;
        for (SolObject obj : game.getObjectManager().getObjects()) {
            if (!(obj instanceof Projectile)) {
                continue;
            }
            Projectile proj = (Projectile) obj;
            current.add(proj);
            boolean isNew = !seen.contains(proj);
            if (isNew && spawnedThisTick < MAX_BOLTS_PER_TICK
                    && proj.getPosition().dst(cam) <= NEAR_CAM) {
                spawnBolt(game, proj, heroShip);
                spawnedThisTick++;
            }
        }

        seen.clear();
        seen.addAll(current);

        if (logTimer >= LOG_INTERVAL) {
            logTimer = 0;
            logger.debug("vfx weapon lightning: liveProjectiles={}, boltsTotal={}", current.size(), boltsTotal);
        }
    }

    private void spawnBolt(SolGame game, Projectile proj, SolShip heroShip) {
        Vector2 muzzle = proj.getPosition();

        Vector2 end = new Vector2();
        SolObject target = nearestTarget(game, muzzle, heroShip);
        if (target != null) {
            end.set(target.getPosition());
        } else {
            Vector2 vel = proj.getVelocity();
            if (vel.len2() > 1e-4f) {
                end.set(vel).nor().scl(FALLBACK_LENGTH).add(muzzle);
            } else {
                end.set(muzzle.x, muzzle.y - FALLBACK_LENGTH);
            }
        }

        MetalLocator locator = buildLocator(game, muzzle);
        Vector2 mid = new Vector2(muzzle).add(end).scl(0.5f);
        // Weapon arcs are hot and bright with an impact flare; purely cosmetic
        // flair on your shots (the projectile itself carries the damage).
        LightningDrawable bolt = new LightningDrawable(mid, new Vector2(muzzle), end, color,
                BOLT_WIDTH, BOLT_LIFE, SEGMENT_LENGTH, ANGLE_TOLERANCE, 0f, 3, locator, rng.nextLong(),
                1.2f, DrawableLevel.PROJECTILES, true);

        List<Drawable> drawables = new ArrayList<>(1);
        drawables.add(bolt);
        game.getObjectManager().addObjDelayed(new DrawableObject(drawables, mid, new Vector2(), null, true, false));
        boltsTotal++;
    }

    // Nearest asteroid or non-hero ship within range of the muzzle.
    private SolObject nearestTarget(SolGame game, Vector2 from, SolShip heroShip) {
        SolObject best = null;
        float bestDist = TARGET_RANGE;
        for (SolObject obj : game.getObjectManager().getObjects()) {
            if (obj == heroShip) {
                continue;
            }
            if (!(obj instanceof Asteroid || obj instanceof SolShip)) {
                continue;
            }
            float d = obj.getPosition().dst(from);
            if (d > 0.1f && d < bestDist) {
                bestDist = d;
                best = obj;
            }
        }
        return best;
    }

    // Snapshot nearby metallic targets (asteroids + ships) so forks chase them.
    private MetalLocator buildLocator(SolGame game, Vector2 around) {
        List<Vector2> points = new ArrayList<>();
        for (SolObject obj : game.getObjectManager().getObjects()) {
            if ((obj instanceof Asteroid || obj instanceof SolShip)
                    && obj.getPosition().dst(around) <= TARGET_RANGE) {
                points.add(new Vector2(obj.getPosition()));
            }
        }
        return (x, y, radius) -> {
            Vector2 best = null;
            float bestDist = radius;
            for (Vector2 p : points) {
                float d = p.dst(x, y);
                if (d > 0.01f && d < bestDist) {
                    bestDist = d;
                    best = p;
                }
            }
            return best;
        };
    }

    private SolShip heroShip(SolGame game) {
        Hero hero = game.getHero();
        if (hero == null || hero.isTranscendent() || hero.isDead()) {
            return null;
        }
        return hero.getShipUnchecked();
    }
}
