// Copyright 2026 The Destination Sol contributors
// SPDX-License-Identifier: Apache-2.0
package org.destinationsol.modules.gravity.components;

import org.terasology.gestalt.entitysystem.component.EmptyComponent;

// Marker component. An entity carrying this (and a Box2D body) is pulled
// toward every GravityWell each tick by the AstroGravitySystem. Asteroids,
// debris, loot, and comets are the natural carriers; the player ship is left
// out by default so basic flight feels unchanged.
public class GravityAffected extends EmptyComponent<GravityAffected> {
}
