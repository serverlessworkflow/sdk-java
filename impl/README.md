![Verify JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Verify%20JAVA%20SDK/badge.svg)
![Deploy JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Deploy%20JAVA%20SDK/badge.svg) [![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/serverlessworkflow/sdk-java)

# Serverless Workflow Specification - Java SDK- Reference Implementation

Welcome to Java SDK runtime reference implementation, a lightweight implementation of the Serverless Workflow specification which provides a simple, non blocking, reactive API for workflow execution. 

Although initially conceived mainly for testing purposes, it was designed to be easily expanded, so it can eventually become production ready. 

## Status

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

Before getting started, ensure you have Java 17+ and Maven or Gradle installed.

Install [Java 17](https://openjdk.org/projects/jdk/17/)
Install [Maven](https://maven.apache.org/install.html) (if using Maven)
Install [Gradle](https://gradle.org/install) (if using Gradle)

### Dependencies

This implementation follows a modular approach, keeping dependencies minimal:
- The core library is always required.
- Additional dependencies must be explicitly included if your workflow interacts with external services (e.g., HTTP).
This ensures you only include what you need, preventing unnecessary dependencies.

#### Maven

You always need to add this dependency to your pom.xml `dependencies` section:

```xml
<dependency>
      <groupId>io.serverlessworkflow</groupId>
      <artifactId>serverlessworkflow-impl-core</artifactId>
      <version>7.0.0</version>
</dependency>
```

And only if your workflow is using HTTP calls, you must add:

```xml
<dependency>
      <groupId>io.serverlessworkflow</groupId>
      <artifactId>serverlessworkflow-impl-http</artifactId>
      <version>7.0.0</version>
</dependency>
```

#### Gradle projects:

You always need to add this dependency to your build.gradle `dependencies` section:

```text
implementation("io.serverlessworkflow:serverlessworkflow-impl-core:7.0.0")
```

And only if your workflow is using HTTP calls, you must add:

```text
implementation("io.serverlessworkflow:serverlessworkflow-impl-http:7.0.0")
```

## How to use

The quick version is intended for impatient users who want to try something as soon as possible.

The detailed version is more suitable for those users interested in a more thoughtful discussion of the API.

### Quick version

For a quick introduction, we will use a simple workflow [definition](../examples/simpleGet/src/main/resources/get.yaml) that performs a get call. 
We are going to show two ways of invoking the workflow: 
  - blocking the thread till the get request goes through
  - returning control to the caller, so the main thread continues while the get is executed

In order to execute the workflow, blocking the thread till the HTTP request is completed, you should write

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

In order to execute the workflow without blocking the calling thread till the HTTP request is completed, you should write

``` java 
  try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("get.yaml"))
          .instance(Map.of("petId", 10))
          .start()
          .thenAccept(node -> logger.info("Workflow output is {}", node));
    }
```
When the HTTP request is done, both examples will print a similar output


```shell
Workflow output is {"id":10,"category":{"id":10,"name":"string"},"name":"doggie","photoUrls":["string"],"tags":[{"id":10,"name":"string"}],"status":"string"}
```

You can find the complete java code [here](../examples/simpleGet/src/main/java/NotBlockingExample.java)

### Detailed version

To discuss runtime API we are going to use a couple of workflow:
- [listen.yaml](../examples/events/src/main/listen.yaml), which waits for an event reporting a temperature greater than 38
- [emit.yaml](../examples/events/src/main/emit.yaml), which emits events with a certain temperature, specified as workflow parameter.

Here is a summary of what we are trying to do: 

- The listen.yaml workflow waits for an event (not-blocking).
- We send an event with a low temperature (ignored).
- We send an event with a high temperature (completes the workflow).

The first step is to create a [WorkflowApplication](core/src/main/java/io/serverlessworkflow/impl/WorkflowApplication.java) instance. An application is an abstraction that allows customization of different aspects of the workflow execution (for example, change the default `ExecutorService` for thread spawning)

Since `WorkflowApplication` implements `Autocloseable`, we better use a **try-with-resources** block, ensuring any resource that the workflow might have used is freed when done. 

`try (WorkflowApplication appl = WorkflowApplication.builder().build())`

Once we have the application object, we use it to parse our definition examples. To load each workflow definition, we use the `readFromClasspath` helper method defined in [WorkflowReader](api/src/main/java/io/serverlessworkflow/api/WorkflowReader.java) class.

```java
      WorkflowDefinition listenDefinition =
          appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("listen.yaml"));
      WorkflowDefinition emitDefinition =
          appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("emit.yaml")); 
```

A [WorkflowDefinition](core/src/main/java/io/serverlessworkflow/impl/WorkflowDefinition.java) object is immutable and, therefore, thread-safe. It is used to execute as many workflow instances as desired. 

To execute a workflow, we first create a [WorkflowInstance](core/src/main/java/io/serverlessworkflow/impl/WorkflowInstance.java) object (its initial status is PENDING) and then invoke the `start` method on it (its status is changed to RUNNING). The `start` method returns a [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html), which we use to indicate that a log message should be printed when the workflow is completed.

```java
  WorkflowInstance waitingInstance = listenDefinition.instance(Map.of());
      waitingInstance
          .start()
          .thenAccept(node -> logger.info("Waiting instance completed with result {}", node));
```

As soon as the workflow execution reach the point where it waits for events to arrive, control is returned to the calling thread. Since the execution is not blocking, we can execute another workflow instance while the first one is waiting. 

We will send an event with a temperature that does not satisfy the criteria, so the listen instance will continue waiting. We use a regular Java `Map` to pass parameters to the workflow instance that sends the event. Note that since we want to wait till the event is published, we call `join` after `start`, telling the `CompletableFuture` to wait for workflow completion.

```java
 emitDefinition.instance(Map.of("temperature", 35)).start().join();
 ```
 
 It's time to complete the waiting instance and send an event with the expected temperature. We do so by reusing `emitDefinition`.

```java
 emitDefinition.instance(Map.of("temperature", 39)).start().join();
 ```
 
After that, listen instance will be completed and we will see this log message

```java
[pool-1-thread-1] INFO events.EventExample - Waiting instance completed with result [{"temperature":39}]
```
The source code of the example is [here](../examples/events/src/main/java/EventExample.java)

