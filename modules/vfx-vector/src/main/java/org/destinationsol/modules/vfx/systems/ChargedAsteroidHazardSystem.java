// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.game.DmgType;
import org.destinationsol.game.Hero;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.UpdateAwareSystem;
import org.destinationsol.game.asteroid.Asteroid;
import org.destinationsol.game.attributes.RegisterUpdateSystem;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;
import org.destinationsol.game.drawables.DrawableObject;
import org.destinationsol.game.item.Shield;
import org.destinationsol.game.ship.SolShip;
import org.destinationsol.modules.vfx.LightningDrawable;
import org.destinationsol.modules.vfx.MetalLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Makes playfield asteroids a real hazard: an asteroid builds an electrical
// charge over time and, once charged, if the hero lingers close enough, it
// discharges a hot bolt straight at the ship.
//
// Damage rule (per the design): lightning damages SHIELDS only. If the hero has
// a shield up, the bolt drains it (DmgType.ENERGY, which shields absorb). If the
// shield is down or absent, the bolt just fizzles on the hull - no damage -
// UNLESS the target is electrically weak (e.g. a future explosive barrel), in
// which case it would take the full hit. Background landmark rocks never do this;
// only the interacting playfield asteroids are hazardous.
@RegisterUpdateSystem(priority = 2)
public class ChargedAsteroidHazardSystem implements UpdateAwareSystem {

    private static final Logger logger = LoggerFactory.getLogger(ChargedAsteroidHazardSystem.class);

    // Seconds an asteroid takes to fully charge before it can discharge.
    private static final float CHARGE_TIME = 5f;
    // How close an asteroid must be to the hero to sense/zap it.
    private static final float ARC_RANGE = 6f;
    // Shield damage per discharge.
    private static final float SHIELD_DAMAGE = 6f;

    private static final float BOLT_LIFE = 0.3f;
    private static final float BOLT_WIDTH = 0.09f;
    private static final float SEGMENT_LENGTH = 0.4f;
    private static final float ANGLE_TOLERANCE = 30f;

    private final Color hazardColor = new Color(0.7f, 0.85f, 1f, 1f);
    private final Random rng = new Random();
    private final List<Vector2> metalSnapshot = new ArrayList<>();

    private float charge;

    @Inject
    public ChargedAsteroidHazardSystem() {
    }

    @Override
    public void update(SolGame game, float timeStep) {
        Hero hero = game.getHero();
        if (hero == null || hero.isTranscendent() || hero.isDead()) {
            charge = 0;
            return;
        }

        charge += timeStep;
        if (charge < CHARGE_TIME) {
            return;
        }

        Vector2 heroPos = hero.getPosition();
        Asteroid source = nearestAsteroid(game, heroPos);
        if (source == null || source.getPosition().dst(heroPos) > ARC_RANGE) {
            // Stay charged and ready; fire the moment the hero comes in range.
            charge = CHARGE_TIME;
            return;
        }

        charge = 0;
        spawnHazardBolt(game, source.getPosition(), heroPos);
        applyShieldOnlyDamage(game, hero.getShipUnchecked(), heroPos);
    }

    private void applyShieldOnlyDamage(SolGame game, SolShip heroShip, Vector2 pos) {
        if (heroShip == null) {
            return;
        }
        Shield shield = heroShip.getShield();
        if (shield != null && shield.canAbsorb(DmgType.ENERGY)) {
            // Shield up: ENERGY damage is routed to (and absorbed by) the shield.
            heroShip.receiveDmg(SHIELD_DAMAGE, game, pos, DmgType.ENERGY);
            logger.debug("charged asteroid zapped the shield for {}", SHIELD_DAMAGE);
        } else {
            // No shield: lightning fizzles on the hull - no hull damage by design.
            // An electrically-weak target would instead take a full hit here.
            logger.debug("charged asteroid arc fizzled (no shield, hull is immune to lightning)");
        }
    }

    private Asteroid nearestAsteroid(SolGame game, Vector2 point) {
        Asteroid best = null;
        float bestDist = Float.MAX_VALUE;
        for (SolObject obj : game.getObjectManager().getObjects()) {
            if (obj instanceof Asteroid) {
                float d = obj.getPosition().dst(point);
                if (d < bestDist) {
                    bestDist = d;
                    best = (Asteroid) obj;
                }
            }
        }
        return best;
    }

    private void spawnHazardBolt(SolGame game, Vector2 from, Vector2 to) {
        metalSnapshot.clear();
        for (SolObject obj : game.getObjectManager().getObjects()) {
            if (obj instanceof Asteroid && obj.getPosition().dst(from) <= ARC_RANGE * 2f) {
                metalSnapshot.add(new Vector2(obj.getPosition()));
            }
        }
        List<Vector2> points = new ArrayList<>(metalSnapshot);
        MetalLocator locator = (x, y, radius) -> {
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

        Vector2 mid = new Vector2(from).add(to).scl(0.5f);
        LightningDrawable bolt = new LightningDrawable(mid, new Vector2(from), new Vector2(to), hazardColor,
                BOLT_WIDTH, BOLT_LIFE, SEGMENT_LENGTH, ANGLE_TOLERANCE, 0f, 3, locator, rng.nextLong(),
                1.5f, DrawableLevel.PROJECTILES, true);

        List<Drawable> drawables = new ArrayList<>(1);
        drawables.add(bolt);
        game.getObjectManager().addObjDelayed(new DrawableObject(drawables, mid, new Vector2(), null, true, false));
    }
}
