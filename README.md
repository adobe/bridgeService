# integroBridgeService
This project allows you to expose Integro ACC as a REST service. It allows you to make calls to Java code.

## Making a basic Java Call
The simplest java call is done in the following way:

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

## Building Image
In order to build an image you need to run the following command:
```
docker build --build-arg ARTIFACTORY_USER --build-arg ARTIFACTORY_API_TOKEN -t integrobridgeservice .
```

ARTIFACTORY_USER and ARTIFACTORY_API_TOKEN are locally stored credentials for connecting to your artefactory.

To run the image:

```
docker run -d -p 4567:4567 integrobridgeservice
```

Test Success:
First perform a simple test:
```
http://localhost:4567/hello
```


