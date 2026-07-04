# GMS1 Datamine Plan — porting ideas from D:\Programming\Sandbox\GMS1

Survey of the GameMaker Studio 1 archive for algorithms, effects, and gameplay ideas
worth porting to Destination Sol. Paths below are relative to `D:\Programming\Sandbox\GMS1\`.
GMX projects keep one script per file under `<project>.gmx\scripts\*.gml`, so everything
referenced here is plain-text readable.

## Already ported (for reference — don't redo)

| GMS1 source | DS module |
|---|---|
| `My Projects\Astro Gravity\Astro Gravity 7-16-17.gmx` | `modules/gravity` (AstroGravitySystem, GravityWell) |
| `My Projects\Epic Procedural Lightning.gmx` + `My Projects\Weapon Trail Effects.gmx` | `modules/vfx-vector` (LightningPath, TrailRibbon, MetalLocator) |
| `My Projects\Procedural Asteroids.gmx` | `modules/astro-bodies` (ProceduralRockDrawable) + `engine .../game/asteroid/ProceduralAsteroidDrawable.java` |

## Licensing note

- `My Projects\` — own code ("Ground Effect Games" headers). Safe to port directly.
- `Asset Bundles\`, `Tutorials\` — third-party GMC-forum examples, mostly unlicensed.
  Treat as **idea/algorithm reference only**; reimplement clean-room (DS is Apache 2.0).
- `Games\` — complete third-party games. Ideas and design reference only, never code.

## Phase 1 — quick wins (own code, small scripts, clear landing spots)

### 1. Energy shield visuals — `My Projects\Energy Shields.gmx\scripts\`
- `draw_shield.gml` — textured triangle-strip/fan shield bubble deformed along a path,
  with stretch/twist distortion. Directly portable to a `ShieldDrawable` in
  `modules/vfx-vector` (same pattern as LightningDrawable).
- `damage_shield.gml`, `shield_check_full.gml`, `draw_shield_health.gml` — localized
  shield-impact deformation and health arc display.
- Supporting: `draw_ring_ext.gml`, `draw_crescent.gml`, `path_random_circle.gml`.
- DS landing: shield item rendering (`engine .../game/item/Shield.java` consumers,
  `game/ship/SolShip` draw path). Today shields are just a flat sprite flash.

### 2. Beam weapons + shockwaves — `My Projects\Laser Beams.gmx\scripts\`
- `scr_laserbeam.gml`, `scr_laserbeam_ext.gml` — layered beam rendering (core + glow).
- `scr_shockwave.gml` (also in `My Projects\Shock Glass - Magnifier Shockwave.gmx\scripts\`
  with `scr_magnify.gml`) — expanding distortion ring. The magnify variant needs a
  shader in libGDX; note `docs/todo.txt` already has "try using shaders" under FEATURES.
- DS landing: `engine .../game/projectile/` beam-type projectiles, explosion VFX in
  `game/particle/PartMan.java`.

### 3. Low-discrepancy galaxy placement — `My Projects\Quasirandom Map Generator.gmx\scripts\`
- `scr_halton.gml`, `scr_sobol.gml` — Halton/Sobol quasirandom sequences (tiny, pure math).
- `worley_noise_create.gml`, `scr_get_nearest.gml` — Worley (cellular) noise.
- DS landing: `engine .../world/GalaxyBuilder.java` — `calculateSolarSystemPosition()`
  has an open TODO about annulus placement; Halton-based placement gives even,
  natural-looking system spread. Worley noise is useful for nebula/background chunks
  (`game/chunk/ChunkFiller.java`, and the "BG 3.0 self-repetitive nebulae" todo item).

### 4. Formation flying AI — `Asset Bundles\AI & Pathfinding\formation flying.gmx\scripts\GetFormationSpot.gml`
- Trivial slot-claiming algorithm (leader owns N formation slots, wingmen claim one).
  15 lines; reimplement rather than port.
- Pair with the flocking behavior in `My Projects\school of fish.gmx` (logic lives in
  `objects\*.object.gmx` event XML, not scripts — open the object files).
- DS landing: `engine .../game/input/` pilots (e.g. Guardian/escort behavior); enemy
  wings that fly in formation instead of independently.

## Phase 2 — gameplay systems (bigger design lifts)

### 5. Weapon feel system — `Asset Bundles\Misc\Top Down Weapon System.gmx\scripts\ws_*.gml`
Complete reference design for weapon handling: heat/overheat (`ws_get_heat.gml`,
`ws_weapon_overheated_code.gml`), jamming, recoil-driven accuracy bloom (`ws_shoot.gml`
lines 21–33), burst fire (`ws_shoot_interval*.gml`), per-round reload (`ws_reload*.gml`).
- DS landing: `engine .../game/gun/GunConfig.java` + `game/gun/` — DS has rate/reload
  but no heat, bloom, or burst mechanics. Heat + accuracy bloom would add depth to the
  gun itemization. Idea-port only (third-party code).

### 6. Fire propagation on objects — `My Projects\Particle Starter Kit.gmx\scripts\`
- `scr_flamable.gml`, `scr_firestart.gml`, `scr_firestep.gml`, `scr_firedie.gml`,
  `scr_explosion.gml` — burning state that spreads, consumes, then destroys.
- Same scripts also in `My Projects\Procedural Asteroids.gmx\scripts\` wired to asteroids.
- DS landing: a burn-damage component/system alongside the ECS health systems
  (`engine .../health/`), visuals via `game/particle/PartMan.java`.

### 7. Elliptical orbits — `Asset Bundles\Space\Planetary orbit simulation.gmx\scripts\`
- `orbit_function.gml` — Kepler conic r(θ) = h² / (G·M·(1 + e·cos(θ−ω))); plus
  `orbit_create.gml`, `orbit_recalc.gml`, `polar_plot.gml`.
- DS landing: `engine .../game/planet/Planet.java` (planets currently move on perfect
  circles at constant angular speed). Eccentric belts/comets would be a visible upgrade.
  Also synergizes with `modules/gravity`.

### 8. Atmospheric drag / ballistics — `My Projects\Ballistics.gmx\scripts\aero_*.gml`
- `aero_simulate.gml`, `aero_cd.gml`, `aero_cl.gml`, `aero_re.gml` — drag/lift with
  Reynolds-number-dependent coefficients.
- DS landing: projectile and ship drag inside planet atmospheres
  (`game/planet/PlanetManager`/`Planet.getAtmosphereHeight` consumers). Probably
  simplify to quadratic drag; the full model is overkill.
- (Ignore the `zui_*.gml` UI half of this project — DS already has NUI.)

## Phase 3 — larger ideas (design reference, prototypes)

### 9. In-game ship editor — `My Projects\Ship Creator 3.gmx\scripts\`
- Own code: grid-based hull editor with save/load (`ship_grid_save.gml`,
  `ship_grid_load.gml`, `ship_add_point_*.gml`, `math_centroid_grid.gml`,
  `draw_ship_grid_new.gml`).
- `Asset Bundles\UI & Tools\ShipMaker.gmx` is the Battleships Forever ShipMaker —
  huge and WinAPI-bound (`API_*.gml`), **not portable**, but its module/weapon
  attachment + mirroring UX (`nWepMir.gml`, `cloneGroup.gml`, trigger system `tr_*.gml`)
  is excellent design reference.
- DS landing: player-facing hull designer producing JSON hull configs compatible with
  `engine .../files/HullConfigManager.java` / `game/ship/ShipBuilder.java`.

### 10. Procedural space stations — `Asset Bundles\Space\fractal space stations.gmx\scripts\`
- `create_node.gml`, `draw_node.gml` — recursive node-based station growth. Only two
  scripts; the object events hold the recursion parameters.
- DS landing: procedurally generated stations/mazes (`engine .../game/maze/`), or
  variety for station hulls beyond fixed sprites.

### 11. Planet surface & biome generation
- `Asset Bundles\Space\Planet map.gmx\scripts\` — `scr_diffuse.gml`, `scr_smooth.gml`,
  `scr_create_river.gml`, `scr_resources.gml` — 2D map diffusion/erosion + resource
  seeding.
- `Asset Bundles\Space\Planet Model 13 distro.gmx\scripts\` — hex-sphere planet; the 3D
  half is irrelevant, but the biome pipeline is portable:
  `generate_height_noise.gml`, `generate_temperature.gml`, `generate_wetness.gml`,
  `assign_terrain.gml`, `get_terrain_properties.gml` (height×temp×wetness → biome).
- `Asset Bundles\Terrain\fractal_terrain_generator.gmx`, `TerrainGen.gmx`,
  `worldgenerator.gmx` — heightline generation alternatives.
- DS landing: `engine .../game/planet/GroundBuilder.java`, `PlanetObjectsBuilder.java` —
  biome-driven tile/decoration/enemy selection per planet instead of single-config planets.

### 12. Nebula backgrounds — `Asset Bundles\Effects & Drawing\NebulaDraw.gmx\scripts\`
- DLL-backed (`DLLInit.gml`) so the core is a black box, but `grid_smooth.gml` and
  `grid_merge_colors.gml` are portable, and the output style matches the
  `docs/todo.txt` "BG 3.0: create self-repetitive nebulae" item. Combine with Worley
  noise from item 3.

## Worth a look later (unassessed but promising)

- `My Projects\Epic Procedural Terrain.gmx` — companion to the lightning/asteroid family.
- `My Projects\Holy Grid.gmx` — the `holygrid_*.gml` grid-distortion scripts also appear
  in Energy Shields/Particle Starter Kit; springy grid backgrounds (Geometry Wars style).
- `My Projects\Image Assemble 11-19-16.gmx` — sprite-assembly; possibly ship-part composition.
- `My Projects\Invisibility Cloak.gmx`, `Shard of Light.gmx` — shader-style effects.
- `Asset Bundles\Effects & Drawing\Metaballs_01.gmx` — blob rendering (plasma weapons?).
- `Asset Bundles\Misc\destruct_engine.gmx\scripts\destroy.gml` — pixel-level destruction
  (relates to `docs/todo.txt` "damaged ship hulls in place of exploded ships").
- `Asset Bundles\UI & Tools\Random Sentence Generator.gmx` — logic in object events;
  possible flavor-text/mission-text generator.
- `Games\Space\*` (Super Space Rogues, SciFi Engine, Leviathan, Backspace…) and
  `Games\Misc\Faction Wars.gmx` — design/mechanic inspiration only.

## Skip

- `My Projects\CORE Project` — GM7-era `.gmk` binaries with a pile of DLLs; not minable as text.
- `ShipMaker.gmx` code (WinAPI-bound; design reference only, see item 9).
- `Asset Bundles\Lighting\*`, `Water & Fluid\*` — little relevance to a 2D space game.
- `_Duplicates (removed)`, `_Unsorted (review)` — empty/duplicate holding pens.

## GML → Java porting notes

- Scripts use positional `argumentN` params; headers document the signature — read the
  first comment line of each `.gml` first.
- GM `path_*` polylines map to `LightningPath` in `modules/vfx-vector` — reuse it as the
  general polyline class rather than inventing another.
- GM draw primitives (`draw_primitive_begin_texture`, `draw_vertex_texture_colour`) map
  to libGDX `Mesh`/`PolygonSpriteBatch`; `VectorEffectsDrawer` in vfx-vector is the
  established pattern.
- GM `alarm[n]` timers map to per-system countdown fields updated with `timeStep`.
- `tolerance(x)` (appears everywhere in My Projects) is just `random(-x, +x)` —
  `SolRandom.randomFloat(x)`.
