# Developer Docs
This document deals with the general technical elements of the project.

## Error Handling
We currently manage the following error types.

| Error Code | Exceptions                                                                         | Description                                                |
|------------|------------------------------------------------------------------------------------|------------------------------------------------------------|
| 404        | NonExistentJavaObjectException , JsonProcessingException, AmbiguousMethodException | Errors due to client side manipulations, and payloads.     |
| 408        | IBSTimeOutException                                                                | When the underlying java call takes too long to execute.   |
| 500        | IBSConfigurationException, IBSRunTimeException, TargetJavaMethodCallException      | Errors happening when calling the underlying Java classes. |

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