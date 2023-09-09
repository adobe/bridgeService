# Developer Docs
This document deals with the general technical elements of the project.

## Error Handling
We currently manage the following error types.

| Error Code | Exceptions                                                                         | Description                                                |
|------------|------------------------------------------------------------------------------------|------------------------------------------------------------|
| 404        | NonExistentJavaObjectException , JsonProcessingException, AmbiguousMethodException | Errors due to client side manipulations, and payloads.     |
| 408        | IBSTimeOutException                                                                | When the underlying java call takes too long to execute.   |
| 500        | IBSConfigurationException, IBSRunTimeException, TargetJavaMethodCallException      | Errors happening when calling the underlying Java classes. |

