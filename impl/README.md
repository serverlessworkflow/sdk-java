[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/serverlessworkflow/sdk-java)

# Serverless Workflow Specification — Java SDK (Reference Implementation)

A lightweight, non-blocking, reactive **runtime** for the [Serverless Workflow](https://serverlessworkflow.io/) specification. Use it to load, validate, and execute workflows written in YAML/JSON—or build them programmatically with our Fluent DSL.

---

## Contents

* [Features](#features)
* [Modules](#modules)
* [Installation](#installation)
* [Quick Start](#quick-start)
* [Detailed Walkthrough](#detailed-walkthrough)
* [Fluent Java DSL](#fluent-java-dsl)
* [Mermaid Diagrams](#mermaid-diagrams)
* [Lifecycle Events](#lifecycle-events)
* [Errors & Auth](#errors--auth)
* [Development](#development)
* [License](#license)

---

## Features

This reference implementation can run workflows consisting of:

**Tasks**

* `Switch`
* `Set`
* `Do`
* `Raise`
* `Listen`
* `Emit`
* `Fork`
* `For` / `ForEach`
* `Try` (with `Catch`/`Retry`)
* `Wait`
* `Call`

  * **HTTP**

    * Basic auth
    * Bearer auth
    * OAuth2 / OIDC auth
    * Digest auth (via Fluent DSL)

**Schema Validation**

* Input / Output validation

**Expressions**

* Input / Output / Export
* Special keywords: `runtime`, `workflow`, `task`, …

**Error Definitions**

* Standard & custom error types

**Lifecycle Events**

* `Pending`, `Started`, `Suspended`, `Faulted`, `Resumed`, `Cancelled`, `Completed`

---

## Modules

This SDK is modular by design—pull in only what you need:

* **`serverlessworkflow-impl-core`**
  Workflow engine & core interfaces. Depends on generated types and CloudEvents SDK.

* **`serverlessworkflow-impl-jackson`**
  Adds Jackson integration, JQ expressions, JSON Schema validation, and CloudEvents (de)serialization.
  👉 **Most users add this one.**

* **`serverlessworkflow-impl-http`**
  HTTP `Call` task handler.

* **`serverlessworkflow-impl-jackson-jwt`**
  OAuth2/OIDC helpers for HTTP calls.

There are also companion modules/docs for:

* **Fluent DSL** (programmatic builder)
* **Mermaid** (diagramming workflows)

Links below.

---

## Installation

### Maven

```xml
<!-- Core + Jackson (YAML, JQ, JSON Schema, CloudEvents) -->
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-impl-jackson</artifactId>
</dependency>

<!-- Add if you use HTTP Call tasks -->
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-impl-http</artifactId>
</dependency>

<!-- Add if your HTTP calls require OAuth2/OIDC -->
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-impl-jackson-jwt</artifactId>
</dependency>
```

### Gradle (Kotlin/Groovy)

```gradle
implementation("io.serverlessworkflow:serverlessworkflow-impl-jackson")
implementation("io.serverlessworkflow:serverlessworkflow-impl-http")        // if using HTTP
implementation("io.serverlessworkflow:serverlessworkflow-impl-jackson-jwt") // if using OAuth2/OIDC
```

> Requires **Java 17+**.

---

## Quick Start

We’ll run a simple workflow that performs an HTTP GET. See the full YAML in
[examples/simpleGet/src/main/resources/get.yaml]().

### Blocking execution

```java
try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
  var output =
      appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("get.yaml"))
          .instance(Map.of("petId", 10))
          .start()
          .join();
  logger.info("Workflow output is {}", output);
}
```

### Non-blocking execution

```java
try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
  appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("get.yaml"))
      .instance(Map.of("petId", 10))
      .start()
      .thenAccept(output -> logger.info("Workflow output is {}", output));
}
```

Example output:

```text
Workflow output is {"id":10,"category":{"id":10,"name":"string"},"name":"doggie",...}
```

Full examples:

* Blocking: [examples/simpleGet/src/main/java/io/serverlessworkflow/impl/BlockingExample.java]()
* Non-blocking: [examples/simpleGet/src/main/java/io/serverlessworkflow/impl/NotBlockingExample.java]()

---

## Event publish/subscribe example

We’ll coordinate two workflows—one **listens** for high temperatures and the other **emits** temperature events:

* `listen.yaml` waits for an event with `temperature > 38` (non-blocking)
* `emit.yaml` emits events using a workflow parameter

Create an application (customize thread pools, etc.). `WorkflowApplication` is `AutoCloseable`, so use try-with-resources:

```java
try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
  WorkflowDefinition listen = appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("listen.yaml"));
  WorkflowDefinition emit   = appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("emit.yaml"));

  // Start the listener (non-blocking)
  listen.instance(Map.of())
        .start()
        .thenAccept(out -> logger.info("Waiting instance completed with {}", out));

  // Emit a low temperature (ignored)
  emit.instance(Map.of("temperature", 35)).start().join();

  // Emit a high temperature (completes the waiting workflow)
  emit.instance(Map.of("temperature", 39)).start().join();
}
```

You’ll see:

```
Waiting instance completed with [{"temperature":39}]
```

Source: `examples/events/src/main/java/events/EventExample.java`

---

## Workflow execution control

As shown in previous examples, to start a new workflow instance, first a [WorkflowInstance](https://github.com/serverlessworkflow/sdk-java/blob/main/impl/core/src/main/java/io/serverlessworkflow/impl/WorkflowInstance.java) is created from a [WorkflowDefinition](https://github.com/serverlessworkflow/sdk-java/blob/main/impl/core/src/main/java/io/serverlessworkflow/impl/WorkflowDefinition.java#L74), specifying the desired input, and then start method is invoked over it. Start method returns a CompletableFuture, which might be used to obtain the output, either synchronously or asynchronously. 

Once started, and before it completes, a workflow instance execution can be suspended or cancelled. Once cancelled, a workflow instance is done, while a suspended one might be resumed. 

## Fluent Java DSL

Prefer building workflows programmatically with type-safe builders and recipes?
👉 **Docs:** [https://github.com/serverlessworkflow/sdk-java/blob/main/fluent/README.md](https://github.com/serverlessworkflow/sdk-java/blob/main/fluent/README.md)

Highlights:

* Chainable builders for `Call HTTP`, `Listen/Emit`, `Try/Catch/Retry`, `Switch`, `Fork`, etc.
* Reusable “recipes” (e.g., `http().GET().acceptJSON().endpoint("${ ... }")`)
* Static helpers for auth (`basic(...)`, `bearer(...)`, `oidc(...)`, …) and standard error types.

---

## Mermaid Diagrams

Generate Mermaid diagrams for your workflows right from the SDK.
👉 **Docs:** [https://github.com/serverlessworkflow/sdk-java/blob/main/mermaid/README.md](https://github.com/serverlessworkflow/sdk-java/blob/main/mermaid/README.md)

Great for docs, PRs, and visual reviews.

---

## Lifecycle Events

Every workflow publishes CloudEvents you can subscribe to (in-memory or your own broker):

* `io.serverlessworkflow.workflow.*` → `pending`, `started`, `suspended`, `faulted`, `resumed`, `cancelled`, `completed`
* `io.serverlessworkflow.task.*` → `started`, `suspended`, `resumed`, `cancelled`, `completed`

See `impl` tests/examples for consuming and asserting on these events.

---

## Errors & Auth

**Errors**

* Raise standard errors with versioned URIs (e.g., `runtime`, `communication`, `timeout`, …) or custom ones.
* Fluent helpers (e.g., `serverError()`, `timeoutError()`) set both **type URI** and default **HTTP status**; override as needed.

**Authentication (HTTP Call)**

* Basic / Bearer / Digest
* OAuth2 / OpenID Connect (client credentials, etc.)
* Fluent helpers: `basic("user","pass")`, `bearer("token")`, `oidc(authority, grant, clientId, clientSecret)`, …

---

## Development

**Prereqs**

* Java 17+
* Maven (or Gradle)

**Build & Verify**

```bash
mvn -B clean verify
```

**Formatting & Lint (CI parity)**

```bash
mvn -B -DskipTests spotless:check checkstyle:check
```

**Run examples**

* See `examples/` directory for runnable samples.

---

## License

Apache License 2.0. See `LICENSE` for details.

---

*Questions or ideas? PRs and issues welcome!*
