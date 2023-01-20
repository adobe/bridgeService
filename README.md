# integroBridgeService
This project allows you to expose Integro ACC as a REST service. It allows you to make calls to Java code.

## Getting Started
The best way to start this in the beginning is to simply start the program:

```mvn exec:java -Dexec.args="test"```

This will make the service available under :
```http://localhost:4567```

We are working on implementing a universal service, but for now this is simplest way to start.

## Testing That all is Working
All you need to do is to call :
```/test```

If all is good you should get:
```All systems up```

## Testing That all External Dervices can be Accessed
One of the added values of this service is to create a single point of access for external dependencies. However, this needs to be checked, before using this service. In order to do this you need to the following POST call:

```
/service-check
```

The payload needs to have the following format:

```JSON
{
    "<URL ID 1>": "<dns 1>:<Port>",
    "<URL ID 2>": "<dns 2>:<Port>"
}
```

The payload returns a JSON with the test results:
```JSON
{
    "<URL ID 1>": true,
    "<URL ID 2>": false
}
```

In the example above "<URL ID 2>" is marked as false because it can not be accessed from the BridgeService.

## Making a basic Java Call
The simplest java call is done in the following way:
```/call```

```JSON
{
    "callContent": {
        "<ID>": {
            "class": "<package name>.<class Name>",
            "method": "<method name>",
            "args": ["argument1","argument2"]
        }
    }
}
```

If the IntegroBridgeService can find the method it will execute it. The result is, when successful, is encapsulated in a 'returnValues' object. 

```JSON
{
    "returnValues": {
        "<ID>": "<result>"
    }
}
```

## Call Chaining a basic Java Call
We can chain a series of java calls in the same payload:

```JSON
{
    "callContent": {
        "<ID-1>": {
            "class": "<package name 1>.<class name 1>",
            "method": "<method name 1>",
            "args": ["argument1","argument2"]
        },
        "<ID2->": {
           "class": "<package name 2>.<class name 2>",
           "method": "<method name 2>",
           "args": ["argument1","argument2"]
        }
    }
}
```

In the example above the results will be stored in the following way:

```JSON
{
    "returnValues": {
        "<ID-1>": "<result>",
        "<ID-2>": "<result>"
    }
}
```


### Call Chaining and Call Dependencies
We now have the possibility of injecting call results from one call to the other:

```JSON
{
    "callContent": {
        "<ID-1>": {
            "class": "<package name 1>.<class name 1>",
            "method": "<method name 1>",
            "args": ["argument1","argument2"]
        },
        "<ID2->": {
           "class": "<package name 2>.<class name 2>",
           "method": "<method name 2>",
           "args": ["ID-1","argument2"]
        }
    }
}
```

In the example above "ID-2" will use the return value of the call "ID-1" as ts first argument.

## Creating a Call Context
We sometimes need to set environment variables when making calls. This are usually idirectly related to the call you are doing.

```JSON
{
    "callContent": {
        "<ID>": {
            "class": "<package name>.<class Name>",
            "method": "<method name>",
            "args": ["argument1","argument2"]
        }
    },
    "environmentVariables": {
        "<ENVIRONMENT PROPERTY 1>": "<value>"
    }
}
```

When making the call we first update the environment variables for the system.

## Known Issues and Limitations
As this is a new project there are a few limitations to our solution:
### Cannot call overloaded methods with the same number of arguments.
Today, in order to simply the call model, we have chosen not to specify the argument types in the call. The consequence of this is that in the case of overloaded methods, we only pick the method with the same number of arguments. If two such overloaded methods exist, we choose to throw an exception:
``We could not find a unique method for <method>.`` 

### Only simple arguments
Since this is a REST call we can only correctly manage simple arguments in the payload. One workaround is to use Call Dependencies in Call Chaining (see above). I.e. you can pass simple arguments to one method, and use the complex results of that method as an argument for the following java call.  

### Complex Non-Serialisable Return Objects
In many cases the object a method returns is not rerializable. If that is the case we mine the object, and extract all simple values from the object.


## Building Image
In order to build an image you need to run the following command:
```
docker build --build-arg ARTIFACTORY_USER --build-arg ARTIFACTORY_API_TOKEN -t integrobridgeservice .
```

ARTIFACTORY_USER and ARTIFACTORY_API_TOKEN are locally stored credentials for connecting to your artefactory.

To run the image:

```
docker run -rm -d -p 4567:4567 integrobridgeservice
```


