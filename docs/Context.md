# Managing Contexts and Static Variables

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




