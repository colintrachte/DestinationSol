// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.astrobodies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.assets.Assets;
import org.destinationsol.game.GameDrawer;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.asteroid.procedural.RockMesh;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;

// Renders a procedural rock through the engine's Drawable/GameDrawer pipeline.
//
// The level controls which layer it lives on:
//   - a FAR_DECO_* level (depth > 1) makes it a faded BACKGROUND landmark that
//     parallax-scrolls slower than the playfield and draws behind it. These are
//     scenery: no gravity, no collision, fixed in space.
//   - BODIES (depth 1) puts it on the PLAYFIELD, where it lines up 1:1 with the
//     physics world.
//
// Parallax is applied here exactly as RectSprite does it: a point at depth d is
// drawn at (p - cam)/d + cam. getPosition() stays in world space so culling is
// unaffected.
public class ProceduralRockDrawable implements Drawable {

    private final TextureAtlas.AtlasRegion texture = Assets.getAtlasRegion("engine:uiWhiteTex");
    private final DrawableLevel level;

    private final RockMesh mesh;
    private final float scale;
    private final Color outline = new Color();
    private final Color spoke = new Color();
    private final float outlineWidth;

    private final Vector2 position = new Vector2();
    private final Vector2 relativePosition = new Vector2();
    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();

    public ProceduralRockDrawable(RockMesh mesh, float scale, Color outlineColor, float outlineWidth, DrawableLevel level) {
        this.mesh = mesh;
        this.scale = scale;
        this.level = level;
        this.outline.set(outlineColor);
        this.spoke.set(outlineColor.r, outlineColor.g, outlineColor.b, outlineColor.a * 0.25f);
        this.outlineWidth = outlineWidth;
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
    }

    @Override
    public void prepare(SolObject host) {
        position.set(host.getPosition());
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
        return mesh.radius * scale;
    }

    @Override
    public void draw(GameDrawer drawer, SolGame game) {
        int n = mesh.perimeterCount;
        float depth = level.depth;
        Vector2 cam = game.getCam().getPosition();

        // Faint spokes from center to each perimeter vertex (the triangle fan).
        drawer.maybeChangeAdditive(true);
        for (int k = 0; k < n; k++) {
            toDraw(a, position.x, position.y, cam, depth);
            toDraw(b, position.x + mesh.perimeter[k * 2] * scale,
                    position.y + mesh.perimeter[k * 2 + 1] * scale, cam, depth);
            drawer.drawLine(texture, a, b, spoke, outlineWidth * 0.5f, true);
        }
        drawer.maybeChangeAdditive(false);

        // Bright closed outline.
        for (int k = 0; k < n; k++) {
            int next = (k + 1) % n;
            toDraw(a, position.x + mesh.perimeter[k * 2] * scale,
                    position.y + mesh.perimeter[k * 2 + 1] * scale, cam, depth);
            toDraw(b, position.x + mesh.perimeter[next * 2] * scale,
                    position.y + mesh.perimeter[next * 2 + 1] * scale, cam, depth);
            drawer.drawLine(texture, a, b, outline, outlineWidth, true);
        }
    }

    // World point -> draw point, applying parallax for background depths.
    private static void toDraw(Vector2 out, float worldX, float worldY, Vector2 cam, float depth) {
        if (depth == 1f) {
            out.set(worldX, worldY);
        } else {
            out.set((worldX - cam.x) / depth + cam.x, (worldY - cam.y) / depth + cam.y);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean okToRemove() {
        return false;
    }
}
