# Java Version Migration — Research & Plan (Issue #25)

BridgeService is currently compiled to Java 11. This document captures the complexity analysis and recommended migration path for supporting other Java versions, both in the IBS runtime and in host projects.

---

## Integration Models

The README defines two ways to integrate IBS (see [Implementing The Bridge Service in Your Project](../README.md#implementing-the-bridge-service-in-your-project)):

- **Injection Model** *(recommended)*: IBS is added as a Maven compile-scope dependency to the host project. The host's JVM loads IBS classes directly. Example: **v6SOAPAPI** (`Adobe-Campaign/pom.xml`).
- **Aggregator Model**: The host project is added as a Maven dependency to IBS. IBS's own JVM loads the host's classes via reflection.

This distinction is critical for Java version compatibility — which JVM loads whose bytecode determines what breaks.

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

## Scenario D — Why Java 17, not Java 21

### Spark Java 2.9.4 is a blocker for Java 21

| Area | Java 21 | Java 17 |
|------|---------|---------|
| Official Spark Java support | No | No (but works with `--add-opens`) |
| Jetty 9.4.x | Not supported | End-of-community-support; functionally works |
| Spark replacement required immediately? | **Yes** | No — can stay on Spark until Spring Boot migration |
| `setAccessible(true)` fix needed? | Yes | Yes |

Spark Java 2.9.4 (last release ~2021, minimally maintained) bundles Jetty 9.4.48 which does not officially support Java 21. Running IBS on Java 21 requires replacing Spark first. On Java 17, Spark is usable with a small number of `--add-opens` JVM flags as a transitional measure.

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
Current state          Java 11, Spark Java 2.9.4
       │
       ▼
Step 1 (issue #25)     Publish Java 17 classifier JAR alongside default Java 11 JAR
                       Fix setAccessible(true) in CallContent.java:171-172
                       Add CI matrix: test on Java 11 and Java 17
                       Add --add-opens flags to docs/Technical.md for Spark + Java 17
                       Drop Java 8 claim from ReleaseNotes.md
       │
       ▼
Step 2 (Spring Boot)   Replace Spark with Spring Boot 3.x (new major version, e.g. 4.0.0)
                       Java 17 becomes the new single minimum — drop Java 11 JAR
                       Migrate javax.* → jakarta.* throughout IBS and v6SOAPAPI
                       Major version bump signals breaking change to host projects
```

---

## Files to Change in Step 1

| File | Change |
|------|--------|
| `integroBridgeService/src/main/java/.../CallContent.java:171-172` | Add `setAccessible(true)` before `newInstance()` and `invoke()` |
| `pom.xml:40-42` | Switch to `<release>` tag; pin `maven-compiler-plugin` ≥ 3.11.0 |
| `pom.xml` | Add Maven profile for Java 17 classifier JAR |
| `.github/workflows/onPushSimpleTest.yml:37` | Add CI matrix for Java 11 and Java 17 |
| `docs/Technical.md` | Add Java version compatibility section and `--add-opens` notes |
| `ReleaseNotes.md` | Remove outdated Java 8 availability claim |
