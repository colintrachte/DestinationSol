# Astro Bodies module

Procedural asteroid/space-body geometry ported from *Astro Gravity*. A rock is a
random-perimeter ring, triangulated into a textured mesh, with the **same
outline reused as exact Box2D collision** — so what you see is what you hit.

The core geometry (`AsteroidShape`, `RockMesh`) and the Box2D fixture builder
(`RockCollision`) have been **promoted to the engine**, at
`org.destinationsol.game.asteroid.procedural`, because real gameplay asteroids
now use them directly (`AsteroidBuilder.build()`) — modules can depend on the
engine but not the other way around, so anything the engine itself needs to
build asteroids has to live there rather than in this module. This module
keeps only what's specific to its own decorative use case.

## Pipeline (from `path_random_circle` + `draw_space_rock`)

1. **Perimeter** — `AsteroidShape.generate` lays a closed ring of points whose
   radius wanders between `radius*(1-variance)` and `radius`.
2. **Fill** — triangulated as a fan from the center (`RockMesh.indices`), so it's
   a solid mesh, not just an outline.
3. **Texture layers** — `RockMesh` carries per-vertex `uvs` ready for one or more
   texture layers (a base layer plus additive detail, exactly as the GML stacked
   `draw_path_circle` + a `bm_add` `draw_path_circle_ext`).

## Files

| File | Role |
|------|------|
| `org.destinationsol.game.asteroid.procedural.AsteroidShape` (engine) | Pure, deterministic generator → `RockMesh`. Unit-tested. |
| `org.destinationsol.game.asteroid.procedural.RockMesh` (engine) | Perimeter + vertices + uvs + fan indices. |
| `org.destinationsol.game.asteroid.procedural.RockCollision` (engine) | Attaches one Box2D triangle fixture per fan triangle — exact concave collision. Used by `AsteroidBuilder`. |
| `ProceduralRockDrawable` (this module) | Renders outline + fan spokes through `GameDrawer` for **background landmark** rocks — faded, parallax, no physics. |
| `systems/DemoRockSystem` (this module) | Spawns a few decorative procedural rocks near the hero so you can see them. |

Real playfield asteroids (full collision, hazards) render through the engine's
`org.destinationsol.game.asteroid.ProceduralAsteroidDrawable`, which fills the
mesh with the asteroid's texture via a `PolygonSpriteBatch` pass rather than
drawing an outline. This module's `ProceduralRockDrawable` intentionally stays
wireframe-only since decorative landmarks are meant to read as faint scenery,
not full-detail rocks.
