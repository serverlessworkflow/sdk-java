![Verify JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Verify%20JAVA%20SDK/badge.svg)
![Deploy JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Deploy%20JAVA%20SDK/badge.svg) [![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/serverlessworkflow/sdk-java)

# Serverless Workflow Specification - Java SDK- Reference Implementation

Welcome to Java SDK runtime reference implementation, a lightweight implementation of the Serverless Workflow specification which provides a simple, non blocking, reactive API for workflow execution. 

Although initially conceived mainly for testing purposes, it was designed to be easily expanded, so it can eventually become production ready. 

## Status. 

This reference implementation is currently capable of running workflows consisting of:


* Tasks
    * Switch 
    * Set
    * Do
    * Raise
    * Listen
    * Emit
    * Fork
    * For
    * Try
    * Wait
    * Call
        * HTTP
* Schema Validation
    * Input
    * Output
* Expressions
    * Input
    * Output 
    * Export
    * Special keywords: runtime, workflow, task...
* Error definitions


## Setup

Serverless workflow reference implementation only requires setting up Java and Maven/Gradle. 

### JDK Version

Reference implementation requires [Java 17](https://openjdk.org/projects/jdk/17/) or newer versions. 

### Dependencies

One of the goals of the reference implementation is to maintain the number of dependencies as lower as possible. With that spirit, a modular approach has been followed, letting the users decide, depending on their workflows nature, which dependencies should be include. 

In practical terms, this means a separation between the core part and additional dependencies that should be explicitly included if your workflow is interacting with an external service that communicated using a particular technology supported by the specification (at this moment, just HTTP). The intention of this is to avoid adding dependencies that you do not really need (for example, when gRPC call will be implemented, if we were adding the gRPC stack to the core dependencies, you wont be able to get rid of it even if none of your workflows use it)

#### Maven

You always need to add this dependency to your pom.xml `dependencies` section:

```xml
<dependency>
      <groupId>io.serverlessworkflow</groupId>
      <artifactId>serverlessworkflow-impl-core</artifactId>
      <version>7.0.0-SNAPSHOT</version>
</dependency>
```

And only if your workflow is using HTTP calls, you must add:

```xml
<dependency>
      <groupId>io.serverlessworkflow</groupId>
      <artifactId>serverlessworkflow-impl-http</artifactId>
      <version>7.0.0-SNAPSHOT</version>
</dependency>
```



### Gradle projects:

You always need to add this dependency to your build.gradle `dependencies` section:

```text
implementation("io.serverlessworkflow:serverlessworkflow-impl-core:7.0.0-SNAPSHOT")
```

And only if your workflow is using HTTP calls, you must add:

```text
implementation("io.serverlessworkflow:serverlessworkflow-impl-http:7.0.0-SNAPSHOT")
```


## How to use

Quick version is intended for impatient users that want to try something as soon as possible.

Detailed version is more suitable for those users interested on a more thoughtful discussion of the API.

### Quick version

For a quick introduction, we are going to use a simple workflow [definition](../examples/simpleGet/src/main/resources/get.yaml) that performs a get call. 
We are going to show two ways of invoking the workflow: 
  - blocking the thread till the get request goes through
  - returning control to the caller, so the main thread continues while the get is executed

In order to execute the workflow, blocking the thread till the http request is completed, you should write

``` java 
try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      logger.info(
          "Workflow output is {}",
          appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("get.yaml"))
              .instance(Map.of("petId", 10))
              .start()
              .join());
    }
```
You can find the complete java code [here](../examples/simpleGet/src/main/java/BlockingExample.java)

In order to execute the workflow, without blocking the calling thread till the http request is completed, you should write

``` java 
  try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("get.yaml"))
          .instance(Map.of("petId", 10))
          .start()
          .thenAccept(node -> logger.info("Workflow output is {}", node));
    }
```
When the http request is done, both examples will print a similar output

`Workflow output is {"id":10,"category":{"id":10,"name":"string"},"name":"doggie","photoUrls":["string"],"tags":[{"id":10,"name":"string"}],"status":"string"}`

You can find the complete java code [here](../examples/simpleGet/src/main/java/NotBlockingExample.java)

### Detailed version

To discuss runtime API we are going to use a couple of workflow:
- [listen.yaml](../examples/events/src/main/listen.yaml), which waits for an event reporting a temperature greater than 38
- [emit.yaml](../examples/events/src/main/emit.yaml), which emits events with a certain temperature, specified as workflow parameter.

A brief summary of what we are trying to do. We will start listen.yaml, which will complete when it receives an event with the proper temperature, but it wont block the main thread while waiting for it. Then, we will send an event with a lower temperature, that will be ignored. And finally, we will send an event with a greater temperature, that will complete the waiting workflow. 

The first step is to create a [WorkflowApplication](core/src/main/java/io/serverlessworkflow/impl/WorkflowApplication.java) instance. An application is an abstraction that allow customization of different aspect of the workflow execution (for example change the default `ExecutorService` for thread spawning)

Since `WorkflowApplication` implements `Autocloseable`, we better use a try...finally block, ensuring any resource that might have been used by the workflow is freed when done. 

`try (WorkflowApplication appl = WorkflowApplication.builder().build())`

Once we have the application object, we use it to parse our definition examples. To load each workflow definition, we use `readFromClasspath` helper method defined in [WorkflowReader](api/src/main/java/io/serverlessworkflow/api/WorkflowReader.java) class.

```java
      WorkflowDefinition listenDefinition =
          appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("listen.yaml"));
      WorkflowDefinition emitDefinition =
          appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("emit.yaml")); 
```

A [WorkflowDefinition](core/src/main/java/io/serverlessworkflow/impl/WorkflowDefinition.java) object is immutable and therefore thread safe. It is used to execute as many workflow instances as desired. 

To execute a workflow, we first create a [WorkflowInstance](core/src/main/java/io/serverlessworkflow/impl/WorkflowInstance.java) object (the initial status is PENDING) and then invoke `start` method on it (the status is changed to RUNNING). `start` method returns a [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html), which we use to indicate that a log message should be printed when the workflow is completed.

```java
  WorkflowInstance waitingInstance = listenDefinition.instance(Map.of());
      waitingInstance
          .start()
          .thenAccept(node -> logger.info("Waiting instance completed with result {}", node));
```

The next line will be executed as soon as the workflow execution starts waiting for events to arrive, moment at which control is returned to the calling thread. Therefore, we can execute another workflow instance while the first one is waiting. 

We are going to send an event with a temperature that does not satisfy the criteria, so the listen instance will continue waiting. To pass parameters to the workflow instance that sends the event, we use a regular Java `Map`. Notice that, since we want to wait till the event is published before executing the next line, we call `join` after `start`, telling the `CompletableFuture` to wait for workflow completion.

```java
 emitDefinition.instance(Map.of("temperature", 35)).start().join();
 ```
 
 Now its time to complete the waiting instance and send an event with the expected temperature. We do so by reusing `emitDefinition`.

```java
 emitDefinition.instance(Map.of("temperature", 39)).start().join();
 ```
 
After that, listen instance will be completed and we will see this log message

```java
[pool-1-thread-1] INFO events.EventExample - Waiting instance completed with result [{"temperature":39}]
```
The source code of the example is here (../examples/events/src/main/java/EventExample.java)

