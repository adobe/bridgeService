# Developer Docs
This document deals with the general technical elements of the project.

## Error Handling
We currently manage the following error types.

| Error Code | Exceptions                                                                                                                               | Description                                                |
|------------|------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| 404        | NonExistentJavaObjectException , JsonProcessingException, AmbiguousMethodException, JavaObjectInaccessibleException, IBSPayloadException | Errors due to client side manipulations, and payloads.     |
| 408        | IBSTimeOutException                                                                                                                      | When the underlying java call takes too long to execute.   |
| 500        | IBSConfigurationException, IBSRunTimeException, TargetJavaMethodCallException                                                            | Errors happening when calling the underlying Java classes. |

## Managing Contexts and Static Variables
We have three modes of Integrity management :
* Automatic (disabled for now)
* Semi-Manual
* Manual

These modes can be set by changing the system property `IBS.CLASSLOADER.AUTOMATIC.INTEGRITY.INJECTION`.

### Automatic Loading (Disabled for now)
Automatic loading assumes that all classes that are accessed by the called system should be loaded in the context. In this case we do not need to specify what should be managed by the local class loader and what by the standard loader. The drawback of this is that some more memory will be needed (5%).

This is the default behavior, and it kicks in even when a wrong value is given for the execution property `IBS.CLASSLOADER.AUTOMATIC.INTEGRITY.INJECTION`.

### Manual Loading
In some cases we want to have control over the classes and packages that are loaded into the Class Loader. In this case we load a minimal number of classes in the IBS classLoader. The drawback of this is that the controls are a little more complex.

Semi-Manual Loading will statically load the classes you call.

#### Managing Environment Variables and Calls
This page describes the state of how Calls access static variables. The structure is the following:
1. Environment Variable Setting
2. Calling Java
3. (optional) Environment Variable Setting
4. Calling Java

## HTTP Framework

BridgeService uses [Javalin 6](https://javalin.io/) as its HTTP framework (since release 3.12). Javalin 6 runs on
Java 11–21 and ships Jetty 11 with the `jakarta.*` namespace.

### Migrating from earlier versions (Spark Java)

Before 3.12, BridgeService used Spark Java 2.9.4 (Jetty 9, `javax.*` namespace). The key differences:

| Area | Before 3.12 (Spark) | 3.12+ (Javalin 6) |
|---|---|---|
| HTTP framework | `spark-core:2.9.4` | `io.javalin:javalin:6.x` |
| Jetty version | Jetty 9.4 | Jetty 11 |
| Servlet namespace | `javax.servlet` | `jakarta.servlet` |
| `startServices()` return type | `void` | `Javalin` (store for `app.stop()`) |
| SSL configuration | `secure(keystore, ...)` | `SslPlugin` via `config.registerPlugin(...)` |
| Multipart | `req.raw().getParts()` + `javax.servlet` multipart config | `ctx.req().getParts()` + `jakarta.servlet.MultipartConfigElement` |
| Route handlers | `get("/path", (req, res) -> ...)` | `app.get("/path", ctx -> ...)` |
| Exception handlers | `exception(Ex.class, (e, req, res) -> ...)` | `app.exception(Ex.class, (e, ctx) -> ...)` |

If your project depends on BridgeService in Injection Model and uses `javax.servlet-api`, update it to use
`jakarta.servlet-api` (or the version bundled by Jetty 11) when upgrading to 3.12+.

## Log Handling
The logs are by default deleted after a certain time. In production, we have opted for the following rules:
* They are stored in the directory 'ibs_output'
* When the size becomes larger than 50MB, we archive the contents to a zip file
* The zip files are deleted on a regular basis based on the following rules:
  * The files exceed 3GB 
  * The files are more than 10 days old.
