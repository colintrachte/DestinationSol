# Porting the GameMaker library into Destination Sol

This document maps the strongest systems from the GameMaker Studio 1 projects
(`E:\Game Maker\GMS 1\My Projects`) onto Destination Sol, and lays out a path to
add them as **standalone Gestalt modules** rather than as invasive edits to the
engine. The first system — softened N-body asteroid gravity from *Astro
Gravity* — is already scaffolded as a working module under `modules/gravity`.

The focus of this pass, per the request, is the **asteroid + fire/effects
engine**, with the material-property and parent/child-nesting ideas captured
where they intersect those systems.

---

## 1. The organizing idea: systems as modules

Destination Sol already has the bones of a plugin system. It uses Terasology
**Gestalt** modules plus an **entity-component-system** (`org.destinationsol.entitysystem`).
A module is just a folder under `modules/` with a `module.json`, a one-line
`build.gradle` (`id 'destination-sol-module'`), an `assets/` tree, and
`src/main/java`. The build auto-discovers any such folder (`modules/subprojects.gradle`),
and a module may carry its own components, systems, events, and assets while
depending on the engine.

That maps cleanly onto how the GameMaker projects are organized: each project is
effectively a self-contained feature (procedural asteroids, lightning, trails,
shields, ship creator) built from a parent-object hierarchy plus a folder of
scripts. The translation strategy is therefore:

> One GameMaker "project / parent-object family" → one Destination Sol module.
> Shared GML scripts (`math_*`, `draw_*`) → engine-independent utility classes
> inside the module, kept pure where possible so they can be unit-tested.

Recommended module breakdown (priority order):

| Module | Source project(s) | Status |
|--------|-------------------|--------|
| `gravity` | Astro Gravity | **Built + auto-wired + tested** |
| `vfx-vector` | Epic Procedural Lightning, Weapon Trail Effects, Shock Glass, Energy Shields | **Lightning (ambient + weapon-fire) + trail built + tested** |
| `astro-bodies` | Astro Gravity (`create_space_matter`, `p_space_matter` tree) | **Procedural shape + collision + outline render built + tested** |
| `materials` | Astro Gravity (`object_set_material`, `periodic_table.csv`) | Designed below |
| `formula-curves` | Holy Grid, Spreadsheets, Script Starter Kit | Out of scope this pass |

---

## 2. Asteroids and gravity — the flagship

### 2.1 The problem in Destination Sol today

There are two parallel asteroid paths:

- **Legacy**: `game.asteroid.Asteroid` (a `SolObject`) built by `AsteroidBuilder`.
  This is what actually runs. Asteroids are Box2D dynamic bodies with a linear
  velocity, a spin, split-on-death, and atmospheric burn near a planet
  (`updateInAtm`). There is **no gravity** — they drift in straight lines.
- **ECS**: `asteroids.components.AsteroidMesh` + `asteroids.systems.AsteroidBodyCreationSystem`,
  the newer entity-based path, also with no gravity.

So belts are static rings of drifting rocks; nothing falls toward a planet or a
sun, and there are no orbits. The note from the request — "the issues I have
been having with asteroids in Destination Sol are totally solved in Astro
Gravity" — points straight at this gap.

### 2.2 What Astro Gravity does

In `o_asteroid`'s Step event the whole physics update is two lines:

```gml
math_astrogravity(p_space_matter, screen.sec * .012, 2000)
math_astrogravity_ext(o_shockwave, screen.sec * .012, 2000, 1000)
```

`p_space_matter` is the parent of every celestial body (stars, planets, moons,
comets, asteroids), so this is genuine N-body attraction toward all of them,
plus a repulsion term from shockwaves. The force law (`math_astrogravity.gml`)
is the important part:

```gml
magnitude = point_distance(0, 0, sprite_width, sprite_height)
motion_add(point_direction(x, y, other.x, other.y),
           force * power(magnitude, 3) / (1 + sqr(dist + magnitude)))
```

Two design decisions make this stable where naive gravity is not:

1. **Softening.** The denominator is `1 + (dist + R)^2`, not `dist^2`. Textbook
   `G*m/dist^2` diverges to infinity as `dist -> 0`; that singularity is the
   root cause of asteroids that jitter, get flung off at impossible speeds, or
   end up at NaN coordinates. The `+1` and the `+R` inside the square make the
   force finite everywhere and taper it smoothly to zero at the center.
2. **Radius, not mass, as the source.** `R` is the visual size of the body, so
   wells stay sensible relative to how big they look on screen. (Astro Gravity
   keeps a separate realistic `mass = volume * density` only for *classifying*
   what kind of star/planet a body is — see materials below.)

Everything is multiplied by `screen.sec` (delta time), so behavior is
frame-rate independent, and a `2000`-pixel **cutoff** keeps the cost bounded —
a body only feels the wells near it.

### 2.3 The translation (already written)

`modules/gravity/AstroGravity.java` is the line-for-line translation of the
force law as a pure function:

```java
public static float accelerationMagnitude(float k, float radius, float dist, float cutoff) {
    if (dist >= cutoff) {
        return 0f;
    }
    float r = radius;
    float soft = dist + r;
    return k * (r * r * r) / (1f + soft * soft);
}
```

`systems/AstroGravitySystem.java` is the `update` loop. It implements the
engine's `UpdateAwareSystem`, rebuilds a pooled list of `GravityWell`s each
tick from `PlanetManager.getPlanets()` (radius from `getFullHeight()`) and
`getSystems()` (suns at system centers), then iterates the live Box2D world
(`world.getBodies(...)`) and applies, to each affected dynamic body, an impulse
`mass * deltaVelocity`. Multiplying by mass reproduces the GML behavior where
`m1` cancels out of `F = m1 * a`, so every body gets the same velocity change
regardless of mass.

It deliberately affects only asteroids (and anything tagged `GravityAffected`)
by default, so the player ship still flies the way players expect. The whole
thing is decoupled from the asteroid classes — it keys off Box2D `userData`, so
it works for both the legacy and ECS asteroid paths at once.

Stability is locked down by `AstroGravityTest` (finite at center, never NaN,
zero past cutoff, pulls inward, weakens with distance).

### 2.4 Stable initial orbits (next step for `astro-bodies`)

`create_space_matter.gml` does more than place a body — it computes a proper
orbital state so things actually orbit instead of falling straight in:

```gml
orbit_speed     = math_orbital_velocity(mass, orbit_radius)   // sqrt(GM/r)
velocity_angle  = orbit_angle + 90 + tolerance(...)           // tangential, jittered
ke              = sqr(orbit_speed) / 2
energy          = ke - GM / orbit_radius                       // specific orbital energy
// eccentricity vector, semi-latus rectum, etc. -> path_orbit(...)
```

When `energy > 0` the body is moving faster than escape velocity and is flagged
an "orphan" (hyperbolic). This is worth porting into the belt/world generator so
that newly spawned asteroids get a tangential velocity for a near-circular orbit
(`v = sqrt(k_orbit * R / r)`) rather than a random drift. That single change,
combined with the gravity module, turns belts into living orbital rings.

### 2.5 Procedural shape and area-conserving fragmentation

Astro Gravity does not use a sprite for the rock at all — it builds the asteroid
**procedurally as a mesh** and skins it. The pipeline (from `create_space_matter`
and `draw_space_rock`) is:

1. **Perimeter.** Map a closed ring of points between an inner and outer radius:

   ```gml
   path_random_circle(path, 0, 0, radius * (1 - variance), radius, 0, radius, 1, 1)
   ```

   i.e. a polygon whose vertices sit between `radius*(1-variance)` and `radius`.

2. **Fill.** Triangulate that perimeter (a triangle fan from the center) so it
   is a solid shape, not just an outline (`draw_path_circle`).

3. **Texture layers.** Skin the triangles with one or more texture layers
   depending on what the body is — the base colour layer, then an **additive**
   detail layer on top for the surface/glow (`draw_space_rock` draws a solid
   `draw_path_circle` then a `bm_add` `draw_path_circle_ext` with a texture).
   Stars, planets, comets and asteroids differ only in how many layers and which
   textures/blends they stack.

The Destination Sol translation lives in the `astro-bodies` module: generate the
perimeter points (jitter each radius by `variance`), triangulate to a libGDX
mesh / `PolygonSprite`, and draw a base layer plus additive detail layers keyed
off the body's `type` and `Material`. The same perimeter doubles as the Box2D
collision polygon (or feeds the existing `CollisionMeshLoader`), so collision and
visuals match exactly — and you are no longer limited to the two bundled asteroid
textures. The `vfx-vector` module's `TrailRibbon` already demonstrates the
"generate vertices in a pure function, draw them in a thin layer" pattern this
needs.

Fragmentation is **area-conserving** and matches what the engine already does in
`Asteroid.maybeSplit`, but stated more cleanly. On destruction Astro Gravity
spawns `n` children with `r = sqrt(radius^2 / n)` so total cross-sectional area
is preserved, and only if `r > 8`, otherwise it drops loot points:

```gml
var n = 2 + random(10);
var r = sqrt(sqr(radius) / n);
if (r > 8) { repeat n { create_space_matter(r, ...) } }
else       { create(o_point) }   // loot instead of more rocks
```

The engine's `maybeSplit` already conserves area (`sclSum < .7 * size*size`);
the worthwhile borrow is the **clean radius formula** and the **"too small to
split -> drop loot instead"** cutoff, which avoids dust-sized physics bodies.

---

## 3. Material properties

Astro Gravity gives every body a three-component material vector:

```gml
///object_set_material(organic, metallic, ectenic)
material[0] = organic; material[1] = metallic; material[2] = ectenic;
```

Asteroids are "mostly metallic", comets "mostly organic", planets/moons mixed —
set with `random_squared` so extremes are rare. There is also a real
`periodic_table.csv` (Type, Name, Symbol, Rarity ppm, Half-Life) and a realistic
`density` in kg/m^3, with `mass = volume * density`.

This is captured in the scaffold as `modules/gravity/components/Material.java`
(`organic / metallic / ectenic` + `density`, plus an `isMetallic()` helper). In
a dedicated `materials` module it would grow into:

- a loader for `periodic_table.csv` (ship the CSV as a module asset under
  `assets/`), used to roll a plausible elemental composition from the material
  blend and to compute mined-ore yields;
- damage/heat reactions keyed off the blend (organic burns, metallic conducts,
  ectenic powers shields);
- the lightning targeting hook below (`isMetallic()` decides what forks chase).

Recommendation: **integrate** `Material` as a reusable engine-adjacent
component (asteroids, ships, and stations can all carry it), since multiple
modules want to read it.

---

## 4. The fire / effects engine — integrate vs. parallel

Destination Sol already has a particle effects layer: `game.particle.PartMan`,
`DSParticleEmitter`, `SpecialEffects`, an `.emitter` file format
(`assets/emitters/fire.emitter`, `flame`, `spark`, `explFire`, `explSmoke`,
`electricity`, `railTrail`, ...) and `specialEffectsConfig.json`. The GameMaker
library's effects split into two clearly different kinds, which drives the
integrate-or-parallel call:

**Particle-based effects (smoke, fire, sparks, explosions, dust) → integrate.**
These already exist as emitters in the engine. The GML equivalents
(`effect_explosion`, the asteroid burn smoke/fire sources) map directly onto
`SpecialEffects`/`PartMan`. Port the *parameters and triggers* (e.g. asteroid
burn intensity scaling with atmospheric speed), not a new particle system.

**Vector / geometry effects → parallel.** These are drawn from primitives, not
particles, and the engine has no equivalent, so they belong in a new `vfx-vector`
module:

- **Forked, metal-seeking lightning** (`draw_lightning_forked.gml`). A
  recursive line-strip whose alpha fades with distance (`.02 + 9.8/(i+10)`),
  whose forks fire probabilistically, and — the clever bit — half of all forks
  steer toward the nearest metal via `collision_circle(..., argument8, ...)`.
  In Destination Sol that target test becomes "nearest body whose `Material`
  `isMetallic()`", reusing the material component. There is a whole family here
  (`simple`, `path`, `ring`, `flat`, `hue`, `color`, `textured`) to draw from.
- **Path-ribbon trails** (`draw_trail.gml`). A textured triangle-strip built
  along *any* path, tapering from a start width to an end width. One routine
  serves weapon swipes, comet tails (`create_trail` in Astro Gravity), engine
  trails, and shockwave arcs. This is more flexible than the engine's
  `railTrail` emitter and is worth having as a primitive.
- **Shockwave rings** (`create_shockwave`, `o_shockwave`) and the **Shock Glass
  / magnifier** distortion (`draw_magnify*`) — screen-space ring distortions
  used on explosions and lasers. These are shader/mesh effects with no engine
  analogue; parallel module.
- **Energy shields** (`create_shield` / `damage_shield` / `o_shield`) — a
  hit-reactive bubble that ripples where struck; pairs naturally with the
  ectenic material component.

Because Destination Sol renders through libGDX, all of these translate to
`ShapeRenderer` / `PolygonSpriteBatch` / mesh draws with additive blending
(`bm_add` → `GL20.GL_ONE`), the same blend the GML uses for glow. The engine's
`CommonDrawer` already exposes `drawLine`, `drawCircle`, and `setAdditive(true)`,
so the simplest effects need no new pipeline at all.

**The first two are now built** in `modules/vfx-vector` (see its README):

- `LightningPath` / `LightningGeometry` — the recursive, fading, metal-seeking
  forked lightning, as a pure deterministic generator. The "nearest metal"
  fork target is the `MetalLocator` interface, backed by the `gravity` module's
  `Material.isMetallic()`.
- `TrailRibbon` — the tapering path ribbon, as a pure vertex generator.
- `VectorEffectsDrawer` — the thin draw layer over `CommonDrawer` with additive
  glow.

Both generators are unit-tested (`LightningPathTest`, `TrailRibbonTest`) and the
math was independently validated (endpoints snap, alpha stays in range, the walk
always terminates, ribbon offsets are perpendicular). Shockwave rings, the
shock-glass magnifier, and energy shields remain as designed-but-not-built items
in the same module.

---

## 5. Cross-cutting patterns worth stealing

These show up across the GameMaker projects and are good engine-wide habits:

- **Delta-time everywhere.** Every motion/animation term is multiplied by
  `screen.sec`. Destination Sol already threads `timeStep` through
  `UpdateAwareSystem.update`, so honor it in every ported system (the gravity
  module does).
- **Camera/zoom LOD culling.** `o_asteroid` only simulates when in view *and*
  `camera.zoom < radius`; off-screen bodies freeze (`saved_speed`, `speed = 0`),
  fade their alpha, and destroy themselves when fully faded. Drawing detail
  scales with `segments = sqr(radius)/camera.zoom`. This is how a whole galaxy
  stays affordable — apply the same gating to the gravity loop and any
  procedural-mesh draws for dense belts.
- **Intermittent work via alarms.** Collision checks run on a timer
  (`alarm[0] = 60`), not every frame. The engine equivalent is a simple tick
  accumulator inside a system.
- **Lazy hierarchical spawning.** Stars spawn planets only once the star is in
  view, planets spawn moons/rings likewise — "to prevent all the stars in the
  galaxy from generating planets at once." The same guard belongs on any
  recursive world generation to avoid a spawn storm at load.
- **Quasirandom placement.** `random_halton(i, 3, 360)` distributes moons/rings
  evenly without clustering. A Halton/golden-ratio sequence is a cheap win for
  belt and decoration placement.

---

## 6. Parent/child nesting → components, not inheritance

The GameMaker hierarchy is `o_asteroid → p_space_debris → p_space_matter →
p_selectable`, where each parent adds behavior (selectable, space-matter physics
and spawning, debris). Destination Sol's idiomatic equivalent is **composition**:
instead of one deep parent chain, attach the capabilities a body needs as
components and let systems act on whatever has them. The mapping:

| GameMaker parent | Adds | Destination Sol equivalent |
|------------------|------|----------------------------|
| `p_selectable` | targetable by HUD | `Targetable` / selection component |
| `p_space_matter` | gravity, orbit, spawn, collide | `GravityAffected` / `GravitySource` + `Material` |
| `p_space_debris` | minor debris physics | size/health components |
| `o_asteroid` | the concrete rock | asteroid mesh/prefab + the above |

So the "custom parent/child relationships" survive the port — they just become
**component sets** declared per entity (and per module) rather than a class
tree, which is exactly what makes them composable across modules without an
integration headache.

---

## 7. The built modules

### `modules/gravity`

- `AstroGravity.java` — pure softened-gravity math (from `math_astrogravity.gml`).
- `GravityWell.java` — pooled attractor record.
- `systems/AstroGravitySystem.java` — `UpdateAwareSystem` that pulls asteroids
  toward planets and suns against the live Box2D world.
- `components/Material.java`, `GravityAffected.java`, `GravitySource.java`.
- `AstroGravityTest.java` — JUnit 5 stability tests.

**Wiring is automatic.** `AstroGravitySystem` is annotated
`@RegisterUpdateSystem(priority = 1)`. `SolGame` scans the module environment for
annotated `UpdateAwareSystem`s, instantiates each with its no-arg constructor,
and dependency-injects it — so **no engine edit is needed**. priority 1 runs it
just after the priority-0 default systems (PlanetManager, ObjectManager), so
positions and bodies are current. Tune via the constants at the top of the
system, starting with `FORCE_CONSTANT`.

### `modules/vfx-vector`

- `LightningPath` / `LightningGeometry` — recursive, fading, metal-seeking
  forked lightning (pure, deterministic).
- `TrailRibbon` — tapering path ribbon (pure vertex generator).
- `MetalLocator` — fork-target hook, backed by `Material.isMetallic()`.
- `VectorEffectsDrawer` — thin additive draw layer over `CommonDrawer`.
- `components/Trail`, `components/LightningBolt`.
- `LightningPathTest`, `TrailRibbonTest` — JUnit 5.

These are triggered by gameplay (a weapon fires, a strike lands), so they are
plain library classes plus components rather than an auto-registered update
system; call `VectorEffectsDrawer` from a render hook.

---

## 8. Debugging and testing

Because the game can't be built or run in the authoring environment, correctness
is established two ways, both runnable on your PC:

**Unit tests (pure logic).** Each module ships JUnit 5 tests that pin the
properties that matter — gravity is finite at the center, never NaN, zero past
cutoff, and weakens with distance; lightning always terminates, snaps to its
target, keeps alpha in range, and is deterministic per seed; the trail's
vertices are perpendicular and taper correctly. Run them with:

```
gradlew :modules:gravity:test
gradlew :modules:vfx-vector:test
```

(The same formulas were also validated numerically outside the engine: the
softened curve decays as expected, the lightning walk's worst-case length is ~60
segments for a 200px bolt, and ribbon offsets are exactly perpendicular.)

**Runtime logging (live behavior).** The systems log through the engine's
existing slf4j → log4j2 setup. `log4j2.xml` now has loggers for
`org.destinationsol.modules.gravity` and `.modules.vfx` at DEBUG (set them to
INFO to mute). Output goes to `destinationsol.log` in the project root.

- `AstroGravitySystem` emits a throttled heartbeat every ~2 game-seconds:
  `AstroGravity: wells=…, affectedBodies=…, maxImpulse=…`. If any impulse is
  non-finite it logs a `WARN` instead — the immediate tell of a gravity blow-up,
  so you can catch a bad `FORCE_CONSTANT`/cutoff before it ruins a session.
- `VectorEffectsDrawer` logs a throttled bolt summary (vertex and fork counts).

A good first play-test loop: launch with `run.bat`, fly to an asteroid belt,
`tail` `destinationsol.log`, and confirm `affectedBodies` climbs as rocks come
into range and `maxImpulse` stays small and finite. Then raise `FORCE_CONSTANT`
until the belt visibly bends.

### 8.1 Seeing it in-game (the built-in demo)

So you don't have to go hunting for a belt, two throwaway demo systems make both
features visible the moment a game starts:

- `modules/gravity/.../systems/DemoAsteroidSpawner` — once the hero exists, drops
  a ring of 16 low-velocity asteroids around your ship (and logs
  `Demo: spawned 16 asteroids…` at INFO). With zero starting velocity, the
  `AstroGravitySystem` immediately begins pulling them toward the nearest planet
  or sun, so you can watch them drift.
- `modules/vfx-vector/.../systems/LightningArcSystem` — every ~0.15s it makes the
  nearest on-screen asteroid crackle a blue-white lightning arc to its nearest
  neighbour, forks chasing the nearest "metal" (asteroid). This is the visible
  proof the `vfx` module renders through the engine's `Drawable`/`GameDrawer`
  pipeline (`LightningDrawable`).

- `modules/astro-bodies/.../systems/DemoRockSystem` — drops 6 procedurally-shaped
  rocks near the ship, each a unique random silhouette drawn with its triangle
  fan (logs `Demo: spawned 6 procedural rocks…`). This is the visible proof of
  `AsteroidShape` / `ProceduralRockDrawable`.

All are auto-wired via `@RegisterUpdateSystem` and spawn only normal engine
objects — no engine edit. They're clearly marked as demos; delete any class to
remove it. If you see the `Demo: spawned…` log lines, drifting ring asteroids,
crackling arcs, and procedural rock outlines, the whole pipeline — module
discovery, update registration, gravity, vector VFX, and procedural geometry —
is working end to end.

### 8.1.1 Layers: background landmark vs. playfield

Because a 2D game is faking 3D space, every space body is one of two kinds, and
the kind drives visuals, physics, and danger:

- **Background landmark** (astro-bodies `DemoRockSystem` → `ProceduralRockDrawable`
  on a `FAR_DECO_*` level). Drawn faded and cool-tinted, parallax-scrolling
  slower than the playfield and behind it, fixed in space, with **no gravity and
  no collision**. These are scenery/navigation landmarks. Lightning around them
  is purely cosmetic.
- **Playfield** (engine asteroids). Full gravity (`AstroGravitySystem`), full
  collision, and **hazardous** lightning.

The parallax + fade is what makes it instantly readable which rocks you can hit
and which are distant backdrop, instead of everything sitting on the same plane.

### 8.1.2 Charged-asteroid hazard and the shields-only damage rule

`ChargedAsteroidHazardSystem` gives playfield asteroids a charge that builds over
~5s; when you linger within range of a charged one, it discharges a hot bolt at
your ship. Damage follows the design rule: **lightning damages shields, not
hull.** If your shield is up, the bolt drains it (`DmgType.ENERGY`, which
`Shield.canAbsorb` accepts); if the shield is down or absent, the bolt fizzles
harmlessly — *unless* the target is electrically weak (e.g. a future explosive
barrel), which is the documented extension point for full-damage hits. This
relies on a small engine addition, `SolShip.getShield()`.

### 8.1.3 Lightning visuals

`LightningDrawable` no longer draws fat single lines. Each segment is rendered in
three additive passes — a wide soft glow, a colour-gradient mid layer (hot white
core colour near the source fading to the cool glow colour at the tip), and a
thin bright white core — with the width tapering toward the tip, a radial spark
burst at the impact point, and an `intensity` that scales brightness/width/sparks
so a charging asteroid visibly ramps up. The fully textured triangle-strip ribbon
(`TrailRibbon`, with per-vertex colour) remains the next step once a
`PolygonSpriteBatch` pass is added.

### 8.2 Lightning driven by real gameplay (weapon fire)

Beyond the ambient demo, `modules/vfx-vector/.../systems/WeaponLightningSystem`
wires lightning to an actual game event: it polls the object list for newly
created `Projectile`s near the camera (i.e. when you fire) and arcs a bolt from
the muzzle to the nearest enemy ship or asteroid, forks chasing the nearest
metal. It detects fire purely by spotting fresh `Projectile` objects, so it needs
no engine edit and works with any weapon. This is the template for wiring the
other effects to gameplay — shield strikes (on a `DamageEvent`), engine trails
(on the ship's thruster state), and so on. It logs live projectile and bolt
counts at DEBUG under `org.destinationsol.modules.vfx`.

### Suggested next steps

1. Play-test gravity; tune `FORCE_CONSTANT` until belts bend, watching the log.
2. Give belt-spawned asteroids a tangential orbital velocity (section 2.4).
3. Build `astro-bodies`: procedural perimeter → triangulated mesh → layered
   textures (section 2.5), reusing the `TrailRibbon` "pure-geometry" pattern.
4. Wire a real `MetalLocator` (nearest `Material.isMetallic()` body) and hook
   `VectorEffectsDrawer` into a weapon's render pass.
5. Promote `Material` to a shared component and add the `periodic_table.csv`
   loader (`materials` module).
