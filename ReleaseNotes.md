# Integro Bridge Service - RELEASE NOTES
## 2.11.11 (SNAPSHOT)
This is the first version that is on the public github. Hence we have the issue numbers being reset.
* [#34 java.lang.LinkageError: loader constraint violation: loader 'app' (instance of jdk.internal.loader.ClassLoaders](https://github.com/adobe/bridgeService/issues/34): Discovered a problem with Linkage error when using the traditional class loader. We now load everything locally, this means that all static variables are stored in the context.
* [#39 Object not found should be error 404](https://github.com/adobe/bridgeService/issues/39)
* [#40 Manage server side exceptions](https://github.com/adobe/bridgeService/issues/40)
* [#43 Introduce automatic loading of classes](https://github.com/adobe/bridgeService/issues/43)

## 2.11.10
* [#57 Call Timeout Management](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/57): Introducing the timeout, mechanism. We have a global timeout, and a call session timeout. We also log the individual call durations in the return payload.
* [#58 Object Instance Management](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/58): We can now instantiate objects and call their instance methods.
* [#60 Integro Independance](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/60): We are now completely independant of integro as a library. For testing purposes we now use dedicated classes that are available in the module "bridgeService-data". This can be included in a deployment by setting `demo.project.mode=compile`.
* [#63 multi-module solution](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/63): Moved to a multi-module approach. This we can better manage and maintain the dependant test classes.
* [#21 Public GitHub](https://github.com/adobe/bridgeService/issues/21): Migrated from the Adobe Enterprise artefactory to the public GitHub, adapted deployment scripts and devOps methods.
* [#20 Available JavaDoc](https://github.com/adobe/bridgeService/issues/20): Made the javaDoc available.

## 2.11.5
* [#56 Java 11 Compatibility](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/56).Integro Bridge Service is now Java 11 compatible. From now on the standard builds are in Java 11. (Java 8 is also available, but is made on demand).
* [#48 Dynamically load all called classes](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48). Previously, we were loading a preset set of packages for managing the static variable contexts in calls. We now automatically include the classes that are being called. This solves the following issues:
  * [#41 Ensuring that the system value handler is included in the static paths](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/41) : We were discovering that static variables are not automatically included in the static cpath context.
  * [#47 Issue with Server=null when calling PushNotifications with IBS](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/47) : We discovered that when the environment variables are in one context, and that the following java call isn't, we would not have access to those variables.
* Renamed Configuration class `com.adobe.campaign.tests.service.ConfigValueHandler` to `com.adobe.campaign.tests.bridge.service.ConfigValueHandlerIBS`. This is to avoid confligt with projects using the same design pattern.
* Added Continuous Integration Scripts.

## 2.0.4
* [#44 Environment variables are always strings](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/44). Facing issues with integegers in environment variables not being picked up, we decided to force all environment variables as Strings.
  * We now include two new Exceptions:
    * IBSConfigurationException : thrown whenever the configuration we set (currently only for the setting of the environment variables) does not match an existing class/method.
    * IBSRunTimeException : When for some reason the payload has issues non-related to JSON.

## 2.0.0
* Implementation of the Injection model. IntegroBridgeService is added as a dependency in projects and can now be spawned in any environment.
  * [#6 Move from an aggregator model to an insertion model. From now on IBS is put in the libraries that we want to interface.](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/6)
  * [#27 externalizing environment variables](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/27)
  * [#29 Updated the README to include clearer information about call chaining](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/29)
  * [#32 The classpath is no longer hard-coded](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/32)
  * [#26 introduced continuous integration](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/26)
  * [#36 Allow projects injecting the IBS to append their version number to the IBS test end point](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/36)
  * [#31 Make SSL params in main configurble](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/31)

## 1.0.0
* First working version. Using Aggregator model, containing Integro-ACC
