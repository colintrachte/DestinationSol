// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.assets.Assets;
import org.destinationsol.game.GameDrawer;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;

import java.util.Random;

// An in-world lightning bolt rendered through the engine's Drawable pipeline.
//
// Rather than one fat line, each segment is drawn in three additive passes - a
// wide dim glow, a medium gradient mid-layer, and a thin bright white core - so
// it reads as a glowing bolt with a hot center and soft bloom. The colour runs
// as a gradient from a hot core colour near the source to the cooler glow colour
// at the tip, the width tapers toward the tip, and a small spark burst flares at
// the impact point. An `intensity` (0..~1.5) scales brightness/width/sparks so a
// "charged" emitter can visibly ramp up over time.
//
// This is the no-extra-pipeline upgrade. A true textured triangle-strip ribbon
// (TrailRibbon) with per-vertex colour is the further step once a
// PolygonSpriteBatch pass exists.
public class LightningDrawable implements Drawable {

    private static final float FLICKER_INTERVAL = 0.04f;

    private final TextureAtlas.AtlasRegion texture = Assets.getAtlasRegion("engine:uiWhiteTex");
    private final DrawableLevel level;

    private final Vector2 relativeA = new Vector2();
    private final Vector2 relativeB = new Vector2();
    private final Vector2 absoluteA = new Vector2();
    private final Vector2 absoluteB = new Vector2();
    private final Vector2 position = new Vector2();
    private final Vector2 relativePosition = new Vector2();

    private final Color glowColor = new Color();
    private final Color coreColor = new Color();
    private final Color tmp = new Color();
    private final Vector2 sparkEnd = new Vector2();

    private final float width;
    private final float radius;
    private final float intensity;
    private final boolean impactSparks;

    private final float segmentLength;
    private final float angleToleranceDeg;
    private final float curlDeg;
    private final int maxForkDepth;
    private final MetalLocator locator;
    private final Random rng;

    private float life;
    private final float maxLife;
    private float flickerTimer;
    private LightningGeometry geometry;

    public LightningDrawable(Vector2 hostPosition, Vector2 endA, Vector2 endB, Color color,
                             float width, float life, float segmentLength, float angleToleranceDeg,
                             float curlDeg, int maxForkDepth, MetalLocator locator, long seed,
                             float intensity, DrawableLevel level, boolean impactSparks) {
        this.relativeA.set(endA).sub(hostPosition);
        this.relativeB.set(endB).sub(hostPosition);
        this.absoluteA.set(endA);
        this.absoluteB.set(endB);
        this.position.set(hostPosition);
        this.relativePosition.set(relativeA).add(relativeB).scl(0.5f);
        this.glowColor.set(color);
        // Hot core: the glow colour pushed toward white.
        this.coreColor.set(Math.min(1f, color.r + 0.5f), Math.min(1f, color.g + 0.5f),
                Math.min(1f, color.b + 0.5f), 1f);
        this.width = width;
        this.life = life;
        this.maxLife = life;
        this.intensity = intensity;
        this.impactSparks = impactSparks;
        this.level = level;
        this.segmentLength = segmentLength;
        this.angleToleranceDeg = angleToleranceDeg;
        this.curlDeg = curlDeg;
        this.maxForkDepth = maxForkDepth;
        this.locator = locator == null ? MetalLocator.NONE : locator;
        this.rng = new Random(seed);
        this.radius = endA.dst(endB) * 0.5f + segmentLength;
        regenerate();
    }

    private void regenerate() {
        geometry = LightningPath.generate(absoluteA.x, absoluteA.y, absoluteB.x, absoluteB.y,
                segmentLength, angleToleranceDeg, curlDeg, locator, rng, maxForkDepth);
    }

    @Override
    public TextureAtlas.AtlasRegion getTexture() {
        return texture;
    }

    @Override
    public DrawableLevel getLevel() {
        return level;
    }

    @Override
    public void update(SolGame game, SolObject host) {
        float dt = game.getTimeStep();
        life -= dt;
        absoluteA.set(host.getPosition()).add(relativeA);
        absoluteB.set(host.getPosition()).add(relativeB);
        flickerTimer -= dt;
        if (flickerTimer <= 0) {
            flickerTimer = FLICKER_INTERVAL;
            regenerate();
        }
    }

    @Override
    public void prepare(SolObject host) {
        position.set(host.getPosition()).add(relativePosition);
    }

    @Override
    public Vector2 getPosition() {
        return position;
    }

    @Override
    public Vector2 getRelativePosition() {
        return relativePosition;
    }

    @Override
    public float getRadius() {
        return radius;
    }

    @Override
    public void draw(GameDrawer drawer, SolGame game) {
        float fade = life <= 0 ? 0 : (life / maxLife);
        float boltAlpha = fade * Math.min(1.5f, intensity);
        if (boltAlpha <= 0) {
            return;
        }
        drawer.maybeChangeAdditive(true);
        drawBolt(drawer, geometry);
        if (impactSparks) {
            drawImpact(drawer, boltAlpha);
        }
        drawer.maybeChangeAdditive(false);
    }

    private void drawBolt(GameDrawer drawer, LightningGeometry geo) {
        float fade = life <= 0 ? 0 : (life / maxLife);
        float boltAlpha = fade * Math.min(1.5f, intensity);
        int n = geo.points.size();
        for (int i = 1; i < n; i++) {
            float t = n <= 1 ? 0 : (i / (float) (n - 1));
            float segAlpha = geo.alphas.get(i) * boltAlpha;
            if (segAlpha <= 0.01f) {
                continue;
            }
            float taper = 1f - 0.65f * t;
            float w = width * taper * (0.8f + 0.6f * intensity);

            Vector2 p0 = geo.points.get(i - 1);
            Vector2 p1 = geo.points.get(i);

            // wide soft glow (glow colour)
            tmp.set(glowColor.r, glowColor.g, glowColor.b, clamp01(segAlpha * 0.22f));
            drawer.drawLine(texture, p0, p1, tmp, w * 3.0f, true);
            // mid layer: colour gradient hot -> cool along the bolt
            lerp(tmp, coreColor, glowColor, t, clamp01(segAlpha * 0.5f));
            drawer.drawLine(texture, p0, p1, tmp, w * 1.6f, true);
            // thin bright white core
            tmp.set(1f, 1f, 1f, clamp01(segAlpha * 1.1f));
            drawer.drawLine(texture, p0, p1, tmp, w * 0.55f, true);
        }
        for (LightningGeometry fork : geo.forks) {
            drawBolt(drawer, fork);
        }
    }

    // A short radial spark burst at the impact end, approximating the particle
    // flecks the original used, without needing the particle system.
    private void drawImpact(GameDrawer drawer, float boltAlpha) {
        Vector2 end = geometry.points.get(geometry.points.size() - 1);
        int sparks = 3 + (int) (intensity * 3);
        for (int s = 0; s < sparks; s++) {
            float ang = rng.nextFloat() * 360f;
            float len = width * (2f + rng.nextFloat() * 6f) * intensity;
            sparkEnd.set(end.x + len * (float) Math.cos(Math.toRadians(ang)),
                    end.y + len * (float) Math.sin(Math.toRadians(ang)));
            tmp.set(coreColor.r, coreColor.g, coreColor.b, clamp01(boltAlpha * 0.8f));
            drawer.drawLine(texture, end, sparkEnd, tmp, width * 0.5f, true);
        }
    }

    private static void lerp(Color out, Color a, Color b, float t, float alpha) {
        out.set(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t, alpha);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean okToRemove() {
        return life <= 0;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
