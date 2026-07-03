// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.game.asteroid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.game.GameDrawer;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.SolObject;
import org.destinationsol.game.asteroid.procedural.RockMesh;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;

// Renders a procedural rock as a filled, textured triangle-fan mesh (rather than
// a rectangular sprite), through GameDrawer's PolygonSpriteBatch pass. The same
// RockMesh doubles as the Box2D collision fixtures (see RockCollision), so the
// drawn silhouette and the physical one are identical.
public class ProceduralAsteroidDrawable implements Drawable {
    private final RockMesh mesh;
    private final TextureAtlas.AtlasRegion texture;
    private final DrawableLevel level;
    private final float radius;

    // Packed PolygonSpriteBatch vertex format: x, y, colorFloatBits, u, v.
    // Color and uv are fixed per vertex, so only x/y are recomputed each frame.
    private final float[] packedVertices;
    private final short[] triangles;

    private final Vector2 position = new Vector2();
    private final Vector2 relativePosition = new Vector2();

    public ProceduralAsteroidDrawable(RockMesh mesh, TextureAtlas.AtlasRegion texture, Color tint, DrawableLevel level) {
        this.mesh = mesh;
        this.texture = texture;
        this.level = level;
        this.radius = mesh.radius;
        this.triangles = mesh.indices;

        int vertexCount = mesh.vertices.length / 2;
        packedVertices = new float[vertexCount * 5];
        float colorBits = tint.toFloatBits();
        float u0 = texture.getU();
        float v0 = texture.getV();
        float uSpan = texture.getU2() - u0;
        float vSpan = texture.getV2() - v0;
        for (int i = 0; i < vertexCount; i++) {
            packedVertices[i * 5 + 2] = colorBits;
            packedVertices[i * 5 + 3] = u0 + mesh.uvs[i * 2] * uSpan;
            packedVertices[i * 5 + 4] = v0 + mesh.uvs[i * 2 + 1] * vSpan;
        }
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
    public void update(SolGame game, SolObject o) {
    }

    @Override
    public void prepare(SolObject o) {
        position.set(o.getPosition());
        float angleRad = o.getAngle() * MathUtils.degRad;
        float cos = MathUtils.cos(angleRad);
        float sin = MathUtils.sin(angleRad);
        int vertexCount = mesh.vertices.length / 2;
        for (int i = 0; i < vertexCount; i++) {
            float lx = mesh.vertices[i * 2];
            float ly = mesh.vertices[i * 2 + 1];
            packedVertices[i * 5] = lx * cos - ly * sin + position.x;
            packedVertices[i * 5 + 1] = lx * sin + ly * cos + position.y;
        }
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
        drawer.drawMesh(texture, packedVertices, triangles);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean okToRemove() {
        return true;
    }
}
