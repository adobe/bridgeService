# Managing Contexts and Static Variables
We have three modes of Integrity management :
* Automatic
* Semi-Manual
* Manual

These modes can be set by changing the system property `IBS.CLASSLOADER.AUTOMATIC.INTEGRITY.INJECTION`.

## Automatic Loading
Automatic loading assumes that all classes that are accessed by the called system should be loaded in the context. In this case we do not need to specify what should be managed by the local class loader and what by the standard loader. The drawback of this is that some more memory will be needed (5%).

This is the default behavior, and it kicks in even when a wrong value is given for the execution property `IBS.CLASSLOADER.AUTOMATIC.INTEGRITY.INJECTION`.

## Manual Loading
In some cases we want to have control over the classes and packages that are loaded into the Class Loader. In this case we load a minimal number of classes in the IBS classLoader. The drawback of this is that the controls are a little more complex.

Semi-Manual Loading will statically load the classes you call.

## Managing Environment Variables and Calls 
This page describes the state of how Calls access static variables. The structure is the following:
1. Environment Variable Setting
2. Calling Java
3. (optional) Environment Variable Setting
4. Calling Java

We make a distinction of weather the second call has its own env vars or not.

| Nr  | EnvVar in Integrity Path | Call Classes in Integrity Path | SAME Call Access to EnvVar | Different Call Access to EnvVar | Current State       | Expected | Comment                                                                                                                               | Test                                                                                      |
|-----|--------------------------|--------------------------------|----------------------------|---------------------------------|---------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| 1   | TRUE                     | TRUE                           | TRUE                       | FALSE                           | PASS                | PASS     | In this case our call should have access to the envvars, but the consecutive calls should not                                         | `TestFetchCalls#testIntegrityEnvVars_case1_allPathsSet_rawMode`                           |
| 2   | TRUE                     | FALSE                          | FALSE                      | FALSE                           | FAIL (Fixed in #48) | PASS     | In this case CALL-1's static variables are not permutated to that of our call. We shoudl have these values available                  | `TestFetchCalls#testIntegrityEnvVars_case2_withEnvVarPathsIncludedButNotCallPath_rawMode` |
| 3   | FALSE                    | TRUE                           | TRUE                       | TRUE                            | PASS                | PASS     | In this case the env vars are accessible to all calls, so we still have access to them                                                | `TestFetchCalls#testIntegrityEnvVars_case3B_allPathsSet_rawMode`                                                                         |
| 4   | FALSE                    | FALSE                          | TRUE                       | TRUE                            | PASS                | PASS     | In this case the context remains the same. I.e. the static variables are still present in the system, and will affect the second call | `TestFetchCalls#testIntegrityEnvVars_case4_noPackagesInIntegrityPath_rawMode`              |




