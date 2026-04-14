# Testing and Using BridgeService as an MCP Server

This document explains how to run BridgeService as an MCP server and verify it works, first
using the built-in demo data, then from an external project that hosts its own Java library.

## Table of Contents

- [Testing with the built-in demo data (bridgeService-data)](#testing-with-the-built-in-demo-data)
  - [Starting the server](#starting-the-server)
  - [MCP handshake](#mcp-handshake)
  - [Discovering tools](#discovering-tools)
  - [Calling tools](#calling-tools)
  - [Call chaining best practice](#call-chaining-best-practice)
  - [Project-specific pre-call setup (IBS.MCP.PRECHAIN)](#project-specific-pre-call-setup-ibsmcpprechain)
  - [Passing environment variables via headers (ibs-env-*)](#passing-environment-variables-via-headers-ibs-env-)
  - [Tools that are intentionally excluded](#tools-that-are-intentionally-excluded)
- [Exposing your own project as MCP tools](#exposing-your-own-project-as-mcp-tools)
  - [Injection model — adding IBS to your project](#injection-model--adding-ibs-to-your-project)
  - [Aggregator model — adding your project to IBS](#aggregator-model--adding-your-project-to-ibs)
  - [Configuring tool discovery](#configuring-tool-discovery)
  - [Surfacing Javadoc as tool descriptions](#surfacing-javadoc-as-tool-descriptions)
  - [Javadoc quality gate](#javadoc-quality-gate)
  - [What tools will be generated](#what-tools-will-be-generated)
- [Connecting to Claude Code](#connecting-to-claude-code)
  - [Naming your MCP server](#naming-your-mcp-server)
  - [Start BridgeService](#start-bridgeservice)
  - [Register the MCP server](#register-the-mcp-server)
  - [Verify the connection](#verify-the-connection)

---

## Testing with the built-in demo data

`bridgeService-data` is a module included in this repository that provides concrete Java classes
used by the test suite. It is the quickest way to verify that the MCP endpoint is working correctly
without any external dependencies.

### Starting the server

Run the following command from the repository root. It starts IBS on port 8080 in demo mode
(which compiles `bridgeService-data` into the classpath), enables the MCP endpoint, and points
tool discovery at the `testdata.one` package:

```bash
mvn -pl integroBridgeService exec:java \
    -Dexec.args="test" \
    -Ddemo.project.mode=compile \
    -DIBS.MCP.ENABLED=true \
    -DIBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES=com.adobe.campaign.tests.bridge.testdata.one
```

To scan multiple packages, separate them with commas:

```bash
-DIBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES=com.adobe.campaign.tests.bridge.testdata.one,com.adobe.campaign.tests.bridge.testdata.two
```

### MCP handshake

Every MCP session begins with an `initialize` request. Send it once before any other call:

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "clientInfo": { "name": "my-client", "version": "1.0" },
      "capabilities": {}
    }
  }'
```

Expected response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "serverInfo": { "name": "bridgeService", "version": "2.11.19" },
    "capabilities": { "tools": {} }
  }
}
```

### Discovering tools

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

With `testdata.one` on the classpath the response includes tools derived from two classes:
`SimpleStaticMethods` and `ClassWithLogger`. Some representative entries:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "SimpleStaticMethods_methodReturningString",
        "description": "Returns the success string constant used for testing.",
        "inputSchema": { "type": "object", "properties": {} }
      },
      {
        "name": "SimpleStaticMethods_methodAcceptingStringArgument",
        "description": "Appends the success suffix to the given string.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "arg0": { "type": "string", "description": "the input string" }
          },
          "required": ["arg0"]
        }
      },
      {
        "name": "SimpleStaticMethods_methodAcceptingTwoArguments",
        "description": "Concatenates two strings with a + separator and appends the success suffix.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "arg0": { "type": "string", "description": "the first string" },
            "arg1": { "type": "string", "description": "the second string" }
          },
          "required": ["arg0", "arg1"]
        }
      },
      {
        "name": "SimpleStaticMethods_methodAcceptingIntArgument",
        "description": "Returns the given integer multiplied by three.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "arg0": { "type": "integer", "description": "the input integer" }
          },
          "required": ["arg0"]
        }
      },
      {
        "name": "ClassWithLogger_fetchRandomCountry",
        "description": "Returns a randomly selected ISO 3166-1 alpha-2 country code from the available set (AT, AU, CA, CH, DE).",
        "inputSchema": { "type": "object", "properties": {} }
      },
      {
        "name": "ClassWithLogger_getCountries",
        "description": "Returns the fixed list of ISO 3166-1 alpha-2 country codes available for testing: AT, AU, CA, CH, DE.",
        "inputSchema": { "type": "object", "properties": {} }
      },
      {
        "name": "java_call",
        "description": "Generic BridgeService call. Accepts the full /call payload including call chaining, instance methods, environment variables, and timeout.",
        "inputSchema": { "...": "see README" }
      }
    ]
  }
}
```

### Calling tools

**No-argument method:**

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "SimpleStaticMethods_methodReturningString",
      "arguments": {}
    }
  }'
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"returnValues\":{\"result\":\"_Success\"},\"callDurations\":{\"result\":2}}"
      }
    ],
    "isError": false
  }
}
```

**Method with a String argument:**

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "SimpleStaticMethods_methodAcceptingStringArgument",
      "arguments": { "arg0": "hello" }
    }
  }'
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"returnValues\":{\"result\":\"hello_Success\"},\"callDurations\":{\"result\":1}}"
      }
    ],
    "isError": false
  }
}
```

**Method with two String arguments:**

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "SimpleStaticMethods_methodAcceptingTwoArguments",
      "arguments": { "arg0": "hello", "arg1": "world" }
    }
  }'
```

**Fetching a random country from ClassWithLogger:**

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "tools/call",
    "params": {
      "name": "ClassWithLogger_fetchRandomCountry",
      "arguments": {}
    }
  }'
```

**Using the `java_call` fallback for call chaining** (get a country list, then pass it to another method):

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "tools/call",
    "params": {
      "name": "java_call",
      "arguments": {
        "callContent": {
          "countries": {
            "class": "com.adobe.campaign.tests.bridge.testdata.one.ClassWithLogger",
            "method": "getCountries",
            "args": []
          },
          "size": {
            "class": "com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods",
            "method": "methodAcceptingListArguments",
            "args": ["countries"]
          }
        }
      }
    }
  }'
```

### Call chaining best practice

Each auto-discovered tool call (and each `java_call` invocation) runs inside a freshly isolated
class loader context. This means:

- Static variables set in one tool call are **not visible** to the next tool call.
- Authentication state, cached connections, or any other static state established in one call will
  be gone by the time a second call starts.

**For single, stateless operations** (read a value, compute something, convert data) calling
individual auto-discovered tools one by one is fine.

**For multi-step scenarios** — especially those involving authentication, object creation followed
by mutation, or any operation where step N depends on state established by step N−1 — bundle all
steps into a single `java_call` using call chaining:

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 10,
    "method": "tools/call",
    "params": {
      "name": "java_call",
      "arguments": {
        "callContent": {
          "step1": {
            "class": "com.example.Auth",
            "method": "login",
            "args": ["user", "password"]
          },
          "step2": {
            "class": "com.example.Resource",
            "method": "create",
            "args": ["step1"]
          }
        }
      }
    }
  }'
```

All entries in `callContent` execute in the same isolated context in insertion order. A prior
call's return value is substituted by referencing its key as a string argument (e.g. `"step1"` in
the `args` of `step2`).

The `java_call` tool description also makes this explicit, so AI agents reading the tool list
will see the guidance directly.

---

### Project-specific pre-call setup (`IBS.MCP.PRECHAIN`)

Some projects need one or more setup operations to run before every tool invocation — for example,
an authentication step that establishes a session token in the class loader's static cache.

`IBS.MCP.PRECHAIN` addresses this at the server level. Set it to a JSON `callContent` fragment and
BridgeService will prepend those calls to every **auto-discovered** tool invocation, running them
inside the same isolated context as the actual call. Pre-chain return values are stripped from the
response before it is returned.

#### Configuration

```
IBS.MCP.PRECHAIN={"<key1>":{"class":"...","method":"...","args":[...]}, ...}
```

The value is a standard BridgeService `callContent` JSON object — the same format used in
`java_call` payloads. Entries execute in insertion order, and call-chaining dependency resolution
(referencing a prior entry's key in an `args` array) works as normal.

#### Example: CampaignTests authentication

CampaignTests requires two steps before any operation:

1. Fetch an auth token (`ConnectionToken.fetchAuthFromIMSBearerToken`)
2. Store it as the current authentication (`CampaignUtils.setCurrentAuthentication`)

With `IBS.MCP.PRECHAIN` the auth is injected automatically into every auto-discovered tool call:

```
IBS.MCP.PRECHAIN={"ibs_auth":{"class":"com.example.ConnectionToken","method":"fetchAuthFromIMSBearerToken","args":["ibs-secret-endpoint","ibs-secret-token"]},"ibs_set_auth":{"class":"com.example.CampaignUtils","method":"setCurrentAuthentication","args":["ibs_auth"]}}
```

#### Passing secrets securely

Sensitive values (tokens, endpoints) should be passed as HTTP headers in the MCP server
registration, using the existing `ibs-secret-` prefix. BridgeService injects these headers into
the class loader result cache at the start of every call, making them available for
call-chaining dependency resolution — no extra code is required.

Register the server with credentials in the `.mcp.json` `headers` map:

```json
{
  "mcpServers": {
    "CampaignTests": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "ibs-secret-endpoint": "https://my-instance.campaign.adobe.com",
        "ibs-secret-token": "eyJ..."
      }
    }
  }
}
```

The prechain args reference those header names as plain strings:

```
IBS.MCP.PRECHAIN={"ibs_auth":{"class":"...","method":"fetchAuthFromIMSBearerToken","args":["ibs-secret-endpoint","ibs-secret-token"]}, ...}
```

BridgeService resolves the strings `"ibs-secret-endpoint"` and `"ibs-secret-token"` to the
corresponding header values via the standard dependency mechanism — exactly as call chaining would
resolve any prior result by key.

**Secrets are always protected:**

- Headers with the `ibs-secret-` prefix are suppressed from all tool responses.
- Pre-chain return values (e.g. `ibs_auth`, `ibs_set_auth`) are stripped from `returnValues`
  before the response is returned — only the actual tool result is visible to the caller.
- The value of `IBS.MCP.PRECHAIN` is never written to logs at INFO or DEBUG level.

#### Prechain is NOT applied to `java_call`

`java_call` invocations bypass the prechain entirely. `java_call` accepts a complete `callContent`
payload, so callers have full control over what runs in the chain. If your `java_call` payload
needs the auth setup, include it explicitly as the first entries in `callContent`.

#### Consumer project guidance

Any project registering BridgeService as an MCP server that requires setup before every tool call
should:

1. Configure `IBS.MCP.PRECHAIN` with the required setup steps.
2. Pass credentials as `ibs-secret-*` headers in the MCP server registration.
3. Document the pattern in the project's own `CLAUDE.md` so AI agents understand they do not
   need to perform auth themselves — it is handled transparently by the server:

```markdown
## BridgeService MCP usage
- Auth is pre-configured via IBS.MCP.PRECHAIN; do not include auth calls in your tool payloads.
- For multi-step scenarios use `java_call` with call chaining (single `callContent` payload).
  State does not persist between separate tool calls.
```

---

### Passing environment variables via headers (`ibs-env-*`)

Some Java methods depend on environment variables that must be set before execution — for example,
a hostname, a port, or a locale that changes per deployment. The standard `/call` endpoint accepts
these under an `environmentVariables` JSON node. The MCP server provides an equivalent mechanism
without requiring a dedicated payload node: **encode them as HTTP headers with the `ibs-env-` prefix**.

BridgeService reads every request header whose name begins with `ibs-env-`, strips the prefix to
obtain the variable name, and injects the key/value pair as an environment variable into the
`JavaCalls` execution context — exactly as if it had been provided in the `environmentVariables`
node of a `/call` payload.

This works for both auto-discovered tools and the `java_call` fallback. For `java_call`, the
headers are merged into the `environmentVariables` map before the payload is parsed, so any
variables already present in the `arguments` are preserved; headers only add or overwrite.

#### Configuring env vars in `.claude.json`

Add env vars to the `headers` block of the MCP server registration, using the `ibs-env-` prefix:

```json
{
  "mcpServers": {
    "CampaignTests": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "ibs-secret-login": "admin",
        "ibs-secret-pass": "mypassword",
        "ibs-env-AC.UITEST.HOST": "accintg-ci93.rd.campaign.adobe.com",
        "ibs-env-AC.UITEST.LANGUAGE": "en_US",
        "ibs-env-AC.UITEST.MAILING.PORT": "143",
        "ibs-env-AC.UITEST.MAILING.HOST": "mail.example.com"
      }
    }
  }
}
```

At runtime the server extracts these headers and injects:

| Environment variable      | Value                              |
| ------------------------- | ---------------------------------- |
| `AC.UITEST.HOST`          | `accintg-ci93.rd.campaign.adobe.com` |
| `AC.UITEST.LANGUAGE`      | `en_US`                            |
| `AC.UITEST.MAILING.PORT`  | `143`                              |
| `AC.UITEST.MAILING.HOST`  | `mail.example.com`                 |

#### Prefix configuration

The default prefix is `ibs-env-`. It can be changed via:

```
IBS.MCP.ENV.HEADER.PREFIX=my-custom-prefix-
```

Set it to blank to disable the feature entirely:

```
IBS.MCP.ENV.HEADER.PREFIX=
```

#### Interaction with `IBS.MCP.PRECHAIN`

Env vars injected from `ibs-env-*` headers are available to the environment variable setter before
both the pre-chain steps and the actual tool call execute, because `JavaCalls.environmentVariables`
is populated before `submitCalls()` is called. Pre-chain steps that depend on env vars (e.g., a
hostname resolution step) will see them.

---

### Tools that are intentionally excluded

Some methods in `SimpleStaticMethods` are not exposed as auto-discovered tools:

| Method                                                         | Reason excluded                                                                            |
| -------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `overLoadedMethod1Arg(String)` and `overLoadedMethod1Arg(int)` | Both have one parameter — ambiguous, cannot be disambiguated by parameter count            |
| `methodAcceptingFile(File)`                                    | `File` parameters require multi-part upload, which is not supported by the MCP tool schema |
| Any instance method                                            | Only `public static` methods are discovered                                                |

These methods remain accessible via the `java_call` fallback tool.

---

## Exposing your own project as MCP tools

There are two ways to deploy IBS with your project, matching the two models described in the main
README.

### Injection model — adding IBS to your project

This is the recommended approach. You add `integroBridgeService` as a dependency to your project
and start the server from within it.

**1. Add the dependency to your `pom.xml`:**

```xml
<dependency>
    <groupId>com.adobe.campaign.tests.bridge.service</groupId>
    <artifactId>integroBridgeService</artifactId>
    <version>2.11.19</version>
</dependency>
```

**2. Start the service** with MCP enabled and your package(s) listed:

```bash
mvn compile exec:java \
    -Dexec.mainClass=MainContainer \
    -Dexec.args="test" \
    -DIBS.MCP.ENABLED=true \
    -DIBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES=com.example.myproject.services
```

All public static methods found in `com.example.myproject.services` (and its sub-packages) are
immediately available as MCP tools.

### Aggregator model — adding your project to IBS

In this model you clone (or fork) the BridgeService repository, add your project as a Maven
dependency inside `integroBridgeService/pom.xml`, and build a fat JAR or Docker image that
bundles everything together.

```xml
<!-- inside integroBridgeService/pom.xml -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>myproject</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then build and start:

```bash
mvn clean package
java -jar integroBridgeService/target/integroBridgeService-*.jar test \
    -DIBS.MCP.ENABLED=true \
    -DIBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES=com.example.myproject.services
```

### Configuring tool discovery

The environment variable `IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES` controls which packages are
scanned at startup. It accepts a comma-separated list of package prefixes:

```
IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES=com.example.services,com.example.utils
```

Only **public static methods** in classes directly under those packages (and sub-packages) are
registered as tools. The same variable also drives the IBS class loader isolation, so every class
your methods transitively depend on must be reachable under one of the listed prefixes — or in
the system classpath.

### Surfacing Javadoc as tool descriptions

By default, tool descriptions fall back to a generated string (`"Calls com.example.MyClass.methodName()"`).
To have the actual Javadoc comment appear as the tool description in `tools/list`, add the
`therapi-runtime-javadoc-scribe` annotation processor to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.github.therapi</groupId>
    <artifactId>therapi-runtime-javadoc-scribe</artifactId>
    <version>0.15.0</version>
    <scope>provided</scope>
</dependency>
```

**No extra Maven goals are required.** The annotation processor runs automatically during the
`compile` phase, which is already part of your existing `mvn clean package`. It embeds the
Javadoc comments as resource files inside your compiled JAR
(e.g. `javadoc/com/example/EmailService.json`).

At startup, `MCPToolDiscovery` reads those embedded resources via `RuntimeJavadoc.getJavadoc(method)`.
As long as your JAR is on BridgeService's classpath when the server starts, the descriptions are
picked up transparently — no configuration needed beyond the dependency above.

`@param` Javadoc tags are also read and used as the parameter descriptions in the tool's
`inputSchema`, helping the AI understand what each argument expects.

Without the dependency, tools are still fully functional; only the description quality is reduced.

### Javadoc quality gate

By default (`IBS.MCP.REQUIRE_JAVADOC=true`), BridgeService **only exposes methods that have a
non-empty Javadoc comment**. Methods without Javadoc are silently skipped at startup and will not
appear in `tools/list`.

This is intentional. A method with no Javadoc would receive a generic fallback description such as
`"Calls com.example.MyClass.method()"`, which gives an AI agent no useful information about when
or why to call it. Exposing such tools increases the risk of accidental invocations.

**To opt out** (expose all public static methods regardless of documentation):

```
IBS.MCP.REQUIRE_JAVADOC=false
```

**Writing good Javadoc for MCP tools** goes beyond just satisfying the gate. Descriptions should
make the testing or domain purpose self-evident so an AI agent can distinguish your tools from
others in a multi-server session:

| Weak                                  | Better                                                                                                                          |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `/** Returns a list of countries. */` | `/** Returns the fixed list of ISO 3166-1 country codes (AT, AU, CA, CH, DE) used as test fixtures for campaign validation. */` |
| `/** Sends an email. */`              | `/** Sends a test email via the configured SMTP mock and returns the delivery receipt ID. */`                                   |
| `/** Gets the cache value. */`        | `/** Returns the value stored under the given key in the in-process integro test-execution cache. */`                           |

Include `@param` tags for each argument — BridgeService uses them to populate the parameter
descriptions in the tool's `inputSchema`.

### What tools will be generated

Given a project with the following class (with `therapi-runtime-javadoc-scribe` on the classpath):

```java
package com.example.myproject.services;

public class EmailService {
    /** Sends an email to the given recipient with the specified subject. */
    public static String sendEmail(String recipient, String subject) { ... }
    /** Returns the list of message subjects in the given account's inbox. */
    public static List<String> listInbox(String account) { ... }
    /** Deletes all messages in the given account's inbox. */
    public static void purgeInbox(String account) { ... }
    public String getStatus() { ... }  // instance method — excluded
}
```

IBS would register the following MCP tools, with descriptions sourced from Javadoc:

| Tool name                 | Description                                                        | Parameters                     |
| ------------------------- | ------------------------------------------------------------------ | ------------------------------ |
| `EmailService_sendEmail`  | Sends an email to the given recipient with the specified subject.  | `arg0: string`, `arg1: string` |
| `EmailService_listInbox`  | Returns the list of message subjects in the given account's inbox. | `arg0: string`                 |
| `EmailService_purgeInbox` | Deletes all messages in the given account's inbox.                 | `arg0: string`                 |

Without `therapi-runtime-javadoc-scribe`, the descriptions would fall back to
`"Calls com.example.myproject.services.EmailService.sendEmail()"` etc.

`getStatus()` is excluded because it is an instance method.

An AI agent calling `tools/list` would see these tools with their JSON Schema and can invoke them
directly using `tools/call`. For scenarios that require instance methods or call chaining, the
always-present `java_call` tool accepts the full BridgeService `/call` payload format — see the
[Making Java Calls](../README.md#making-java-calls) section of the main README.

---

## Connecting to Claude Code

Claude Code supports MCP servers over HTTP natively. Once BridgeService is running with MCP
enabled, you can register it as a named MCP server and Claude Code will automatically call
`tools/list` at startup and make the tools available during your session.

### Naming your MCP server

BridgeService is generic infrastructure that can be deployed for many different projects. The name
you give it at registration time becomes the namespace for all its tools in MCP clients —
Claude Code, for example, exposes tools as `mcp__<serverName>__ClassName_method`.

**Always name the server after your project**, not `"bridgeService"`. This makes tools
self-contextualising in a multi-server session:

| Registration name                    | Tool name seen by agent                        |
| ------------------------------------ | ---------------------------------------------- |
| `bridgeService` (generic, avoid)     | `mcp__bridgeService__EmailService_sendEmail`   |
| `CampaignTests` (project-specific)   | `mcp__CampaignTests__EmailService_sendEmail`   |
| `EmailAutomation` (feature-specific) | `mcp__EmailAutomation__EmailService_sendEmail` |

The name is set entirely on the client side when you register the server — no BridgeService
configuration is needed. See [Register the MCP server](#register-the-mcp-server) for the
exact command.

### Start BridgeService

Using the demo data (quickest way to try it):

```bash
mvn -pl integroBridgeService exec:java \
    -Dexec.args="test" \
    -Ddemo.project.mode=compile \
    -DIBS.MCP.ENABLED=true \
    -DIBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES=com.adobe.campaign.tests.bridge.testdata.one
```

For your own project, start BridgeService with your packages configured instead (see
[Exposing your own project as MCP tools](#exposing-your-own-project-as-mcp-tools)).

### Register the MCP server

In a separate terminal, register the running BridgeService instance as an MCP server in
Claude Code. Replace `CampaignTests` with the name of your project (see
[Naming your MCP server](#naming-your-mcp-server)). Use `--scope user` to make it available
globally across all projects, or omit it to register it only for the current project:

```bash
# Register globally (available in all Claude Code sessions)
claude mcp add --transport http CampaignTests http://localhost:8080/mcp --scope user

# Register for the current project only
claude mcp add --transport http CampaignTests http://localhost:8080/mcp
```

This writes an entry to `~/.claude/mcp.json` (global) or `.mcp.json` in the project root
(project-scoped). The resulting config entry looks like:

```json
{
  "mcpServers": {
    "CampaignTests": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

If BridgeService is deployed remotely (Docker/K8s), replace `http://localhost:8080` with the
actual deployment URL.

### Verify the connection

List all registered MCP servers and their status:

```bash
claude mcp list
```

You should see `bridgeService` listed as connected. You can also check inside an interactive
Claude Code session with the `/mcp` slash command.

Once connected, Claude Code will discover all exposed tools via `tools/list` at the start of
each session. You can then ask Claude to call your Java methods directly — for example:

> "Call `SimpleStaticMethods_methodAcceptingStringArgument` with the argument `hello`"

To remove the server registration when you no longer need it:

```bash
claude mcp remove bridgeService
```
