# BridgeService
# PhasedTesting
[![unit-tests](https://github.com/adobe/bridgeService/actions/workflows/onPushSimpleTest.yml/badge.svg)](https://github.com/adobe/bridgeService/actions/workflows/onPushSimpleTest.yml)
[![codecov](https://codecov.io/gh/adobe/bridgeService/branch/main/graph/badge.svg?token=GSi0gUlqq5)](https://codecov.io/gh/adobe/bridgeService)
[![javadoc](https://javadoc.io/badge2/com.adobe.campaign.tests.bridge.service/integroBridgeService/javadoc.svg)](https://javadoc.io/doc/com.adobe.campaign.tests.bridge.service/integroBridgeService)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=adobe_bridgeService&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=adobe_bridgeService)

This project allows you to expose your Java project/library as a REST service. It allows you to make calls to Java code from any language or framework you are in.

## Table of Contents
- [Release Notes](#release-notes)
- [Implementing The Bridge Service in Your Project](#implementing-the-bridge-service-in-your-project)
    * [Adding the Bridge Service to Your Project](#adding-the-bridge-service-to-your-project)
        - [Installation](#installation)
        + [Considerations](#considerations)
    * [Including your project in the BridgeService](#including-your-project-in-the-bridgeservice)
- [Starting the Bridge Service](#starting-the-bridge-service)
    * [Running the Bridge Locally](#running-the-bridge-locally)
    * [Running a DEMO](#running-a-demo)
- [Setting Information About your Environment](#setting-information-about-your-environment)
- [Testing That all is Working](#testing-that-all-is-working)
- [Testing That all External Dervices can be Accessed](#testing-that-all-external-dervices-can-be-accessed)
- [Making a basic Java Call](#making-a-basic-java-call)
- [Instantiating Objects](#instantiating-objects)
- [Managing Timeouts](#managing-timeouts)
    * [Setting Timeout Globally](#setting-timeout-globally)
    * [Setting a Timeout for the Call Session](#setting-a-timeout-for-the-call-session)
- [Call Chaining a basic Java Call](#call-chaining-a-basic-java-call)
    * [Call Chaining and Call Dependencies](#call-chaining-and-call-dependencies)
    * [Call Chaining and Instance Methods](#call-chaining-and-instance-methods)
- [Creating a Call Context](#creating-a-call-context)
    * [Static Variable Scopes](#static-variable-scopes)
        + [Session Scopes](#session-scopes)
        + [Product Scope](#product-scope)
- [Error Management](#error-management)
- [Contribution](#contribution)
- [Known Errors](#known-errors)
    * [Linked Error](#linked-error)
- [Known Issues and Limitations](#known-issues-and-limitations)
    * [Cannot call overloaded methods with the same number of arguments.](#cannot-call-overloaded-methods-with-the-same-number-of-arguments)
    * [Only simple arguments](#only-simple-arguments)
    * [Complex Non-Serialisable Return Objects](#complex-non-serialisable-return-objects)
    * [Calling Enum Methods](#calling-enum-methods)

## Release Notes
The release notes can be found [here](ReleaseNotes.md).

## Implementing The Bridge Service in Your Project
The bridge service can be used in two ways:
* By adding it to the project you want to expose (recommended), 
* By including your project as a dependency to the Bridge cervice deployed.

### Adding the Bridge Service to Your Project
We think it is simplest to the BridgeService dependency to your project. This allows you to only update your code when needed, and you do not need to create a new release/snapshot everytime your project changes.

![BridgeService Injection Model](diagrams/Processes-injectionModel.drawio.png)

When starting the bridge service you need to run the following command line:

```
mvn compile exec:java -Dexec.mainClass=MainContainer -Dexec.args="test"
```

##### Installation
The following dependency needs to be added to your pom file:

```
 <dependency>
    <groupId>com.adobe.campaign.tests.bridge.service</groupId>
    <artifactId>integroBridgeService</artifactId>
    <version>2.11.14</version>
</dependency>
```

#### Considerations
Since the BridgeService uses Jetty and java Spark, it is quite possible that there maybe conflicts in the project when you add this library. Most importantly you will need to ensure that `javax.servlet` is set to "**compile**" in your maven scope.

We have found it simplest to simply add that library directly in the pom file with the scope "**compile**".

### Including your project in the BridgeService
In this model you can simply add your project as a dependency to the BridgeProject.

![BridgeService Aggregator Model](diagrams/Processes-aggregatorModel.drawio.png)

## Starting the Bridge Service
When deploying this as a project we run this as an executable jar. This is usually done in a Docker image.

### Running the Bridge Locally
You can also run the project locally to debug your project. To do this, you need to run the following command line:

from the root project:
```mvn -pl integroBridgeService exec:java -Dexec.args="test"```

or directly from the module "integroBridgeService":
```mvn exec:java -Dexec.args="test"```

This will make the service available under :
```http://localhost:8080```

### Running a DEMO
The bridge service can be launched in this project with a demo project (Included in an aggregator mode). When deployed in this mode, we include the module `bridgeService-data` which is part of this project. If you want to include this project in your deployment you need to set the property `demo.project.mode` to `compile`. 

from the root project:
```mvn -pl integroBridgeService exec:java -Dexec.args="test" -Ddemo.project.mode=compile ```

or directly from the module "integroBridgeService":
```mvn exec:java -Dexec.args="test" -Ddemo.project.mode=compile```


## Setting Information About your Environment
The users accessing bridge service will encounter two different technologies:
* The Bridge Service
* The Host Project

In order to make provide context information the Bridge Service will always let you know which version it is. However, we need to let the users know about the version of the host project. Ths version number can be set by setting the environment property `IBS.PRODUCT.USER.VERSION`.

## Testing That all is Working
All you need to do is to call :
```/test```

If all is good you should get:
```
All systems up - in production
Version : 2.11.14
Product user version : 7.0
```


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
    },
    "callDurations": {
        "<ID>": "<duration ms>"
    }
}
```

## Instantiating Objects
If we do not specify a method, the bridge service assumes that we are instantiating the given class:
```JSON
{
    "callContent": {
        "<ID>": {
            "class": "<package name>.<class Name>",
            "args": ["argument1","argument2"]
        }
    }
}
```

**Note :** You can also set the method name to the class name, but it may be easier to simply skip setting a method name in this case.

## Managing Timeouts
As of version 2.11.6 we now introduce the notion of timeouts. This means that after a declared time a call will be interrupted. Setting this value can be done at two levels:
* The deployment level
* The Call session

**Note :** If set to 0, there is no timeout.

### Setting Timeout Globally
You can set a default value when starting the service. This is done by setting the environment variable `IBS.TIMEOUT.DEFAULT`. If not set, the default value is 10000ms.

### Setting a Timeout for the Call Session
We can also set the Timeout for a java call transaction. In that case the value you pass overrides the global value, but only for you session. If the `timeout` is not test in the payload at the next call, the global value will be used. 

In the example below the method `methodWithTimeOut` waits for the provided, in this case 800ms, amount of time. In the example below the test will pass because we wait for 800ms, and the timeout is 1000s.  
```JSON
{
	"callContent": {
		"call1": {
			"class": "com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods",
			"method": "methodWithTimeOut",
			"args": [
				800
			]
		}
	},
	"timeout": 1000
}
```

If the payload above would have a timeout below 800ms, the call will fail.

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
        "<ID-2>": {
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
    },
    "callDurations": {
        "<ID-1>": "<duration ms>",
        "<ID-2>": "<duration ms>"
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
        "<ID-2>": {
           "class": "<package name 2>.<class name 2>",
           "method": "<method name 2>",
           "args": ["<ID-1>","argument2"]
        }
    }
}
```

In the example above "ID-2" will use the return value of the call "ID-1" as ts first argument.


**NOTE** : When passing a call result as an argument, it needs to be a String. In many languages such as JavaScript, the JSON keys need not be a string, however, for this to work you need to pass the ID as a string.  

### Call Chaining and Instance Methods
We now have the possibility of injecting call results from one call to the other. In the example below we instantiate an object, and in the following call we call a method of that object. This is done by passing the ID of the first call as the `instance` value for the following call.

```JSON
{
    "callContent": {
        "<ID-1>": {
           "class": "<package name>.<class name>",
           "args": ["argument1","argument2"]
        },
        "<ID-2>": {
           "class": "<ID-1>",
           "method": "<method name>",
           "args": ["argument2"]
        }
    }
}
```

In the example above "ID-2" will use call the instance method of the object created in call "ID-1".

## Creating a Call Context
We sometimes need to set environment variables when making calls. This is usually indirectly related to the call you are doing. These variable mirror the values of a property file, so they are always treated as strings.

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

This call will use your internal method for setting environment variables. This method can be set by setting the following environment values, when activating the Bridge on your project:
* IBS.ENVVARS.SETTER.CLASS
* IBS.ENVVARS.SETTER.METHOD

### Static Variable Scopes
One of our main concerns has been the management of static variables. For the sake of conversation we need to identify scopes:
* **Session Scope** : Access to the variables in the same call to the Bridge Service
* **Product Scope** : Access to the variables between two different calls

#### Session Scopes
For now our approach is that two distinct calls to the same Bridge Service node, should share little or no variables. Another way of defining this is that two calls should not interfere with one another. In a Session Context we have two use cases:

We have covered all the use cases in the document [Managing Contexts and Static Variables](./docs/Technical.md#managing-contexts-and-static-variables).

#### Product Scope
Although we do not, yet, provide tools for managing variables that are valid for all calls to the IBS, we can define a series or local environment variables are deployment of the service. This can be done in two ways:
* Managing a properties for in your deployment
* Injecting Runtime properties at the commandline

## Error Management
Currently, whenever there is an error in the underlying java call we will include the orginal error message in the error response. For example, for the call:

```json
{
    "callContent": {
        "call1": {
            "class": "com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods",
            "method": "methodThrowingException",
            "returnType": "java.lang.String",
            "args": [
                3,
                3
            ]
        }
    }
}
```

We would normally get the error:
```
java.lang.IllegalArgumentException: We do not allow numbers that are equal.
```

When using the bridge service, we also include additional info:

```JSON
{
  "title": "Error during call of target Java Class and Method.",
  "code": 500,
  "detail": "We experienced an exception when calling the provided method com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods.methodThrowingException.\nProvided error message : java.lang.IllegalArgumentException: We do not allow numbers that are equal.",
  "bridgeServiceException": "com.adobe.campaign.tests.bridge.service.exceptions.TargetJavaMethodCallException",
  "originalException": "java.lang.IllegalArgumentException",
  "originalMessage": "We do not allow numbers that are equal."
}
```

The BridgeService exception is how the bridgeService manages underlying errors. However we also share the background, originating exception and message in order to help.

## Contribution
There are two main docs for contributing:
* [The Contributing Doc](CONTRIBUTING.md) For general contribution rules.
* [The Technical Doc](docs/Technical.md) For general technical aspects.

## Known Errors

### Linked Error
When using the "manual" mode, this error can happen when you are chaining calls, and that the called classes call a third class in a static way. In such cases you will get an error like:

```
Problems with payload. Check the passed environment variables.
java.lang.LinkageError: loader  constraint violation: when resolving method "com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.MiddleManClassFactory.getMarketingInstance()Lcom/adobe/campaign/tests/bridge/testdata/issue34/pckg1/MiddleMan;" the class loader com.adobe.campaign.tests.bridge.service.IntegroBridgeClassLoader @697a1a68 (instance of com.adobe.campaign.tests.bridge.service.IntegroBridgeClassLoader, child of 'app' jdk.internal.loader.ClassLoaders$AppClassLoader) of the current class, com/adobe/campaign/tests/bridge/testdata/issue34/pckg1/CalledClass2, and the class loader 'app' (instance of jdk.internal.loader.ClassLoaders$AppClassLoader) for the method's defining class, com/adobe/campaign/tests/bridge/testdata/issue34/pckg2/MiddleManClassFactory, have different Class objects for the type com/adobe/campaign/tests/bridge/testdata/issue34/pckg1/MiddleMan used in the signature
```

This usually means that some additional packages need to be included in the environment variable: `IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES`.


## Known Issues and Limitations
As this is a new project there are a few limitations to our solution:
### Cannot call overloaded methods with the same number of arguments.
Today, in order to simply the call model, we have chosen not to specify the argument types in the call. The consequence of this is that in the case of overloaded methods, we only pick the method with the same number of arguments. If two such overloaded methods exist, we choose to throw an exception:
``We could not find a unique method for <method>.`` 

### Only simple arguments
Since this is a REST call we can only correctly manage simple arguments in the payload. One workaround is to use Call Dependencies in Call Chaining (see above). I.e. you can pass simple arguments to one method, and use the complex results of that method as an argument for the following java call.  

### Complex Non-Serialisable Return Objects
In many cases the object a method returns is not rerializable. If that is the case we mine the object, and extract all simple values from the object.

### Calling Enum Methods
We are currently unable to call enums with the Bridge Service.



