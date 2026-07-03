# DestinationSol Fork — TODO

Priority order for a test pass:

#720 — spawn into station after save/load (most likely to have an observable edge case)
#731 — fire rapidly at an enemy, kill them, check rep gained vs. expected; verify it works for each major faction pair
#732 — open shop, note price, fly away, return, confirm price re-randomized; test buy with exactly enough money at 1.35× price
#722 — equip a weapon with isUnderneathHull: true and verify it renders under own hull but visible from outside when docked to a station
#616/#712 — sell/drop the last item in inventory via both keyboard and mouse

Compiled from upstream issues and open PRs at [MovingBlocks/DestinationSol](https://github.com/MovingBlocks/DestinationSol) as of 2026-06-17.

---

## Upstream PRs to Cherry-pick / Review

These are open PRs in the upstream repo that haven't been merged yet. Each is a self-contained fix worth evaluating for this fork.

| PR | Title | Fixes |
|----|-------|-------|
| [#712](https://github.com/MovingBlocks/DestinationSol/pull/712) | Inventory UI fixes (crash on drop shield, crash on keyboard nav after all sold, highlight jumps to nearest remaining item) | #711 |
| [#724](https://github.com/MovingBlocks/DestinationSol/pull/724) | `UIAnimatedImage` widget — renders animated sprites in NUI; fixes menu background ship animation | #723 |
| [#730](https://github.com/MovingBlocks/DestinationSol/pull/730) | Reputation loss proportional to damage dealt (not constant per-hit); implements destroy penalty that was referenced but never applied | #729 |
| [#700](https://github.com/MovingBlocks/DestinationSol/pull/700) | Prevent saving during the tutorial so it can't overwrite the main-game save | #699 |
| [#728](https://github.com/MovingBlocks/DestinationSol/pull/728) | Steam Workshop support — JSON delta for NUI files, `DesktopLauncher` refactor, Steam facade extracted to separate repo | — |

> Note: PR #734 (Bifrost) is marked POC-only; skip for now.

---

## Bugs to Fix

### Crash / Blocker

- [x] **#720** — Docking a ship at a station, saving, then continuing causes a collision (instant death / ejection). Ship spawn position on load collides with the docked station.
- [x] **#719** — `"bound must be positive"` crash when `planetsConfig.json` lacks one of each difficulty tier (easyOnly, hardOnly, general). Game calls `Random.nextInt(0)`.
- [x] **#718** — `imperialMedium`'s rigid body crashes the main menu when inserted into `menuBackgroundShipConfig.json`. Silent crash with no error in log.
- [x] **#715** — Unexpected engine equipping behavior when swapping ships. Engines get orphaned or double-equipped after the `make engines equippable` change (#704).
- [ ] **#701** — Crash at end of tutorial.

### Gameplay / Logic

- [x] **#725** — Guards defined inside `temporaryEnemies` entries are silently ignored — no escorts spawn. The `guard` field is parsed for regular enemies but not wired up for temporary enemy spawn paths.
- [x] **#729** — Reputation loss is constant per-hit regardless of weapon damage (addressed by PR #730 above; track separately if not cherry-picking the whole PR).
- [ ] **#722** — Weapon sprites with `isUnderneathHull: true` render behind *other ships' hulls* too (not just their own), making them invisible while docked.
- [x] **#721** — Disabling a module that overrides `playerSpawnConfig` does not revert the ship-selection screen to the default ships. Stale config reference causes `NullPointerException` on new game.
- [x] **#699** — Starting a tutorial overwrites the main-game save (addressed by PR #700 above; standalone if not cherry-picking).

### Visual / UI

- [ ] **#723** — Spritesheets shown instead of animations outside of gameplay (main menu background ships, ship preview). Addressed by PR #724.
- [x] **#616** — Minor UI quirks: keyboard navigation out-of-bounds crash and null-item equip crash fixed.
- [x] **#614** — If the ship is turning to follow the mouse cursor when the map is opened, it keeps spinning until the map is closed. Input state not cleared on map open.
- [x] **#567** — Getting crushed between a starport and a hub can result in negative HP. HP should floor at zero on crush damage.
- [ ] **#572** — Starport map sprites don't line up with their actual positions.

### Older / Lower Priority

- [ ] **#658** — Wormholes grab trading posts and drag them away from planets.
- [ ] **#562** — Massive lag when many smoke/fire particles are active.
- [ ] **#560** — Problems with transport gates on the map ($10 gates).
- [x] **#535** — Starports sometimes push each other around.
- [ ] **#488** — Imperial ships don't fight back when starting as `Imperial Small`.

---

## Feature Requests to Consider

These are enhancement issues from upstream. Priority ordered by how much they interact with the factions/engine work already in this fork.

### High Relevance (builds on recent faction/AI changes)

- [x] **#731** — Gain reputation with a faction by killing its enemies. Requires defining enemy factions in faction config and applying a bonus on enemy-kill events. Works well with the factions rework (#703).
- [x] **#729 / #730** — Reputation loss proportional to damage (already in PR #730; low-hanging fruit).
- [ ] **#726** — Improve fixed-weapon aiming for ally/enemy ship AI. Currently enemies with fixed guns don't lead their shots accurately.
- [ ] **#727** — Ship AI as an equippable item (`ai/imperialAI/imperialAI.json`) with params like `fixedGunAccuracy`, `firingFrequency`, `preferredDistanceFromPlayer`. Large architectural change but unlocks per-faction AI tuning.

### Medium Relevance

- [ ] **#733** — Flexible loot drop definitions on enemies (explicit drop tables with weights/quantities instead of inheriting equipped items only).
- [x] **#732** — Variable item prices per station/system, with optional price fluctuation (`variability` field). Foundation for a trading loop.
- [x] **#705** — Allow slight drift movement when no engine is equipped (instead of complete stop), so ships feel physical even when engineless.
- [ ] **#717** — Electronic Counter Measures ability idea — jams enemy targeting/weapons temporarily.

### Low Priority / Long-term

- [ ] **#628** — Improve the in-game tutorial content and flow.
- [ ] **#532** — Engine sound effects and visual effects (exhaust plumes, etc.).
- [ ] **#429** — Event theming system — seasonal or triggered world-state events.

---

## Code Quality / Architecture

- [ ] **#659** — Follow-up cleanup items from the Gestalt-DI migration. Unfinished wiring and leftover legacy patterns.
- [ ] **#617** — Refactor ECS rubble further. Current implementation is an intermediate state.
- [ ] NUI skin/widget warnings on startup: `UISkinFormat.java:150` fails to resolve `UIInputBind` (skips its style info), and `UIFormat.java:236` doesn't recognize the `image` field on `InteractHint`. Pre-existing, unrelated to the Java 25 migration.
- [ ] **#416** — Migrate desktop logging from log4j to Logback/SLF4J (PR #501 is a stale attempt at this).
- [ ] **#590** — ECS wrapper for the sound system (open PR, no reviewer activity).
- [ ] **#648** — Expand the API surface exposed to modules (open PR by BenjaminAmos).

---

## Documentation / Distribution

- [ ] **#714** — Add supported language list to F-Droid and Google Play store descriptions.
- [ ] **#657** — Update README with F-Droid link (open PR, trivial merge).
- [ ] **#419** — Box art for Twitch, GOG, and Steam store pages.
