// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity.components;

import org.terasology.gestalt.entitysystem.component.Component;

// Ported from Astro Gravity's object_set_material(organic, metallic, ectenic).
// Every body in the universe is described as a blend of three base materials.
// The three weights are normally in the 0..1 range and need not sum to one;
// they bias how an entity reacts to weapons, heat, and lightning (metallic
// bodies attract forked lightning, organic bodies burn, ectenic is the
// sci-fi "exotic" component used for shields and exotic matter).
public class Material implements Component<Material> {

    public float organic;
    public float metallic;
    public float ectenic;

    // Bulk density in kg/m^3. Astro Gravity used a realistic spread
    // (~500..7500; Earth is ~5500). Mass is derived as volume * density.
    public float density = 5500f;

    public void set(float organic, float metallic, float ectenic) {
        this.organic = organic;
        this.metallic = metallic;
        this.ectenic = ectenic;
    }

    // True when the metallic component dominates - used by lightning targeting.
    public boolean isMetallic() {
        return metallic >= organic && metallic >= ectenic;
    }

    @Override
    public void copyFrom(Material other) {
        this.organic = other.organic;
        this.metallic = other.metallic;
        this.ectenic = other.ectenic;
        this.density = other.density;
    }
}
