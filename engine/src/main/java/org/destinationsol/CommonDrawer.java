/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.destinationsol;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import org.destinationsol.assets.Assets;
import org.destinationsol.common.SolMath;
import org.destinationsol.ui.DisplayDimensions;
import org.destinationsol.ui.ResizeSubscriber;
import org.destinationsol.ui.UiDrawer;

import javax.inject.Inject;

public class CommonDrawer implements ResizeSubscriber {
    private final SpriteBatch spriteBatch;
    private final PolygonSpriteBatch polygonBatch;
    private final BitmapFont font;
    private final float originalFontHeight;
    private final GlyphLayout layout;
    private final OrthographicCamera orthographicCamera;
    private final Viewport screenViewport;
    private boolean polygonBatchActive;

    private DisplayDimensions displayDimensions;

    @Inject
    CommonDrawer(DisplayDimensions displayDimensions) {
        this.displayDimensions = displayDimensions;

        spriteBatch = new SpriteBatch();
        polygonBatch = new PolygonSpriteBatch();

        font = Assets.getFont("engine:main").getBitmapFont();
        originalFontHeight = font.getXHeight();

        layout = new GlyphLayout();

        orthographicCamera = new OrthographicCamera(1024, 768);
        screenViewport = new ScreenViewport(orthographicCamera);

        SolApplication.addResizeSubscriber(this);
    }

    public void setMatrix(Matrix4 matrix) {
        spriteBatch.setProjectionMatrix(matrix);
        polygonBatch.setProjectionMatrix(matrix);
    }

    public void begin() {
        orthographicCamera.update();
        spriteBatch.begin();
        polygonBatchActive = false;
    }

    public void end() {
        toSpriteBatch();
        spriteBatch.end();
    }

    // Textured triangle-mesh draws (procedural asteroids) need PolygonSpriteBatch,
    // which can't be active at the same time as the regular SpriteBatch - so
    // switching between quad draws and mesh draws means ending one and starting
    // the other, mirroring how setAdditive() toggles blend state.
    private void toSpriteBatch() {
        if (polygonBatchActive) {
            polygonBatch.end();
            polygonBatchActive = false;
            spriteBatch.begin();
        }
    }

    private void toPolygonBatch() {
        if (!polygonBatchActive) {
            spriteBatch.end();
            polygonBatch.begin();
            polygonBatchActive = true;
        }
    }

    /**
     * Draws a textured triangle mesh in packed PolygonSpriteBatch vertex format
     * (x, y, colorFloatBits, u, v per vertex, u/v already in atlas texture space).
     */
    public void drawMesh(TextureRegion tex, float[] vertices, short[] triangles) {
        toPolygonBatch();
        polygonBatch.draw(tex.getTexture(), vertices, 0, vertices.length, triangles, 0, triangles.length);
    }

    public void drawString(String s, float x, float y, float fontSize, boolean centered, Color col) {
        drawString(s, x, y, fontSize, UiDrawer.TextAlignment.CENTER, centered, col);
    }

    public void drawString(String s, float x, float y, float fontSize, UiDrawer.TextAlignment align, boolean verticalCentering, Color col) {
        if (s == null) {
            return;
        }

        toSpriteBatch();
        font.setColor(col);
        font.getData().setScale(fontSize / originalFontHeight);
        // http://www.badlogicgames.com/wordpress/?p=3658
        layout.reset();
        layout.setText(font, s);

        switch (align) {
            case LEFT:
                break;
            case CENTER:
                x -= layout.width / 2;
                break;
            case RIGHT:
                x -= layout.width;
                break;
        }

        if (verticalCentering) {
            y -= layout.height / 2;
        }

        font.draw(spriteBatch, layout, x, y);
    }

    public void draw(TextureRegion tr, float width, float height, float origX, float origY, float x, float y,
                     float rot, Color tint) {
        toSpriteBatch();
        setTint(tint);
        spriteBatch.draw(tr, x - origX, y - origY, origX, origY, width, height, 1, 1, rot);
//        setTint(Color.CYAN);
//        spriteBatch.draw(UiDrawer.whiteTexture, 0, 0, 0.5f, 0.5f); // debug rectangle for render overhaul purpose
    }

    private void setTint(Color tint) {
        spriteBatch.setColor(tint);
    }

    public void draw(TextureRegion tex, Rectangle rect, Color tint) {
        draw(tex, rect.width, rect.height, (float) 0, (float) 0, rect.x, rect.y, (float) 0, tint);
    }

    public void drawCircle(TextureRegion tex, Vector2 center, float radius, Color col, float width, float vh) {
        float relRad = radius / vh;
        int pointCount = (int) (160 * relRad);
        Vector2 position = SolMath.getVec();
        if (pointCount < 8) {
            pointCount = 8;
        }
        float lineLen = radius * MathUtils.PI * 2 / pointCount;
        float angleStep = 360f / pointCount;
        float angleStepH = angleStep / 2;
        for (int i = 0; i < pointCount; i++) {
            float angle = angleStep * i;
            SolMath.fromAl(position, angle, radius);
            position.add(center);
            draw(tex, width, lineLen, (float) 0, (float) 0, position.x, position.y, angle + angleStepH, col);
        }
        SolMath.free(position);
    }

    public void drawLine(TextureRegion tex, float x, float y, float angle, float len, Color col, float width) {
        draw(tex, len, width, 0, width / 2, x, y, angle, col);
    }

    public void drawLine(TextureRegion tex, Vector2 startPoint, Vector2 endPoint, Color color, float width, boolean precise) {
        Vector2 endPointCopy = SolMath.getVec(endPoint);
        endPointCopy.sub(startPoint);
        drawLine(tex, startPoint.x, startPoint.y, SolMath.angle(endPointCopy), endPointCopy.len(), color, width);
        SolMath.free(endPointCopy);
    }

    public void dispose() {
        spriteBatch.dispose();
        polygonBatch.dispose();
        font.dispose();
    }

    public SpriteBatch getSpriteBatch() {
        toSpriteBatch();
        return spriteBatch;
    }

    public void setAdditive(boolean additive) {
        int dstFunc = additive ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA;
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, dstFunc);
        polygonBatch.setBlendFunction(GL20.GL_SRC_ALPHA, dstFunc);
    }

    @Override
    public void resize() {
        screenViewport.update(displayDimensions.getWidth(), displayDimensions.getHeight(), true);
    }
}
