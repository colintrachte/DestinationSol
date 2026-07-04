# Plan: Replace SecurityManager-based module sandboxing

## Background

DestinationSol's module system (via `org.terasology.gestalt:gestalt-module`) used to
sandbox module code with a JVM `SecurityManager` (`ModuleSecurityManager` +
`ModuleSecurityPolicy`, installed conditionally via
`FacadeModuleConfig.useSecurityManager()`). JDK 24 (JEP 486) permanently disallows
installing a `SecurityManager` with no opt-back-in flag, so `useSecurityManager()`
was already hardcoded to `false` in both `DesktopModuleConfig`
([desktop/src/main/java/org/destinationsol/desktop/SolDesktop.java](../desktop/src/main/java/org/destinationsol/desktop/SolDesktop.java))
and `TestModuleConfig`
([engine/src/test/java/org/destinationsol/testsupport/TestModuleConfig.java](../engine/src/test/java/org/destinationsol/testsupport/TestModuleConfig.java))
as part of the JDK 25 migration. That means the SecurityManager-based sandbox is
already 100% inert — this plan restores the lost functionality with a mechanism
that works under JDK 25+.

**Threat model (confirmed with user):** defense against buggy/careless community
modules, not adversarial/malicious code. This rules out heavy isolation (separate
process/JVM, OS sandboxing) as disproportionate — an in-process fix is the right
scope.

## Current state (verified against gestalt-module 8.0.0-SNAPSHOT sources)

Gestalt's sandbox has two independent layers:

1. **Class visibility** — `JavaModuleClassLoader.loadClass()` calls
   `PermissionProvider.isPermitted(Class)` directly. **Still works**, no
   `SecurityManager` involved. This is what `API_WHITELIST`/`CLASS_WHITELIST` in
   [engine/src/main/java/org/destinationsol/modules/ModuleManager.java](../engine/src/main/java/org/destinationsol/modules/ModuleManager.java)
   drive.
2. **Operation-level checks** — `PermissionSet.isPermitted(Permission, Class)` is
   only ever invoked by `ModuleSecurityManager.checkPermission()`, which requires
   an installed `SecurityManager`. **Permanently unreachable now.**

**The concrete hole:** `API_WHITELIST` blanket-whitelists `java.lang` (needed for
almost everything modules do), so modules can currently call `System.exit()`,
`Runtime.exec()`/`ProcessBuilder`, `System.load()`/`setProperty()`,
`AccessibleObject.setAccessible()`, spawn unbounded `Thread`s — nothing stops
them. Class-level whitelisting can't distinguish `System.currentTimeMillis()`
(needed) from `System.exit()` (not) since both live on the same whitelisted
class.

## Recommended approach: narrow whitelist + call-site instrumentation

Two changes, both in-process, no new JVM/OS boundary:

### 1. Shrink the whitelist, add a facade for what's lost

Remove blanket `java.lang` from `API_WHITELIST` in `ModuleManager.java`; keep
only the specific safe classes modules actually need. Add a small first-party
facade class (e.g. `org.destinationsol.modules.api.SystemFacade`) exposing the
handful of legitimate `System`/`Runtime` calls modules use (`currentTimeMillis`,
`arraycopy`, `identityHashCode`), and add that facade to `CLASS_WHITELIST`
instead of the raw JDK classes.

### 2. Add a `BytecodeInjector` denylist for dangerous calls that survive on needed classes

Gestalt already has the hook for this — no library fork needed:

- `JavaModuleClassLoader`'s constructor takes `Iterable<BytecodeInjector> injectors`
  and runs javassist `CtClass` transforms at class-load time
  (`findClass()`), independent of `SecurityManager`.
- `BytecodeInjector` is a one-method interface: `void inject(CtClass cc)`.

Write `DangerousApiInjector implements BytecodeInjector` using javassist's
`ExprEditor` / `CtMethod.instrument()` to find calls to a denylist and rewrite
them to throw a `ModuleSandboxViolationException` instead of proceeding.
Denylist candidates:

- `Runtime.exec*`, `ProcessBuilder.start`
- `System.load` / `System.loadLibrary`
- `AccessibleObject.setAccessible`
- `Thread.stop`
- raw `new Thread(...).start()` (optional — cap concurrency)
- `ClassLoader` subclass construction

### Wiring

`FacadeModuleConfig.getClassLoaderSupplier()` currently returns
`JavaModuleClassLoader::create` (the no-injector static factory) in both
`DesktopModuleConfig` and `TestModuleConfig`. Replace with a lambda that builds
the module's classpath `URL[]` (same logic `JavaModuleClassLoader.create`
already does internally) and constructs:

```java
new JavaModuleClassLoader(module.getId(), urls, parent, permissionProvider,
    List.of(new DangerousApiInjector()))
```

### Cleanup

`useSecurityManager()` is fully dead — delete it from `FacadeModuleConfig`,
`DesktopModuleConfig`, and `TestModuleConfig` rather than leaving an inert flag.

## Phasing

1. Add `DangerousApiInjector` + `ModuleSandboxViolationException`, unit-test in
   isolation (construct a `JavaModuleClassLoader` over a test module class with
   a denylisted call, assert it throws).
2. Narrow `API_WHITELIST`/`CLASS_WHITELIST`, add the `SystemFacade` class, wire
   the new class loader supplier in `DesktopModuleConfig` and `TestModuleConfig`.
3. **Audit existing modules** (`modules/astrobodies`, `modules/core`,
   `modules/gravity`, `modules/vfx-vector`) for any current use of soon-removed
   `java.lang` members, to avoid silently breaking shipped content — this is the
   main risk in the whole plan.
4. Delete `useSecurityManager()` and any now-fully-unused permission-set
   scaffolding it implied.

## Key files

- `engine/src/main/java/org/destinationsol/modules/ModuleManager.java` —
  `API_WHITELIST` / `CLASS_WHITELIST`, `loadEnvironment()`
- `engine/src/main/java/org/destinationsol/modules/FacadeModuleConfig.java` —
  interface: `getClassLoaderSupplier()`, `useSecurityManager()` (to remove),
  `getAPIClasses()`
- `desktop/src/main/java/org/destinationsol/desktop/SolDesktop.java` —
  `DesktopModuleConfig`
- `engine/src/test/java/org/destinationsol/testsupport/TestModuleConfig.java` —
  test-side config, same pattern
