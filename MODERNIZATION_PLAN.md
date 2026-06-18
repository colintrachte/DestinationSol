# Destination Sol — Modernization Plan & Dependency Audit

Target: **Java 25 (LTS)** · **Gradle 9.5.1** · full dependency overhaul
Date: 2026-06-18

---

## 1. The big picture

This project is pinned to Java 8 bytecode running on a Java 11 runtime, built with Gradle 7.6
and a set of custom Groovy "convention plugins" in `build-logic/`. Modernizing means moving four
things at once, in dependency order:

1. The **build tool** (Gradle 7.6 → 9.5.1)
2. The **Java target** (8 → 25)
3. The **build/quality plugins** (grgit, spotbugs, pmd, idea-ext, download-task)
4. The **application libraries** (LibGDX, gestalt, guava, gson, logging, crash reporter)

We cannot build or run the game inside the assistant's sandbox (Windows desktop app, native LWJGL
libraries, external Terasology repos). So the loop is: **assistant edits build files → you run on your
PC → report results → iterate.** Every phase below ends in a build you run.

---

## 2. Dependency audit

### Build tooling

| Component | Current | Latest | Notes |
|---|---|---|---|
| Gradle wrapper | 7.6 | **9.5.1** | You already downloaded 9.5.1. Gradle 9 requires Java 17+ to run and supports running on Java 25 (since 9.1). |
| `org.ajoberstar.grgit` | 5.0.0 | **5.3.3** | The exact thing crashing your build today. 5.3.x restores Gradle 9 compatibility. Project is "feature frozen" but maintained for new Gradle versions. |
| `com.github.spotbugs.snom:spotbugs-gradle-plugin` | 5.0.13 | **6.5.6** | Major bump (5.x→6.x); API for report config changed. |
| `de.undercouch:gradle-download-task` | 5.3.0 | **5.7.0** | Low-risk minor bump. |
| `org.jetbrains.gradle.plugin.idea-ext` | 1.1.7 | **1.4.1** | Used heavily for the IntelliJ XML patching in `build.gradle`/`destination-sol-ide.gradle`. |
| PMD (`pmd-core`/`pmd-java`) | 6.15.0 | **7.25.0** | PMD 7 is a big rewrite; ruleset XML in `config/metrics/pmd` may need edits. |
| SpotBugs tool | 4.0.0 | ~4.9.x | Bump alongside the gradle plugin. |
| Checkstyle / JaCoCo | via TeraConfig zip 1.6.3 / jacoco 0.8.5 | newer | Config pulled from Terasology's `codemetrics` zip; may lag. |

### Application libraries

| Library | Current | Latest | Notes |
|---|---|---|---|
| LibGDX (`com.badlogicgames.gdx`) | 1.12.1 | **1.14.2** | Core engine. 1.14.1 had breaking collection changes; 1.14.2 reverted them. Target 1.14.2. |
| gdx-controllers | 2.2.3 | check matrix | Versioned independently; pick the version matching LibGDX 1.14.x from the gdx-controllers compatibility wiki. 2.2.3 may already be fine. |
| **gestalt** (`org.terasology.gestalt`) | **8.0.0-SNAPSHOT** | public latest is 7.x | ⚠️ **Highest risk.** This is an unreleased snapshot on Terasology's Artifactory, not a public release. Its annotation processors run *inside* our build — if it wasn't built for Java 25 it can set our ceiling. See Risks. |
| guava | 30.1-jre | **33.6.0-jre** | Safe, widely compatible. |
| gson | 2.6.2 | **2.14.0** | Safe. |
| slf4j-api | 1.7.25 | 2.0.x | Move to slf4j 2.x as part of logging modernization. |
| **slf4j-log4j12 → log4j 1.x** | 1.7.25 | — | ⚠️ **EOL & insecure.** log4j 1.x is unmaintained. Replace with log4j2 or logback. Requires migrating `log4j-debug.properties` and the `-Dlog4j.configuration=...` JVM args. |
| crashreporter (`cr-destsol`) | 4.0.0 | check Terasology | Terasology-hosted; verify newest. |
| Bundled JRE (Bellsoft Liberica) | 11.0.19+7 | a 25.x JRE | In `destination-sol-jre.gradle`; the distribution bundles this. Must move to a 25 JRE and the download URLs/filenames may have changed. |

---

## 3. Key risks (read before starting)

1. **gestalt 8.0.0-SNAPSHOT is the linchpin.** The engine's entity system + dependency injection
   depend on gestalt's annotation processor, which executes under the JDK that compiles the project.
   If that snapshot can't run on JDK 25, we either (a) find a gestalt build that can, or (b) keep the
   *compile toolchain* at 17/21 while still running Gradle on 25. We'll only know by testing Phase 3.

2. **Gradle 9 removed APIs the convention plugins use.** Notably `$buildDir` (used in
   `gestalt-module.gradle` and `terasology-metrics.gradle`) is removed in Gradle 9 — replaced by
   `layout.buildDirectory`. Expect a round of "this API no longer exists" fixes in Phase 2.

3. **Java 8 → 25 is 17 versions of removed/changed APIs.** Most game code will be fine, but watch for
   removed `sun.*` internals, `Integer(int)` constructors, and anything reflective. Annotation
   processing and the older libraries are the usual sources of breakage.

4. **log4j 1.x migration touches config, not just deps.** The `.properties` log config and the splash
   JVM args reference log4j 1.x; switching backends means rewriting those.

5. **PMD 6 → 7 and SpotBugs 5 → 6** are major bumps with changed config schemas. Since both are set to
   `ignoreFailures = true`, we can afford to get them building and tune rules later.

---

## 4. Phased execution order

Each phase is independently testable. We do *not* move to the next until the current one builds for you.

- **Phase 1 — Tooling floor.** Wrapper → 9.5.1; bump grgit/spotbugs/idea-ext/download-task/pmd in
  `build-logic`; introduce a Java **toolchain** set to 25. Goal: `gradlew help` configures.
- **Phase 2 — Gradle 9 API fixes.** Replace `$buildDir` and any other removed APIs until configuration
  succeeds across all modules. Goal: `gradlew projects` / `gradlew tasks` works.
- **Phase 3 — Language level 8 → 25.** Flip `sourceCompatibility`/`targetCompatibility` and IDE level;
  resolve compile errors. This is where the gestalt-on-25 question gets answered. Goal: `gradlew engine:compileJava`.
- **Phase 4 — Application libraries.** LibGDX 1.14.2, gdx-controllers, guava, gson, crash reporter.
  Goal: `gradlew run` launches the game.
- **Phase 5 — Logging.** slf4j 2.x + log4j2/logback; migrate config + JVM args. Goal: clean logs, no log4j 1.x.
- **Phase 6 — Hardening.** Resolve/pin the gestalt snapshot, add a `libs.versions.toml` version catalog
  (single place for all versions), refresh `Jenkinsfile`/CI and the bundled-JRE download. Goal: reproducible modern build.

---

## 5. Recommended target versions (summary)

```
Gradle           9.5.1
Java toolchain   25
grgit            5.3.3
spotbugs-plugin  6.5.6
download-task    5.7.0
idea-ext         1.4.1
pmd              7.25.0
libGDX           1.14.2
guava            33.6.0-jre
gson             2.14.0
slf4j            2.0.x  (+ log4j2 or logback backend)
gestalt          TBD — verify a Java-25-capable build exists
```

---

## 6. Rollback safety

The project is a git repo. Before each phase we work on a branch and commit per phase, so any step is
revertible with `git checkout` / `git reset`. Nothing here is destructive to your source.
