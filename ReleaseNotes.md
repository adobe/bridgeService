# Bridge Service - RELEASE NOTES

## 2.11.15 - SNAPSHOT
* [#72 Sending root stack trace](https://github.com/adobe/bridgeService/issues/72). With issue #9 we discovered that the stack trace should be that of the original cause.
* [#74 Trimming error messages](https://github.com/adobe/bridgeService/issues/74). We now trim the error messages. 


## 2.11.14
* [#61 Allowing Map/JSON return type](https://github.com/adobe/bridgeService/issues/61). Until recently we were not able to correctly accept map and JSON Objects.
* [#63 Fixed bug when return object has methd called get](https://github.com/adobe/bridgeService/issues/63). Corrected bug related to classes that have a method called get. In these cases we have a 500 error. We have now added an error category which is internal.
* [#9 Include error stack trace](https://github.com/adobe/bridgeService/issues/9). The stack trace is part of the error object. We have decided not tolter, and simply add the error stadk trace.
* [#64 Managing internal errors better](https://github.com/adobe/bridgeService/issues/64). We now deal with ibs-internal error so that we can better debug them.

## 2.11.13
DEPRECATED due to false merge

## 2.11.12
* Disabled automatic mode as discovered some issues with the loading (Issue #55).

## 2.11.11
* [#43 Introduce automatic loading of classes](https://github.com/adobe/bridgeService/issues/43): Instead of selectively loading classes from designated packages, we now load all needed classes in the class loader.
* [#34 java.lang.LinkageError: loader constraint violation: loader 'app' (instance of jdk.internal.loader.ClassLoaders](https://github.com/adobe/bridgeService/issues/34): Discovered a problem with Linkage error when using the traditional class loader. We now load everything locally, this means that all static variables are stored in the context (Issue #43).
* We started reorganizing the errors, and have started doing a more predefined error management. For more info please refer to the [Technical Documentation](./docs/Technical.md) .
* [#35 When calling a class with no modifiers we do not get enough information](https://github.com/adobe/bridgeService/issues/35)
* [#37 Better error formatting](https://github.com/adobe/bridgeService/issues/37) We now end errors as a JSON data including errors and messages.
* [#36 Make the /test endpoint return a JSON](https://github.com/adobe/bridgeService/issues/37) By sending the /test data as json we make it more legible.
* [#38 Better information if the deployment port is already in use ](https://github.com/adobe/bridgeService/issues/38) Whenever we are starting the bridgeService on a port that is already in use, we throw a IBSConfigurationException exception.
* Merged the document `./docs/Context.md` into the `./docs/Technical.md` document.

## 2.11.10
This is the first version that is on the public github. Hence we have the issue numbers being reset. Old issue titles are marked in bold:
* **Call Timeout Management**: Introducing the timeout, mechanism. We have a global timeout, and a call session timeout. We also log the individual call durations in the return payload.
* **Object Instance Management** : We can now instantiate objects and call their instance methods.
* **Integro Independance**: We are now completely independant of integro as a library. For testing purposes we now use dedicated classes that are available in the module "bridgeService-data". This can be included in a deployment by setting `demo.project.mode=compile`.
* **multi-module solution**: Moved to a multi-module approach. This we can better manage and maintain the dependant test classes.
* **Public GitHub**: Migrated from the Adobe Enterprise artefactory to the public GitHub, adapted deployment scripts and devOps methods.
* **Available JavaDoc**: Made the javaDoc available.

## 2.11.5
* **Java 11 Compatibility**: Integro Bridge Service is now Java 11 compatible. From now on the standard builds are in Java 11. (Java 8 is also available, but is made on demand).
* **Dynamically load all called classes**: Previously, we were loading a preset set of packages for managing the static variable contexts in calls. We now automatically include the classes that are being called. This solves the following issues:
  * **Ensuring that the system value handler is included in the static paths**: We were discovering that static variables are not automatically included in the static cpath context.
  * **Issue with Server=null when calling PushNotifications with IBS**: We discovered that when the environment variables are in one context, and that the following java call isn't, we would not have access to those variables.
* Renamed Configuration class `com.adobe.campaign.tests.service.ConfigValueHandler` to `com.adobe.campaign.tests.bridge.service.ConfigValueHandlerIBS`. This is to avoid confligt with projects using the same design pattern.
* Added Continuous Integration Scripts.

## 2.0.4
* **Environment variables are always strings**: Facing issues with integegers in environment variables not being picked up, we decided to force all environment variables as Strings.
  * We now include two new Exceptions:
    * IBSConfigurationException : thrown whenever the configuration we set (currently only for the setting of the environment variables) does not match an existing class/method.
    * IBSRunTimeException : When for some reason the payload has issues non-related to JSON.

## 2.0.0
* Implementation of the Injection model. IntegroBridgeService is added as a dependency in projects and can now be spawned in any environment.
  * Move from an aggregator model to an insertion model. From now on IBS is put in the libraries that we want to interface.
  * externalizing environment variables
  * Updated the README to include clearer information about call chaining
  * The classpath is no longer hard-coded
  * introduced continuous integration
  * Allow projects injecting the IBS to append their version number to the IBS test end point
  * Make SSL params in main configurble

## 1.0.0
* First working version. Using Aggregator model, containing Integro-ACC
