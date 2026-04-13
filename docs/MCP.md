# Testing and Using BridgeService as an MCP Server

This document explains how to run BridgeService as an MCP server and verify it works, first
using the built-in demo data, then from an external project that hosts its own Java library.

## Table of Contents

- [Testing with the built-in demo data (bridgeService-data)](#testing-with-the-built-in-demo-data)
  - [Starting the server](#starting-the-server)
  - [MCP handshake](#mcp-handshake)
  - [Discovering tools](#discovering-tools)
  - [Calling tools](#calling-tools)
  - [Tools that are intentionally excluded](#tools-that-are-intentionally-excluded)
- [Exposing your own project as MCP tools](#exposing-your-own-project-as-mcp-tools)
  - [Injection model — adding IBS to your project](#injection-model--adding-ibs-to-your-project)
  - [Aggregator model — adding your project to IBS](#aggregator-model--adding-your-project-to-ibs)
  - [Configuring tool discovery](#configuring-tool-discovery)
  - [Surfacing Javadoc as tool descriptions](#surfacing-javadoc-as-tool-descriptions)
  - [What tools will be generated](#what-tools-will-be-generated)
- [Connecting to Claude Code](#connecting-to-claude-code)
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
          "properties": { "arg0": { "type": "string", "description": "the input string" } },
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
          "properties": { "arg0": { "type": "integer", "description": "the input integer" } },
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
        "inputSchema": { "..." : "see README" }
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
    "content": [{ "type": "text", "text": "{\"returnValues\":{\"result\":\"_Success\"},\"callDurations\":{\"result\":2}}" }],
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
    "content": [{ "type": "text", "text": "{\"returnValues\":{\"result\":\"hello_Success\"},\"callDurations\":{\"result\":1}}" }],
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

### Tools that are intentionally excluded

Some methods in `SimpleStaticMethods` are not exposed as auto-discovered tools:

| Method | Reason excluded |
|---|---|
| `overLoadedMethod1Arg(String)` and `overLoadedMethod1Arg(int)` | Both have one parameter — ambiguous, cannot be disambiguated by parameter count |
| `methodAcceptingFile(File)` | `File` parameters require multi-part upload, which is not supported by the MCP tool schema |
| Any instance method | Only `public static` methods are discovered |

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

| Tool name | Description | Parameters |
|---|---|---|
| `EmailService_sendEmail` | Sends an email to the given recipient with the specified subject. | `arg0: string`, `arg1: string` |
| `EmailService_listInbox` | Returns the list of message subjects in the given account's inbox. | `arg0: string` |
| `EmailService_purgeInbox` | Deletes all messages in the given account's inbox. | `arg0: string` |

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
Claude Code. Use `--scope user` to make it available globally across all projects, or omit it
to register it only for the current project:

```bash
# Register globally (available in all Claude Code sessions)
claude mcp add --transport http bridgeService http://localhost:8080/mcp --scope user

# Register for the current project only
claude mcp add --transport http bridgeService http://localhost:8080/mcp
```

This writes an entry to `~/.claude/mcp.json` (global) or `.mcp.json` in the project root
(project-scoped). The resulting config entry looks like:

```json
{
  "mcpServers": {
    "bridgeService": {
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
