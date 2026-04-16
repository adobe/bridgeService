# Testing and Using BridgeService as an MCP Server

This document explains how to run BridgeService as an MCP server and verify it works, first
using the built-in demo data, then from an external project that hosts its own Java library.

## Table of Contents

- [Testing with the built-in demo data (bridgeService-data)](#testing-with-the-built-in-demo-data)
  - [Starting the server](#starting-the-server)
  - [MCP handshake](#mcp-handshake)
  - [Discovering tools](#discovering-tools)
  - [Calling tools](#calling-tools)
  - [How the method catalog works](#how-the-method-catalog-works)
  - [Call chaining best practice](#call-chaining-best-practice)
  - [Project-specific pre-call setup (IBS.MCP.PRECHAIN)](#project-specific-pre-call-setup-ibsmcpprechain)
  - [Built-in diagnostics tool (ibs_diagnostics)](#built-in-diagnostics-tool-ibs_diagnostics)
  - [Passing environment variables via headers (ibs-env-*)](#passing-environment-variables-via-headers-ibs-env-)
  - [Tools that are intentionally excluded](#tools-that-are-intentionally-excluded)
- [Exposing your own project as MCP tools](#exposing-your-own-project-as-mcp-tools)
  - [Injection model — adding IBS to your project](#injection-model--adding-ibs-to-your-project)
  - [Aggregator model — adding your project to IBS](#aggregator-model--adding-your-project-to-ibs)
  - [Configuring tool discovery](#configuring-tool-discovery)
  - [Surfacing Javadoc as tool descriptions](#surfacing-javadoc-as-tool-descriptions)
  - [Javadoc quality gate](#javadoc-quality-gate)
  - [What tools will be generated](#what-tools-will-be-generated)
- [MCP Configuration Reference](#mcp-configuration-reference)
- [Connecting to Claude Code](#connecting-to-claude-code)
  - [Naming your MCP server](#naming-your-mcp-server)
  - [Start BridgeService](#start-bridgeservice)
  - [Register the MCP server](#register-the-mcp-server)
  - [Verify the connection](#verify-the-connection)
  - [Connecting from Cursor](#connecting-from-cursor)
  - [Other MCP clients](#other-mcp-clients)
- [Best Practices](#best-practices)
  - [Javadoc is your tool description — garbage in, garbage out](#javadoc-is-your-tool-description--garbage-in-garbage-out)

---

## MCP Configuration Reference

| Variable | Default | Description |
|---|---|---|
| `IBS.MCP.ENABLED` | `false` | Enables the MCP endpoint at `/mcp`. Must be `true` for any MCP usage. |
| `IBS.MCP.PRECHAIN` | — | JSON `callContent` fragment prepended to every `java_call` invocation. Used for server-wide setup such as shared authentication. Can also be supplied per-client via the `ibs-prechain` HTTP header (env var takes precedence). |
| `IBS.MCP.REQUIRE_JAVADOC` | `true` | When `true`, only methods with a non-empty Javadoc comment are included in the tool catalog. Methods without Javadoc are silently excluded from `tools/list`. |

See the relevant sections below for full configuration details and examples.

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

`tools/list` always returns exactly **one tool — `java_call`**. Its `description` contains a
catalog of all methods discovered from the configured packages. AI agents read that catalog to
learn which class and method names to place in their `callContent` payloads.

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "java_call",
        "description": "Generic BridgeService call. Accepts the full /call payload including call chaining, instance methods, environment variables, and timeout. Bundle all operations into one callContent chain so they share a single isolated execution context. State (including authentication) does not persist between separate tool calls.\n\nDiscovered methods (use class/method values in callContent for java_call):\n\nSimpleStaticMethods_methodReturningString\n  class:  com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\n  method: methodReturningString\n  Returns the success string constant used for testing.\n  args: (none)\n\nSimpleStaticMethods_methodAcceptingStringArgument\n  class:  com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\n  method: methodAcceptingStringArgument\n  Appends the success suffix to the given string.\n  arg0 (string): the input string\n\n... (further entries for ClassWithLogger methods etc.)",
        "inputSchema": {
          "type": "object",
          "required": ["callContent"],
          "properties": {
            "callContent": {
              "type": "object",
              "description": "Map of call IDs to call definitions.",
              "additionalProperties": {
                "type": "object",
                "required": ["class", "method"],
                "properties": {
                  "class":  { "type": "string" },
                  "method": { "type": "string" },
                  "args":   { "type": "array"  }
                }
              }
            },
            "environmentVariables": { "type": "object" },
            "timeout": { "type": "integer" }
          }
        }
      }
    ]
  }
}
```

### Calling tools

All calls go through `java_call`. Use the class and method names from the catalog in
`callContent`.

**No-argument method:**

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "java_call",
      "arguments": {
        "callContent": {
          "result": {
            "class": "com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods",
            "method": "methodReturningString",
            "args": []
          }
        }
      }
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
      "name": "java_call",
      "arguments": {
        "callContent": {
          "result": {
            "class": "com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods",
            "method": "methodAcceptingStringArgument",
            "args": ["hello"]
          }
        }
      }
    }
  }'
```

**Call chaining** (get a country list, then pass it to another method):

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

### How the method catalog works

`tools/list` returns a single tool — `java_call`. Auto-discovery does not produce separately
callable tools; instead it builds a **catalog** that is embedded in the `java_call` description.

When an AI agent calls `tools/list` it reads the catalog to learn which class and method names
exist, then constructs the appropriate `callContent` payload and calls `java_call`. The catalog is
rebuilt every time the server starts, so it stays in sync with the Java library automatically.

**Why not separate tools per method?**

Separate tools per method force the AI to make one HTTP round-trip per method call and lose
execution context between calls. With `java_call`, any number of steps can be bundled into one
request inside a single isolated class loader — enabling call chaining, where the return value
of step N is passed directly to step N+1 as a live Java object (not serialized JSON). This is
essential for scenarios involving authentication, object creation, or anything with mutable state.

The catalog format in the description for each entry is:

```
ClassName_methodName
  class:  com.example.package.ClassName
  method: methodName
  <Javadoc description>
  arg0 (type): <param description>
  arg1 (type): <param description>
```

**Project skills and `CLAUDE.md`** can reference catalog entries by class/method name to give the
AI more context about when and how to use each one. For per-user auth or multi-step flows, the
skill prepends an auth step to the `java_call` callContent chain.

---

### Call chaining best practice

Each `java_call` invocation runs inside a freshly isolated class loader context. Static variables
set in one call are **not visible** to the next call — authentication state, cached connections,
or any other static state established in one invocation will be gone by the time a second
invocation starts.

**Bundle related operations into a single `java_call`** using call chaining: all entries in
`callContent` share the same isolated context and execute in insertion order. The return value of
an earlier step is referenced by key in the `args` of a later step:

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
BridgeService will prepend those calls to every `java_call` invocation, running them inside the
same isolated context as the actual call. Pre-chain return values are stripped from the response
before it is returned.

#### Configuration

```
IBS.MCP.PRECHAIN={"<key1>":{"class":"...","method":"...","args":[...]}, ...}
```

The value is a standard BridgeService `callContent` JSON object — the same format used in
`java_call` payloads. Entries execute in insertion order, and call-chaining dependency resolution
(referencing a prior entry's key in an `args` array) works as normal.

Alternatively, the same JSON can be supplied as the `ibs-prechain` HTTP header on the MCP server
registration. The header is used when `IBS.MCP.PRECHAIN` is not set. This is useful for
client-side configuration in MCP clients that support custom headers (e.g. Claude Code's
`.claude.json`):

```json
{
  "mcpServers": {
    "CampaignTests": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "ibs-prechain": "{\"ibs_auth\":{\"class\":\"utils.CampaignUtils\",\"method\":\"setCurrentAuthenticationToLocal\",\"args\":[\"ibs-secret-url\",\"ibs-secret-login\",\"ibs-secret-pass\"]}}"
      }
    }
  }
}
```

#### Example: CampaignTests authentication

CampaignTests requires two steps before any operation:

1. Fetch an auth token (`ConnectionToken.fetchAuthFromIMSBearerToken`)
2. Store it as the current authentication (`CampaignUtils.setCurrentAuthentication`)

With `IBS.MCP.PRECHAIN` the auth is injected automatically into every `java_call` invocation:

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

#### How prechain integrates with your call chain

The user's `callContent` steps execute after the prechain steps inside the same isolated context.
User steps can reference prechain keys by name in their `args` and receive the return values by
reference (same JVM heap). Prechain keys are stripped from `returnValues` and `callDurations`
before the response is returned.

```json
{
  "callContent": {
    "result": {
      "class": "com.example.Resource",
      "method": "create",
      "args": ["ibs_auth"]
    }
  }
}
```

Here `"ibs_auth"` is the key of a prechain step that returned an `Authentication` object. It is
resolved at runtime — it is never passed as a literal string.

#### PRECHAIN is deployment-wide — not suitable for per-user auth

`IBS.MCP.PRECHAIN` is a server environment variable. It is the same for every user connecting to
that deployment. This makes it the right mechanism for setup that is **uniform across all callers**
— classloader configuration, plugin initialisation, or auth that uses a shared service account.

**It is the wrong mechanism for per-user auth.** On a shared IBS deployment two testers may need
to connect to different Campaign instances, use different auth methods (local session vs IMS
bearer), or supply different credentials. A single PRECHAIN value cannot satisfy both.

Per-user auth belongs in the **project skill** — a `CLAUDE.md` or a
`~/.claude/skills/<project>.md` file that each user maintains locally. The skill instructs the AI
to open every `java_call` chain with the auth step appropriate for that user:

```markdown
## BridgeService MCP usage
Always start every java_call chain with your auth step:

    "ibs_auth": {
      "class": "utils.CampaignUtils",
      "method": "setCurrentAuthenticationToLocal",
      "args": ["https://my-instance.campaign.adobe.com", "myuser", "mypassword"]
    }
```

User A's skill might call `setCurrentAuthenticationToLocal`; User B's might call
`fetchAuthFromIMSBearerToken`. Both connect to the same IBS server with no server-side changes.

Credentials in skills should reference `ibs-secret-*` headers (configured in the user's
`.mcp.json`) rather than be written in plain text:

```markdown
    "args": ["ibs-secret-endpoint", "ibs-secret-token"]
```

**Summary: what belongs where**

| Concern | Right place | Why |
|---|---|---|
| Per-user auth credentials and method | User's skill / `CLAUDE.md` | Differs across callers — cannot be a server-side default |
| Shared service-account auth | `IBS.MCP.PRECHAIN` | Truly uniform across all users |
| Classloader / plugin setup | `IBS.MCP.PRECHAIN` | Deployment-wide, same for everyone |
| Class/method names and call patterns | Skill / `CLAUDE.md` | LLM guidance, not execution |

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

### Built-in diagnostics tool (`ibs_diagnostics`)

BridgeService exposes a built-in `ibs_diagnostics` tool alongside `java_call`. It requires no
arguments and has no dependency on the HOST project — it is always available regardless of whether
tool discovery succeeds.

Call it via `tools/call`:

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": { "name": "ibs_diagnostics", "arguments": {} }
  }'
```

Response:

```json
{
  "ibsVersion": "3.11.0",
  "deploymentMode": "TEST",
  "mcpConfig": {
    "packagesConfigured": "com.example.services",
    "prechainActive": true,
    "javadocRequired": true
  },
  "headers": {
    "secretHeaderKeys": ["ibs-secret-login", "ibs-secret-pass", "ibs-secret-url"],
    "envVarHeaders": {
      "AC.UITEST.HOST": "my-instance.example.com",
      "AC.UITEST.LANGUAGE": "en_US"
    },
    "regularHeaderCount": 3
  },
  "discoveredToolCount": 142
}
```

**Fields:**

| Field | Description |
|---|---|
| `ibsVersion` | Running BridgeService version |
| `deploymentMode` | `TEST` or `PRODUCTION` |
| `mcpConfig.packagesConfigured` | Value of `IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES` |
| `mcpConfig.prechainActive` | Whether a prechain is configured (env var or header) |
| `mcpConfig.javadocRequired` | Whether `IBS.MCP.REQUIRE_JAVADOC` is enabled |
| `headers.secretHeaderKeys` | Names of `ibs-secret-*` headers received (values suppressed) |
| `headers.envVarHeaders` | Decoded env-var headers (`ibs-env-*` prefix stripped, uppercased) |
| `headers.regularHeaderCount` | Count of headers that are neither secret nor env-var |
| `discoveredToolCount` | Number of methods in the `java_call` catalog |

Use `ibs_diagnostics` as the first step when connecting a new HOSTSERVICE — it confirms
connectivity, verifies that all `ibs-secret-*` and `ibs-env-*` headers are reaching the server,
and shows whether PRECHAIN and Javadoc requirements are active, all without touching HOST code.

---

### Passing environment variables via headers (`ibs-env-*`)

Some Java methods depend on environment variables that must be set before execution — for example,
a hostname, a port, or a locale that changes per deployment. Both the `/call` and `/mcp` endpoints
accept these as HTTP headers with the `ibs-env-` prefix, as an alternative to the
`environmentVariables` JSON node.

BridgeService reads every request header whose name begins with `ibs-env-`, strips the prefix,
uppercases the remainder, and injects the key/value pair as an environment variable into the
`JavaCalls` execution context — exactly as if it had been provided in the `environmentVariables`
node of a `/call` payload. Header-supplied variables are merged with any variables already in the
payload; payload variables take precedence for the same key.

This works for the REST `/call` endpoint, auto-discovered MCP tools, and the `java_call` fallback.

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
IBS.ENV.HEADER.PREFIX=my-custom-prefix-
```

Set it to blank to disable the feature entirely:

```
IBS.ENV.HEADER.PREFIX=
```

#### Interaction with `IBS.MCP.PRECHAIN`

Env vars injected from `ibs-env-*` headers are populated into `JavaCalls.environmentVariables`
inside `addHeaders()`, which is called before `submitCalls()`. Pre-chain steps that depend on
env vars (e.g., a hostname resolution step) will therefore see them.

---

### Methods intentionally excluded from the catalog

Some methods in `SimpleStaticMethods` are not included in the auto-discovery catalog:

| Method                                                         | Reason excluded                                                                 |
| -------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `overLoadedMethod1Arg(String)` and `overLoadedMethod1Arg(int)` | Both have one parameter — ambiguous, cannot be disambiguated by parameter count |
| `methodAcceptingFile(File)`                                    | `File` parameters require multi-part upload, not representable as a JSON arg    |
| Any instance method                                            | Only `public static` methods are discovered                                     |

These methods are still fully accessible via `java_call` — simply specify the class and method
name directly in the `callContent` payload.

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

IBS would embed the following catalog entries in the `java_call` description, with descriptions
sourced from Javadoc:

| Catalog entry             | Description                                                        | Args                           |
| ------------------------- | ------------------------------------------------------------------ | ------------------------------ |
| `EmailService_sendEmail`  | Sends an email to the given recipient with the specified subject.  | `arg0: string`, `arg1: string` |
| `EmailService_listInbox`  | Returns the list of message subjects in the given account's inbox. | `arg0: string`                 |
| `EmailService_purgeInbox` | Deletes all messages in the given account's inbox.                 | `arg0: string`                 |

Without `therapi-runtime-javadoc-scribe`, the descriptions would fall back to
`"Calls com.example.myproject.services.EmailService.sendEmail()"` etc.

`getStatus()` is excluded because it is an instance method.

An AI agent calling `tools/list` reads the catalog, then invokes methods via `java_call` by
placing the listed class and method values in a `callContent` entry. For instance methods,
overloaded methods, or call chaining across multiple steps, the same `java_call` payload handles
all cases — see the [Making Java Calls](../README.md#making-java-calls) section of the main README.

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

Once connected, Claude Code will discover the `java_call` tool and its method catalog via
`tools/list` at the start of each session. You can then ask Claude to call your Java methods —
for example:

> "Use java_call to call `SimpleStaticMethods.methodAcceptingStringArgument` with the argument `hello`"

To remove the server registration when you no longer need it:

```bash
claude mcp remove bridgeService
```

### Connecting from Cursor

Cursor reads MCP server configuration from `~/.cursor/mcp.json` (global) or `.cursor/mcp.json`
in the project root. The format is identical to Claude Code's config. Add an entry manually:

```json
{
  "mcpServers": {
    "CampaignTests": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "ibs-secret-login": "admin",
        "ibs-secret-pass": "mypassword",
        "ibs-secret-url": "https://my-instance.example.com/nl/jsp/soaprouter.jsp",
        "ibs-prechain": "{\"ibs_auth\":{\"class\":\"utils.CampaignUtils\",\"method\":\"setCurrentAuthenticationToLocal\",\"args\":[\"ibs-secret-url\",\"ibs-secret-login\",\"ibs-secret-pass\"]}}"
      }
    }
  }
}
```

Restart Cursor after editing the file. Cursor will call `tools/list` at session start and the
`java_call` catalog will be available to the AI.

### Other MCP clients

Any MCP client that supports the `2024-11-05` protocol version over HTTP can connect to
BridgeService. The registration format varies by client but the required values are always the
same:

| Field | Value |
|---|---|
| Transport | HTTP (stateless JSON-RPC over POST) |
| URL | `http://<host>:8080/mcp` |
| Headers | `ibs-secret-*` for credentials, `ibs-env-*` for env vars, `ibs-prechain` for per-client prechain |

Consult your client's documentation for where to place the config file and how to pass custom
HTTP headers.

---

## Best Practices

### Javadoc is your tool description — garbage in, garbage out

The AI agent sees exactly what you write in Javadoc. Nothing more, nothing less.

BridgeService embeds Javadoc comments into the `java_call` catalog at startup. When an AI reads
`tools/list`, the catalog is its only source of truth about what each method does, what its
parameters mean, and when to call it. A vague or missing description produces a vague or wrong
tool invocation.

**Treat every Javadoc comment as a prompt you are writing for the AI.**

| Weak | Why it fails | Better |
|---|---|---|
| `/** Creates a recipient. */` | No context — the AI cannot tell when or why | `/** Creates a randomly generated test recipient in the nms:recipient schema and returns its internal ID. */` |
| `/** @param auth the auth */` | Circular — adds no information | `/** @param auth Authentication object returned by setCurrentAuthenticationToLocal */` |
| `/** Sends email. */` | Too generic — ambiguous in a multi-tool session | `/** Sends the prepared delivery to all recipients in its target list and returns the delivery log ID. */` |
| No `@param` tags | AI has to guess argument purpose and order | One `@param` per argument, describing what value is expected |

**What to include in every exposed method's Javadoc:**

1. **What the method does** — in domain terms, not implementation terms.
2. **What it returns** — the type and meaning of the return value.
3. **What each parameter expects** — use `@param` tags; BridgeService uses them as argument descriptions in the tool schema.
4. **When to use it vs similar methods** — if overloads or related methods exist, say which scenario each is for.

**The quality gate enforces the minimum bar.** `IBS.MCP.REQUIRE_JAVADOC=true` (the default)
silently drops any method with no Javadoc from the catalog entirely — it will not appear in
`tools/list` and cannot be called via auto-discovery. Passing the gate (a non-empty comment)
is necessary but not sufficient: a one-word description passes the gate but still produces a
useless tool entry.

**Good Javadoc pays compound interest.** A well-described method is discovered correctly the
first time, requires no follow-up prompting, and stays reliable as the AI session context
grows. Poor descriptions lead to incorrect calls, wasted round-trips, and subtle bugs that are
hard to trace back to a missing `@param`.
