// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.common.SolColor;
import org.destinationsol.CommonDrawer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// Thin draw layer that renders the pure geometry through the engine's existing
// CommonDrawer (a textured sprite batch), using additive blending for glow -
// the same bm_add the GameMaker effects used. This keeps the effects on the
// engine's normal render pipeline instead of introducing a second one.
//
// Call these from a render hook that already has the camera matrix bound (for
// example a Drawable attached to the emitting object, or a HUD pass). A 1xN
// white "line" texture region works for both; for trails a soft gradient strip
// reads best.
public final class VectorEffectsDrawer {

    private static final Logger logger = LoggerFactory.getLogger(VectorEffectsDrawer.class);

    // Log roughly one in this many draws when DEBUG is on, so we get a heartbeat
    // without a line every frame.
    private static final int LOG_EVERY = 120;

    private final Color tmpColor = new Color();
    private int drawCounter;

    // Draws a bolt and all of its forks as additive line segments whose alpha
    // follows the geometry's per-vertex fade.
    public void drawLightning(CommonDrawer drawer, LightningGeometry geo, TextureRegion lineTexture, Color color, float width) {
        drawer.setAdditive(true);
        drawLightningRecursive(drawer, geo, lineTexture, color, width);
        drawer.setAdditive(false);

        if (logger.isDebugEnabled() && (drawCounter++ % LOG_EVERY == 0)) {
            logger.debug("vfx lightning: {} total vertices, {} top-level forks",
                    geo.totalVertices(), geo.forks.size());
        }
    }

    private void drawLightningRecursive(CommonDrawer drawer, LightningGeometry geo, TextureRegion lineTexture, Color color, float width) {
        List<Vector2> pts = geo.points;
        for (int i = 1; i < pts.size(); i++) {
            float a = geo.alphas.get(i);
            tmpColor.set(color.r, color.g, color.b, color.a * clamp01(a));
            drawer.drawLine(lineTexture, pts.get(i - 1), pts.get(i), tmpColor, width, true);
        }
        for (LightningGeometry fork : geo.forks) {
            drawLightningRecursive(drawer, fork, lineTexture, color, width);
        }
    }

    // Draws a tapering trail along the path as a run of additive line segments.
    // (For a single-draw textured ribbon, feed TrailRibbon.build(...) to a mesh
    // renderer instead; this segment version needs no extra pipeline.)
    public void drawTrail(CommonDrawer drawer, List<Vector2> path, float widthStart, float widthEnd, TextureRegion texture, Color color) {
        int n = path == null ? 0 : path.size();
        if (n < 2) {
            return;
        }
        drawer.setAdditive(true);
        for (int i = 1; i < n; i++) {
            float t = i / (float) (n - 1);
            float width = widthStart + (widthEnd - widthStart) * t;
            drawer.drawLine(texture, path.get(i - 1), path.get(i), color, width, true);
        }
        drawer.setAdditive(false);
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    // Kept so callers can grab a sensible default glow tint without importing
    // SolColor everywhere.
    public static Color defaultGlow() {
        return new Color(SolColor.WHITE);
    }
}
