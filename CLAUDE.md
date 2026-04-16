# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Does

BridgeService is a REST service that exposes Java code/libraries as HTTP endpoints, enabling any language or framework to invoke Java methods via REST. It is particularly useful in test automation — e.g., Cypress tests calling Java backend logic without rewriting it.

## Build & Common Commands

```bash
# Build entire project
mvn clean install

# Run all tests
mvn test

# Run a specific test class
mvn -Dtest=ClassName test

# Start BridgeService locally on port 8080 (test mode)
mvn -pl integroBridgeService exec:java -Dexec.args="test"

# Start with demo data compiled in
mvn -pl integroBridgeService exec:java -Dexec.args="test" -Ddemo.project.mode=compile

# Add/fix license headers (required before committing new source files)
mvn license:format

# Generate fat JAR with all dependencies
mvn package
```

**Quality gates that must pass before merging:** unit tests pass, no coverage decrease, SonarCloud gate green, license headers present.

## Module Structure

```
parent (pom.xml)
├── integroBridgeService/   ← main service
└── bridgeService-data/     ← test data & demo classes used by the test suite
```

`bridgeService-data` is a dependency of the test scope in `integroBridgeService`. It provides concrete Java classes (under `com.adobe.campaign.tests.bridge.testdata.*`) that the tests call through the REST API.

## Architecture Overview

### Core Request Flow

1. **`IntegroAPI`** (`service/IntegroAPI.java`) — Spark Framework HTTP layer. Three endpoints:
   - `GET /test` — health check
   - `GET /service-check` — external service connectivity check
   - `POST /call` — main invocation endpoint (also accepts multipart for file uploads)

2. **`BridgeServiceFactory`** — deserializes the incoming JSON payload into a `JavaCalls` object.

3. **`JavaCalls`** — orchestrates execution. Handles call chaining, environment variable injection, timeout management (via `ExecutorService`), and Hamcrest-based assertions on results or call duration.

4. **`CallContent`** — represents a single method call (class name, method name, arguments). Uses reflection to invoke the method. Supports instance method calls where a prior call's return value is reused as the object instance.

5. **`IntegroBridgeClassLoader`** — custom class loader that isolates static variable state per call session to prevent cross-call interference. Three modes:
   - **Automatic** — loads all accessed classes (small memory cost)
   - **Semi-Manual** — loads only statically referenced classes
   - **Manual** — loads only explicitly configured packages (most control)

6. **`JavaCallResults`** — aggregates return values, call durations, and assertion outcomes, then serializes to JSON (Jackson).

7. **`IBSPluginManager`** + `IBSDeserializerPlugin` — plugin system that allows custom deserialization of non-JSON-serializable return types. Plugins are discovered by package scan (`IBS.PLUGINS.PACKAGE` env var).

8. **`MetaUtils`** — reflection utilities for method lookup and object property scraping.

9. **`ErrorObject`** — standardized error response with code, message, and stack trace. Error codes: `404` (client error: bad payload, missing class, ambiguous method), `408` (timeout), `500` (server/runtime error).

### Key Environment Variables

| Variable                                                 | Purpose                                  | Default       |
| -------------------------------------------------------- | ---------------------------------------- | ------------- |
| `IBS.TIMEOUT.DEFAULT`                                    | Global execution timeout (ms)            | 10000         |
| `IBS.CLASSLOADER.AUTOMATIC.INTEGRITY.INJECTION`          | Class loader mode                        | AUTO          |
| `IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES`              | Packages to load in static mode          | —             |
| `IBS.DESERIALIZATION.DEPTH.LIMIT`                        | Object scraping recursion depth          | 1             |
| `IBS.DESERIALIZATION.DATE.FORMAT`                        | SimpleDateFormat pattern for date fields | —             |
| `IBS.PLUGINS.PACKAGE`                                    | Package to scan for deserializer plugins | —             |
| `IBS.SECRETS.FILTER.PREFIX`                              | HTTP header prefix for secrets           | `ibs-secret-` |
| `IBS.HEADERS.FILTER.PREFIX`                              | Restrict which headers enter call-chain cache | `""` (all) |
| `IBS.ENV.HEADER.PREFIX`                                  | HTTP header prefix for env-var injection | `ibs-env-`    |
| `IBS.ENVVARS.SETTER.CLASS` / `IBS.ENVVARS.SETTER.METHOD` | Custom env var injection handler         | —             |

These are managed by `ConfigValueHandlerIBS.java`.

## Testing

- **Framework:** TestNG (`integroBridgeService/src/test/resources/testng.xml`)
- **Coverage:** JaCoCo (reports go to `target/site/jacoco/`)
- **Test REST calls:** REST-assured
- **Assertions:** Hamcrest Matchers
- **Mocking:** Mockito
- Tests live in `integroBridgeService/src/test/java/` and call real service endpoints (not mocked — the service is started in-process before tests).

## Deployment

- Entry point: `MainContainer.java` (accepts `"test"` arg for test mode, or SSL keystore config for production)
- Two Docker images: `DockerfileNoSSL` and `DockerfileSSL` (TLSv1.2)
- Built via `buildScripts/buildImage.sh`
- Port 8080 (test mode), 443 (SSL production)

## Java & Dependency Notes

- Java 11 (source/target) is required. Do not use APIs introduced after Java 11.
- Web framework: Spark Java 2.9.4
- JSON: Jackson 2.18.x
- Logging: Log4j 2 (50MB rotation, 3GB cleanup, 10-day retention per `docs/Technical.md`)

## Java Naming Conventions

Apply these prefixes consistently in all new and modified Java code:

| Prefix | Applies to | Example |
|---|---|---|
| `in_` | Method parameters | `in_serverUrl`, `in_userId` |
| `l_` | Local variables | `l_result`, `l_callContent` |
| `lt_` | Variables scoped to a loop or condition block (do not escape the block) | `lt_entry`, `lt_key` |
| *(none)* | `for` loop counters | `i`, `j` |

## Contribution Rules

- All new source files must have the Adobe license header (`mvn license:format` adds it).
- Coverage must not decrease.
- SonarCloud quality gate must be green.
- A signed CLA is required for external contributors.
- All features need to be documented, if they are functional they go in README, otherwise there is a dedicated document called docs/Technical.md
