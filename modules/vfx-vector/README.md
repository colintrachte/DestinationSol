# Vector VFX module

Geometry-drawn special effects ported from the GameMaker library. These
**complement** Destination Sol's existing particle effects (`PartMan`,
`DSParticleEmitter`, the `.emitter` files) rather than replacing them: smoke,
fire and sparks stay on the particle system; this module adds the line/mesh
effects the engine has no equivalent for.

## What's here

| File | Source GML | Role |
|------|-----------|------|
| `LightningPath` / `LightningGeometry` | `draw_lightning_forked.gml` | Recursive, fading, metal-seeking forked lightning. Pure + deterministic. |
| `TrailRibbon` | `draw_trail.gml` | Tapering triangle-strip ribbon along any path (weapon swipes, comet tails, engine wash). Pure. |
| `MetalLocator` | the `collision_circle(..., metal, ...)` fork target | Hook for "nearest metallic body"; back it with the `Material` component. |
| `VectorEffectsDrawer` | `bm_add` draws | Renders the geometry through the engine's `CommonDrawer` with additive glow. |
| `components/Trail`, `components/LightningBolt` | the `o_*` objects | ECS data describing an effect on an entity. |

## Design: pure geometry, thin draw

The generators are **pure functions** — no engine types beyond `Vector2`, no GL,
deterministic given a `Random`. That is what makes them unit-testable
(`LightningPathTest`, `TrailRibbonTest`) and reproducible from a seed for
replays. `VectorEffectsDrawer` is the only part that touches rendering, and it
rides the existing `CommonDrawer` (so there is no second render pipeline).

## Metal-seeking forks

In the original, half of all lightning forks chase the nearest metal via
`collision_circle`. Here that is the `MetalLocator` interface. The live
implementation should return the nearest body whose `Material.isMetallic()` is
true within the radius — i.e. it reuses the `gravity` module's `Material`
component, so weapons naturally arc toward metal hulls and asteroids.

## Drawing it

Call `VectorEffectsDrawer` from a render hook that already has the camera matrix
bound (a `Drawable` on the emitting object, or a HUD pass). A 1xN white strip
texture works for lightning; a soft gradient strip reads best for trails. Use
`engine:fx` or a `core:` trail texture region.

## Debugging

`VectorEffectsDrawer` logs a throttled heartbeat (≈1 in 120 draws) at DEBUG
under `org.destinationsol.modules.vfx`, reporting vertex and fork counts. The
log line for that package is already present in `log4j.properties`; output lands
in `destinationsol.log`. Set it to INFO to mute.
