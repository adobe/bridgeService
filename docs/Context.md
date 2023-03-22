# Managing Contexts and Static Variables
This page describes the state of how Calls access static variables. The structure is the following:
1. Environment Variable Setting
2. Calling Java
3. (optional) Environment Variable Setting
4. Calling Java

We make a distinction of wether the second call has its own env vars or not.


| Nr  | EnvVar in Integrity Path | Call Classes in Integrity Path | SAME Call Access to EnvVar | Different Call Access to EnvVar | Current State | Expected | Comment                                                                                                                               | Test                                                                                                                |
|-----|--------------------------|--------------------------------|----------------------------|---------------------------------|---------------|----------|---------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| 1   | TRUE                     | TRUE                           | TRUE                       | FALSE                           | PASS          | PASS     | In this case our call should have access to the envvars, but the consecutive calls should not                                         | `TestFetchCalls#testIntegrityEnvVars_case1A_allPathsSet` , `TestFetchCalls#testIntegrityEnvVars_case1B_allPathsSet` |
| 2   | TRUE                     | FALSE                          | FALSE                      | FALSE                           | FAIL          | PASS     | In this case CALL-1's static variables are not permutated to that of our call. We shoudl have these values available                  | `TestFetchCalls#testIntegrityEnvVars_case2_withEnvVarPathsIncludedButNotCallPath`                                                                                                   |
| 3   | FALSE                    | TRUE                           | FALSE                      | FALSE                           | PASS          | PASS     | In this case the env vars are accessible to all calls, so we still have access to them                                                | `TestFetchCalls`                                                                                                    |
| 4   | FALSE                    | FALSE                          | TRUE                       | TRUE                            | PASS          | PASS     | In this case the context remains the same. I.e. the static variables are still present in the system, and will affect the second call | `TestFetchCalls#testStaticFieldIntegrityCore_framework_testingPackagePaths1`                                        |





