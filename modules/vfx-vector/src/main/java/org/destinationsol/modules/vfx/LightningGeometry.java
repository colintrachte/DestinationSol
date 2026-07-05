// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.vfx;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

// The computed geometry of one lightning bolt: an ordered polyline plus a
// matching per-vertex alpha (the bolt fades along its length, .02 + 9.8/(i+10)
// from the GML) and any forks that branched off it. Drawing is a separate
// concern (see VectorEffectsDrawer) so this stays pure and testable.
public final class LightningGeometry {

    public final List<Vector2> points = new ArrayList<>();
    public final List<Float> alphas = new ArrayList<>();
    public final List<LightningGeometry> forks = new ArrayList<>();

    void add(float x, float y, float alpha) {
        points.add(new Vector2(x, y));
        alphas.add(alpha);
    }

    // Total vertices including all forks - handy for tests and the debug log.
    public int totalVertices() {
        int total = points.size();
        for (LightningGeometry fork : forks) {
            total += fork.totalVertices();
        }
        return total;
    }
}
