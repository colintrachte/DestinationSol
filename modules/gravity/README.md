# Astro Gravity module

A standalone Gestalt module that adds **numerically stable N-body gravity** and
**material properties** to Destination Sol, ported from the *Astro Gravity*
GameMaker Studio project.

## Why this exists

Destination Sol's asteroids drift in straight lines and only interact with a
planet through atmospheric burn. Astro Gravity solved real orbital motion for a
whole galaxy of bodies without the instability that sinks most hand-rolled
gravity. The trick is a **softened** force law:

```
a = k * R^3 / (1 + (dist + R)^2)
```

instead of the textbook `G*m/dist^2`. The textbook version goes to infinity as
`dist -> 0`, which is exactly what makes asteroids jitter, get flung off at
absurd speeds, or produce NaN positions. The softened denominator is finite
everywhere, and sourcing the pull from a visual radius `R` (not true mass) keeps
planets and suns a sane strength relative to their on-screen size.

See `AstroGravity.java` for the annotated translation and `AstroGravityTest`
for the stability properties it guarantees.

## What's in the box

| File | Role |
|------|------|
| `AstroGravity.java` | Pure, unit-tested softened-gravity math. |
| `GravityWell.java` | Lightweight attractor (position, radius, cutoff). |
| `systems/AstroGravitySystem.java` | Per-tick system: builds wells from planets/suns, pulls affected bodies. |
| `components/Material.java` | `organic / metallic / ectenic` blend + density. |
| `components/GravityAffected.java` | Marker: this entity falls toward wells. |
| `components/GravitySource.java` | Marker: this entity *is* a well. |

## Wiring it in

`AstroGravitySystem` implements the engine's `UpdateAwareSystem`. Register it in
the engine's update-system set the same way the engine registers its own (see
`SolGame`). It then runs fully self-contained against the live Box2D world, so
it affects both the legacy `SolObject` asteroids and the newer ECS asteroid
entities.

By default it pulls **asteroids** and anything carrying `GravityAffected`.
The player ship is intentionally left out so basic flight feels unchanged; add a
`GravityAffected` component (or extend `isAffected`) to opt ships in.

## Tuning

All knobs are constants at the top of `AstroGravitySystem`:
`FORCE_CONSTANT`, `PLANET_RADIUS_FACTOR`, `SUN_RADIUS_FACTOR`, `CUTOFF_FACTOR`.
Start by raising `FORCE_CONSTANT` until belts visibly bend, then lower
`CUTOFF_FACTOR` if performance dips in dense systems.
