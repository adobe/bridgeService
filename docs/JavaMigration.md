# Java Version Migration — Research & Plan (Issue #25)

BridgeService is currently compiled to Java 11. This document captures the complexity analysis and recommended migration path for supporting other Java versions, both in the IBS runtime and in host projects.

---

## Integration Models

The README defines two ways to integrate IBS (see [Implementing The Bridge Service in Your Project](../README.md#implementing-the-bridge-service-in-your-project)):

- **Injection Model** *(recommended)*: IBS is added as a Maven compile-scope dependency to the host project. The host's JVM loads IBS classes directly. Example: **v6SOAPAPI** (`Adobe-Campaign/pom.xml`).
- **Aggregator Model**: The host project is added as a Maven dependency to IBS. IBS's own JVM loads the host's classes via reflection.

This distinction is critical for Java version compatibility — which JVM loads whose bytecode determines what breaks.

---

## Compatibility Matrix

The two axes are:
- **IBS version + framework** — what IBS is compiled for and what HTTP framework it uses
- **Exposed project version** — the Java version of the project whose classes IBS exposes via reflection

Legend: ✅ Works &nbsp;|&nbsp; ⚠️ Works with workarounds &nbsp;|&nbsp; ❌ Fails

### Injection Model

The exposed project's JVM runs IBS. Both the bytecode and the HTTP framework must be compatible with that JVM.

| IBS version + framework | Exposed Java 8 | Exposed Java 11 | Exposed Java 17 | Exposed Java 21 |
|---|:---:|:---:|:---:|:---:|
| **Java 11 + Spark** (pre-3.12) | ❌ ¹ | ✅ | ⚠️ ² | ❌ ³ |
| **Java 11 + Javalin 6** *(current)* | ❌ ¹ | ✅ | ✅ ⁴ | ✅ ⁴ |
| **Java 17 + Spring Boot 3.x** | ❌ ¹ | ❌ ¹ | ✅ ⁴ | ✅ ⁴ |
| **Java 17 + Javalin 6** | ❌ ¹ | ❌ ¹ | ✅ ⁴ | ✅ ⁴ |
| **Java 17 + Javalin 7** | ❌ ¹ | ❌ ¹ | ✅ ⁴ | ✅ ⁴ |
| **Java 21 + Spring Boot 3.x** | ❌ ¹ | ❌ ¹ | ❌ ¹ | ✅ ⁴ |
| **Java 21 + Javalin 6** | ❌ ¹ | ❌ ¹ | ❌ ¹ | ✅ ⁴ |
| **Java 21 + Javalin 7** | ❌ ¹ | ❌ ¹ | ❌ ¹ | ✅ ⁴ |

¹ Exposed project JVM cannot load IBS bytecode — `UnsupportedClassVersionError`.  
² Bytecode loads fine on Java 17 JVM, but Spark is unofficial on Java 17 and needs `--add-opens` flags.  
³ Bytecode loads fine on Java 21 JVM, but Spark is not compatible with Java 21.  
⁴ Requires `setAccessible(true)` fix in `CallContent.java:171-172` (Java 17+ strong encapsulation).

### Aggregator Model

IBS runs its own JVM and loads the exposed project's classes via reflection. The HTTP framework runs on IBS's JVM; the exposed project's bytecode must be loadable by that JVM.

| IBS version + framework | Exposed Java 8 | Exposed Java 11 | Exposed Java 17 | Exposed Java 21 |
|---|:---:|:---:|:---:|:---:|
| **Java 11 + Spark** (pre-3.12) | ✅ | ✅ | ❌ ⁵ | ❌ ⁵ |
| **Java 11 + Javalin 6** *(current)* | ✅ | ✅ | ❌ ⁵ | ❌ ⁵ |
| **Java 17 + Spring Boot 3.x** | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ | ❌ ⁵ |
| **Java 17 + Javalin 6** | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ | ❌ ⁵ |
| **Java 17 + Javalin 7** | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ | ❌ ⁵ |
| **Java 21 + Spring Boot 3.x** | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ |
| **Java 21 + Javalin 6** | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ |
| **Java 21 + Javalin 7** | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ |

⁴ Requires `setAccessible(true)` fix in `CallContent.java:171-172` (Java 17+ strong encapsulation).  
⁵ IBS JVM cannot load the exposed project's class files — `UnsupportedClassVersionError` in `IntegroBridgeClassLoader.defineClass()`.

---

## Scenario A — IBS runs inside a host project with a Higher Java version

*Injection Model: host is Java 17+, IBS is Java 11.*

| # | File | Line | Issue | Severity |
|---|------|------|-------|----------|
| 1 | `CallContent.java` | 171 | `getDeclaredConstructor().newInstance()` — no `setAccessible(true)`. Java 17+ strong encapsulation throws `InaccessibleObjectException` if the class's package is not opened. | **Critical** |
| 2 | `CallContent.java` | 172 | `l_method.invoke(ourInstance, ...)` — no `setAccessible(true)`. Same module-encapsulation risk for non-public methods. | **High** |
| 3 | `IntegroBridgeClassLoader.java` | 69–73 | `defineClass()` reads raw bytecode. If the host's classes are compiled to Java 17+ (class file version 61+), loading them into a Java-11-compiled IBS context throws `UnsupportedClassVersionError`. | **High** |
| 4 | CI workflows | `onPushSimpleTest.yml:37` | Only Java 11 is tested. `maven-pr-analyze.yml` uses Java 17 for SonarCloud but does not run tests — Java 17 breakage goes undetected. | **Medium** |

**Required fixes:**

1. Add `setAccessible(true)` in `CallContent.java:171-172` before `newInstance()` and `invoke()`. Catch `java.lang.reflect.InaccessibleObjectException` and map it to `JavaObjectInaccessibleException`.
2. Add a CI matrix in `onPushSimpleTest.yml` to test on Java 11 and 17.
3. Switch `pom.xml` from `source/target` properties to `<release>` in the maven-compiler-plugin. Pin `maven-compiler-plugin` ≥ 3.11.0.

---

## Scenario B — IBS hosted in a project with a Lower Java version (Java 8)

*Injection Model: host requires Java 8.*

| # | File | Lines | Issue |
|---|------|-------|-------|
| 1 | `JavaCalls.java` | 235 | `String.isBlank()` — Java 11 API only. |
| 2 | `MCPRequestHandler.java` | 287, 312, 362, 369, 387 | `String.isBlank()` — 5 occurrences. |
| 3 | `ReleaseNotes.md` | ~76 | Claims "Java 8 is also available" — outdated. Current code is not Java 8 compatible. |

**Recommendation**: Officially drop Java 8 support. Java 11 is the minimum runtime. Fix the outdated claim in `ReleaseNotes.md`. No code changes needed.

---

## Scenario C — IBS itself upgraded to Java 17

Java 17 is the recommended upgrade target (see Scenario D for why, not Java 21).

### Impact on the Injection Model (v6SOAPAPI)

A Java 11 host JVM cannot load class files compiled to Java 17 (class file version 61). This breaks v6SOAPAPI at:

- **Compile time** (if host build JDK is 11): `javac` fails with `class file has wrong version 61.0, should be 55.0`.
- **Runtime** (if host build JDK is 17 but host JVM is 11): `UnsupportedClassVersionError` when IBS classes are first loaded.

**Solution — Two separate JARs (Maven classifier):**

Publish two artifacts from the same source:

| Artifact | Target | Who uses it |
|----------|--------|-------------|
| `integroBridgeService-3.x.x.jar` (default) | `<release>11` | Java 11 hosts (e.g. v6SOAPAPI, unchanged) |
| `integroBridgeService-3.x.x-java17.jar` (classifier `java17`) | `<release>17` | Java 17+ hosts |

Java 17 hosts declare:
```xml
<dependency>
    <groupId>com.adobe.campaign.tests.bridge.service</groupId>
    <artifactId>integroBridgeService</artifactId>
    <version>3.x.x</version>
    <classifier>java17</classifier>
</dependency>
```

Implemented via a Maven profile that re-runs the compiler plugin with `<release>17` and adds `<classifier>java17</classifier>` to the jar plugin. Same source, two outputs, versions stay in sync.

### Impact on the Aggregator Model

IBS (Java 17 JVM) loading a host project's Java 11 classes — no issue. Java 17 JVM is fully backwards-compatible with Java 11 bytecode.

---

## Java LTS Status (as of 2026-04-24)

| Java version | Eclipse Temurin | Amazon Corretto | Azul Zulu |
|---|---|---|---|
| **Java 11** | Oct 2027 | Jan 2032 | Jan 2032 |
| **Java 17** | Sep 2027 | Aug 2029 | Jan 2030 |
| **Java 21** | **Oct 2029** | **Aug 2031** | **Jan 2032** |

Key observations:
- Java 11 and Java 17 have nearly the **same Temurin expiry** (~Sep/Oct 2027, ~18 months away). Migrating to Java 17 as an intermediate step buys almost no additional runway on Temurin.
- Java 21 is the only version that meaningfully extends the LTS window (Oct 2029 on Temurin — 3.5 more years).
- Oracle JDK Java 11 premier support already ended Sep 2023; Red Hat free support ended Oct 2024.

**Consequence**: the Java 11 LTS problem motivates combining the Java upgrade with the Javalin migration rather than treating it as a later, lower-priority step.

---

## Decision

Two separate migrations in priority order. Isolating the framework change from the Java version change limits blast radius — if something breaks, the cause is unambiguous.

### Priority 1 — Migrate from Spark Java to Javalin 6 (issue #38, keep Java 11)

- Spark Java is unmaintained; Jetty 9.4 is end-of-community-support.
- Javalin 6 runs on Java 11–21, ships Jetty 11 (`jakarta.*` namespace), near-identical API to Spark.
- **IBS stays on Java 11** — zero bytecode compatibility impact on existing exposed projects.
- Immediately unblocks injection into Java 17 and Java 21 exposed projects.
- Lower risk: single change variable, existing hosts require no changes.

### Priority 2 — Upgrade IBS to Java 21 (issue #39, after Javalin is stable)

- Java 11 and Java 17 expire at nearly the same time on Temurin (~Oct/Sep 2027) — no value in stopping at Java 17.
- Java 21 LTS runs to Oct 2029 on Temurin — the only version that materially extends the support window.
- Enables complete Aggregator Model coverage (Java 8 through Java 21 exposed projects) and Virtual Threads.
- **Breaking change for Injection Model**: Java 11 and Java 17 exposed projects can no longer use Injection — they must switch to Aggregator Model.
- Major version bump required (e.g. 4.0.0).

### Integration model guidance after Priority 2

| Exposed project version | Recommended model | Reason |
|---|---|---|
| Java 8 | Aggregator | JVM cannot load Java 21 bytecode |
| Java 11 | Aggregator | JVM cannot load Java 21 bytecode |
| Java 17 | Aggregator | JVM cannot load Java 21 bytecode |
| Java 21 | Injection *(recommended)* or Aggregator | JVM can load Java 21 bytecode; Injection is simpler |

### Java 17 vs Java 21 — why Java 21

| | Java 17 | Java 21 |
|---|---|---|
| Injection Model reach | Java 17 + Java 21 projects | Java 21 projects only |
| Aggregator Model reach | Java 8, 11, 17 | **Java 8, 11, 17, 21** |
| Spring Boot 3.x | ✅ | ✅ |
| LTS until | 2029 | **2031** |
| Virtual Threads | ❌ | ✅ |

---

## Web Framework Alternatives

Jetty/Spark is dropped. The replacement framework must support Java 21 and fat JAR packaging. BridgeService has ~4 HTTP endpoints — a lightweight framework fits better than a full application server.

| Framework | Java minimum | Migration effort from Spark | Virtual Threads | Weight | Verdict |
|---|---|---|:---:|---|---|
| **Javalin 6** | Java 11 | Minimal — API mirrors Spark 1:1 | ✅ opt-in | ~0.5 MB + Jetty 11 | **Recommended** |
| **Javalin 7** | Java 17 | Minimal — API mirrors Spark 1:1 | ✅ | ~0.5 MB + Jetty 12 | Recommended if Java 17+ only |
| **Spring Boot 3.x** | Java 17 | Medium — annotation-based, full DI/autoconfigure | ✅ | ~15–20 MB | Viable; more than needed |
| **Helidon SE** | Java 11 | Low-medium — imperative, Spark-like SE API | ✅ | ~6 MB | Fallback if Javalin proves limiting |
| **Quarkus** | Java 17 | Medium-high — JAX-RS annotations, DI | ✅ | ~10 MB | Overkill for 4 endpoints |
| **Micronaut** | Java 17 | Medium-high — annotation-heavy, AOT | ✅ | ~8 MB | Same as Quarkus |
| **Vert.x** | Java 11 | High — async/reactive, rethink all handlers | ✅ | ~2 MB | Wrong paradigm |
| **Undertow standalone** | Java 11 | Very high — no routing DSL | ✅ | ~3 MB | Too low-level |

**Javalin version summary:**

| Javalin version | Java minimum | Jetty | Namespace | Notes |
|---|---|---|---|---|
| **4.x** | Java 8 | Jetty 9 | `javax.*` | Legacy |
| **5.x** | Java 11 | Jetty 11 | `jakarta.*` | Superseded by 6 |
| **6.x** | Java 11 | Jetty 11 | `jakarta.*` | **Current — covers Java 11, 17, 21** |
| **7.x** | Java 17 | Jetty 12 | `jakarta.*` | Latest — Java 17+ only |

**Javalin 6** is the right choice for the IBS migration: a single version that runs on Java 11 through Java 21, ships Jetty 11 with `jakarta.*`, and has an API nearly identical to Spark Java. Spring Boot remains a valid alternative if broader ecosystem integration is needed later.

### Spring Boot 3.x requires Java 17

| Area | Finding |
|------|---------|
| **Spring Boot 3.x minimum** | Java 17. No Spring Boot 3.x path exists on Java 11. |
| **Spring Boot 2.x** | Supports Java 11, but EOL since November 2023. Not a viable path. |
| **javax → jakarta** | Spring Boot 3.x uses `jakarta.servlet.*`. IBS currently uses `javax.servlet-api:3.1.0`. All imports must be migrated. v6SOAPAPI also has `javax.servlet-api` at compile scope — needs updating when IBS migrates. |

Java 17 is therefore the perfect stepping stone: it is the Spring Boot 3.x minimum, and the Java 17 classifier JAR is exactly what will become the new default after the Spring Boot migration.

---

## Recommended Migration Path

```
                  Java 11 + Spark Java 2.9.4 + Jetty 9.4  [COMPLETE — pre-3.12]
                  Injection:  Java 11 exposed projects ✔
                  Aggregator: Java 8, 11 exposed projects ✔
       │
       ▼
Priority 1 (#38)  Java 11 + Javalin 6  ✅ DONE (released 3.12)
                  Replace Spark + Jetty with Javalin 6
                  Fix setAccessible(true) in CallContent.java:171-172
                  Migrate javax.* → jakarta.*
                  CI matrix: test on Java 11, 17, 21 host JVMs
                  Injection:  Java 11, 17, 21 exposed projects ✔
                  Aggregator: Java 8, 11 exposed projects ✔
       │
       ▼
Priority 2 (#39)  Java 21 + Javalin 6  (major version bump e.g. 4.0.0)
                  Upgrade IBS to Java 21 (<release>21</release>)
                  Pin maven-compiler-plugin ≥ 3.11.0
                  Drop Java 8 claim from ReleaseNotes.md
                  Injection:  Java 21 exposed projects ✔
                  Aggregator: Java 8, 11, 17, 21 exposed projects ✔
                  ⚠ Java 11/17 exposed projects must move to Aggregator Model
```

---

## Files to Change — Priority 1 (Javalin migration, issue #38)

| File | Change |
|------|--------|
| `integroBridgeService/pom.xml` | Remove `spark-core:2.9.4` and `javax.servlet-api:3.1.0`; add `io.javalin:javalin:6.x` |
| `integroBridgeService/src/main/java/.../IntegroAPI.java` | Rewrite HTTP layer: replace Spark static DSL with Javalin instance API; migrate multipart from `javax.servlet` to `ctx.uploadedFiles()`; replace `secure(...)` with Javalin `SslPlugin`; return `Javalin` instance from `startServices()` |
| `integroBridgeService/src/main/java/.../MCPRequestHandler.java` | Change `handle(spark.Request, spark.Response)` to `handle(io.javalin.http.Context)` |
| `integroBridgeService/src/main/java/.../CallContent.java:171-172` | Add `setAccessible(true)` before `newInstance()` and `invoke()` (Java 17+ strong encapsulation) |
| `integroBridgeService/src/test/java/.../E2ETests.java` | Remove `Spark.awaitInitialization()` (Javalin `start()` blocks); replace `Spark.stop()` with `app.stop()` |
| `integroBridgeService/src/test/java/.../E2EPortCheck.java` | Remove `spark.Spark` import |
| `integroBridgeService/src/test/java/.../LogManagementTest.java` | Remove `spark.Spark` import |
| `integroBridgeService/src/test/java/.../MCPBridgeServerTest.java` | Replace `Spark.awaitInitialization()` and `Spark.stop()` with Javalin instance calls |
| `.github/workflows/onPushSimpleTest.yml` | Add CI matrix: test on Java 11, 17, and 21 host JVMs |
| `docs/Technical.md` | Add Javalin migration notes and `jakarta.*` namespace change |
