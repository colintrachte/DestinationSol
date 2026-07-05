// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.astrobodies.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.game.Hero;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.UpdateAwareSystem;
import org.destinationsol.game.asteroid.procedural.AsteroidShape;
import org.destinationsol.game.asteroid.procedural.RockMesh;
import org.destinationsol.game.attributes.RegisterUpdateSystem;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;
import org.destinationsol.game.drawables.DrawableObject;
import org.destinationsol.modules.astrobodies.ProceduralRockDrawable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// One-shot demo: scatters procedurally-shaped rocks far out around the hero as
// faded BACKGROUND landmarks. They live on a parallax layer (FAR_DECO), so they
// scroll slower than the playfield and read clearly as distant scenery rather
// than sitting on top of the real asteroids - and they have no gravity, no
// collision, and a fixed position. Delete this class to remove the demo.
@RegisterUpdateSystem(priority = 1)
public class DemoRockSystem implements UpdateAwareSystem {

    private static final Logger logger = LoggerFactory.getLogger(DemoRockSystem.class);

    private static final int COUNT = 10;
    private static final float MIN_RING = 25f;
    private static final float MAX_RING = 70f;

    // Faded, cool-tinted so they recede into the background.
    private final Color rockColor = new Color(0.55f, 0.5f, 0.6f, 0.4f);
    private final Random rng = new Random();
    private boolean done;

    @Inject
    public DemoRockSystem() {
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
        for (int i = 0; i < COUNT; i++) {
            float angle = i * (360f / COUNT) + MathUtils.random(-15f, 15f);
            float ring = MathUtils.random(MIN_RING, MAX_RING);
            Vector2 pos = new Vector2(heroPos).add(ring * MathUtils.cosDeg(angle), ring * MathUtils.sinDeg(angle));

            // Bigger, since they are far away on a parallax layer.
            float radius = MathUtils.random(3f, 8f);
            int points = MathUtils.random(10, 16);
            RockMesh mesh = AsteroidShape.generate(radius, MathUtils.random(0.3f, 0.6f), points, rng);

            // Alternate FAR_DECO depths so the field has parallax variation.
            DrawableLevel level = (i % 2 == 0) ? DrawableLevel.FAR_DECO_2 : DrawableLevel.FAR_DECO_3;
            ProceduralRockDrawable drawable = new ProceduralRockDrawable(mesh, 1f, rockColor, 0.08f, level);

            List<Drawable> drawables = new ArrayList<>(1);
            drawables.add(drawable);
            // Fixed in space (zero velocity) - a landmark, not a participant.
            game.getObjectManager().addObjDelayed(new DrawableObject(drawables, pos, new Vector2(), null, false, false));
        }

        done = true;
        logger.info("Demo: spawned {} background landmark rocks (parallax, faded, no physics)", COUNT);
    }
}
