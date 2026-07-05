// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.game.Hero;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.UpdateAwareSystem;
import org.destinationsol.game.asteroid.Asteroid;
import org.destinationsol.game.attributes.RegisterUpdateSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

// One-shot demo: once the hero exists, drop a ring of low-velocity asteroids
// around the ship so you can immediately SEE both new systems at work - the
// AstroGravitySystem pulling the rocks toward the nearest planet/sun, and the
// vfx LightningArcSystem crackling arcs between them. It spawns once and then
// does nothing. Delete this class to remove the demo; nothing else depends on it.
//
// Placements are rejected and retried if they'd overlap an already-placed rock:
// spawning them touching triggers simultaneous Box2D contacts on the very first
// world.step(), which can chain into Asteroid.maybeSplit()/spawnChip() spawning
// more (also overlapping) fragments in the same tiny area - a collision cascade
// that stalled the whole frame instead of a clean demo.
@RegisterUpdateSystem(priority = 1)
public class DemoAsteroidSpawner implements UpdateAwareSystem {

    private static final Logger logger = LoggerFactory.getLogger(DemoAsteroidSpawner.class);

    private static final int COUNT = 16;
    private static final float MIN_RING = 4f;
    private static final float MAX_RING = 9f;
    private static final float MIN_SIZE = 0.3f;
    private static final float MAX_SIZE = 0.9f;
    private static final float MIN_SEPARATION = 0.3f;
    private static final int MAX_PLACEMENT_ATTEMPTS = 20;

    private boolean done;

    @Inject
    public DemoAsteroidSpawner() {
    }

    @Override
    public void update(SolGame game, float timeStep) {
        if (done) {
            return;
        }
        Hero hero = game.getHero();
        if (hero == null || hero.isTranscendent() || hero.isDead()) {
            return;
        }

        Vector2 heroPos = hero.getPosition();
        List<Vector2> placedPositions = new ArrayList<>(COUNT);
        List<Float> placedSizes = new ArrayList<>(COUNT);
        int spawned = 0;
        for (int i = 0; i < COUNT; i++) {
            float size = MathUtils.random(MIN_SIZE, MAX_SIZE);
            Vector2 pos = findNonOverlappingSpot(heroPos, size, placedPositions, placedSizes, i);
            if (pos == null) {
                continue;
            }
            placedPositions.add(pos);
            placedSizes.add(size);
            // Zero starting velocity so gravity's pull is obvious as they begin to drift.
            Asteroid asteroid = game.getAsteroidBuilder().buildNew(game, pos, new Vector2(), size, null);
            game.getObjectManager().addObjDelayed(asteroid);
            spawned++;
        }

        done = true;
        logger.info("Demo: spawned {} asteroids around the hero to showcase gravity + lightning vfx", spawned);
    }

    // Retries at random ring/angle offsets until a spot clears every already-placed
    // rock by at least the sum of their radii plus a safety margin, or gives up.
    private Vector2 findNonOverlappingSpot(Vector2 heroPos, float size, List<Vector2> placedPositions,
                                            List<Float> placedSizes, int slot) {
        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            float angle = slot * (360f / COUNT) + MathUtils.random(-8f, 8f);
            float ring = MathUtils.random(MIN_RING, MAX_RING);
            Vector2 pos = new Vector2(heroPos).add(ring * MathUtils.cosDeg(angle), ring * MathUtils.sinDeg(angle));

            boolean overlaps = false;
            for (int j = 0; j < placedPositions.size(); j++) {
                float requiredDist = size / 2 + placedSizes.get(j) / 2 + MIN_SEPARATION;
                if (pos.dst2(placedPositions.get(j)) < requiredDist * requiredDist) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                return pos;
            }
        }
        return null;
    }
}
