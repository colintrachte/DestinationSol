# Plan: Republishing this fork as your own game

Date: 2026-07-02

This is a working plan for taking this fork — after the modernization and
gameplay work in [MODERNIZATION_PLAN.md](../MODERNIZATION_PLAN.md) and
[todo.md](../todo.md) — to a public, possibly commercial, release under your
own name rather than as "Destination Sol."

It is not legal advice. It's the licensing homework done up front so the
actual work (renaming, re-asseting, store setup) has a clear checklist and
nothing gets discovered late.

---

## 1. What you can already do freely

The **code** is Apache 2.0 ([LICENSE](../LICENSE)), copyright Milosh Petrov
(2014-2015) and The Terasology Foundation (2015-2020), per [NOTICE](../NOTICE).
Apache 2.0 is permissive:

- Modify, rebrand, and sell it commercially — allowed.
- No obligation to open-source your changes (unlike GPL).
- Only real obligations: keep the copyright notice, ship the license text,
  and carry forward the NOTICE file's attributions somewhere reasonable
  (an in-game "credits" or "third-party licenses" screen is the normal way).

So the engine, gameplay code, and your own modules (`gravity`, `vfx-vector`,
`astro-bodies`, etc.) are yours to republish as-is.

## 2. What is NOT freely reusable

### Soundtrack — blocks commercial release as-is

[LICENSE_SOUNDTRACK](../LICENSE_SOUNDTRACK) is CC BY-NC 4.0
("NonCommercial" — Section 2(a)(1)(A)), by NeonInsect. This is a hard
blocker for a paid/monetized release with the stock soundtrack included.

- [ ] Either commission/license new music, use a royalty-free commercial
      track pack, or reach out to NeonInsect for a separate commercial license.
- [ ] Do not ship the existing `.ogg` soundtrack files in a commercial build
      until one of the above is resolved.

### Sprites (MillionthVector) — reusable, but attribution required

CC BY 4.0, credited in [README.md](../README.md) contributors section.
Commercial use is fine; attribution must be kept (credits screen / store
page / manual — any reasonable place).

- [ ] Decide where attribution lives in the rebranded release (credits menu
      is simplest) and carry the MillionthVector + NeonInsect (if music kept
      non-commercially, e.g. a free version) attributions forward.

### "Destination Sol" name, logo, and store presence — the real risk

Apache 2.0 explicitly does **not** grant trademark rights (Apache 2.0 §6).
The name "Destination Sol" and its logo are still actively sold by the
original rights holders on [Steam](http://store.steampowered.com/app/342980/)
and Google Play. Reusing them risks:

- A trademark complaint from the rights holder.
- Steam/Google Play flagging a new listing as a confusing duplicate of an
  existing one (a platform-policy problem, separate from the legal one).

- [ ] Pick a new game name and logo before any public/commercial listing.
- [ ] Do not reuse `mainMenuLogo.png` / `readMeLogo.png` or "Destination Sol"
      branding in store assets, splash screens, or marketing.
- [ ] Update in-game strings, window title, `module.json`/`build.gradle`
      artifact names, and package/bundle identifiers away from
      `destinationsol`/`miloshpetrov.sol2` where they're user-visible or
      used as a store bundle ID.

## 3. Action checklist, in order

1. **Legal cleanup**
   - [ ] Resolve soundtrack licensing (swap or license) before any paid build.
   - [ ] Keep Apache 2.0 NOTICE + license text bundled with the app (about/credits screen).
   - [ ] Keep MillionthVector attribution wherever assets are still used.
2. **Rebrand**
   - [ ] New name, new logo, new store page copy/art.
   - [ ] Sweep for "Destination Sol" / "Sol" branding in UI strings, window
         title, README, package/bundle IDs, Steam/Play store metadata.
3. **Technical rename** (mechanical, do after gameplay work stabilizes so
   diffs stay reviewable)
   - [ ] Rename Gradle project/module identifiers and Android/desktop bundle IDs.
   - [ ] Update save-file directory name (currently tied to the old identifiers)
         so it doesn't collide with an existing Destination Sol install.
   - [ ] New app icon / splash assets.
4. **Store setup**
   - [ ] New Steam/Google Play listing under the new name (do not attempt to
         reuse or "take over" the existing listing — it belongs to the
         current rights holder).
   - [ ] Store page must not imply affiliation with or endorsement by the
         original Destination Sol team (Apache 2.0 explicitly disclaims this
         too, for the code itself).

## 4. Open questions to revisit

- How much of the gameplay/content (planets, factions, ship designs) counts
  as "expression" you'd want to differentiate anyway for a distinct
  identity, versus code you're keeping as-is. Worth a pass once the
  modernization work is stable.
- Whether to keep any MillionthVector sprites long-term or fully re-asset
  for a distinct visual identity — attribution is fine either way, this is
  a creative call, not a legal one.
- Get an actual lawyer's sign-off before a real commercial launch — this
  doc is scoped to "don't get blindsided," not a substitute for that.
