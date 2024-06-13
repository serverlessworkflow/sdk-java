![Verify JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Verify%20JAVA%20SDK/badge.svg)
![Deploy JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Deploy%20JAVA%20SDK/badge.svg) [![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/serverlessworkflow/sdk-java)

# Serverless Workflow Specification - Java SDK

Provides the Java API for the [Serverless Workflow Specification](https://github.com/serverlessworkflow/specification)

With the SDK you can:

* Read workflow JSON and YAML definitions
* Write workflow in JSON and YAML format. 

Serverless Workflow Java SDK is **not** a workflow runtime implementation but can be used by Java runtime implementations to parse workflow definitions.

### Status

| Latest Releases | Conformance to spec version |
| :---: | :---: |
| [7.0.0.Final](https://github.com/serverlessworkflow/sdk-java/releases/tag/7.0.0.Final) | [v0.10](https://github.com/serverlessworkflow/specification/tree/0.10.x) |
| [5.0.0.Final](https://github.com/serverlessworkflow/sdk-java/releases/tag/5.0.0.Final) | [v0.8](https://github.com/serverlessworkflow/specification/tree/0.8.x) |
| [4.0.5.Final](https://github.com/serverlessworkflow/sdk-java/releases/tag/4.0.5.Final) | [v0.8](https://github.com/serverlessworkflow/specification/tree/0.8.x) |
| [3.0.0.Final](https://github.com/serverlessworkflow/sdk-java/releases/tag/3.0.0.Final) | [v0.7](https://github.com/serverlessworkflow/specification/tree/0.7.x) |
| [2.0.0.Final](https://github.com/serverlessworkflow/sdk-java/releases/tag/2.0.0.Final) | [v0.6](https://github.com/serverlessworkflow/specification/tree/0.6.x) |
| [1.0.3.Final](https://github.com/serverlessworkflow/sdk-java/releases/tag/1.0.3.Final) | [v0.5](https://github.com/serverlessworkflow/specification/tree/0.5.x) |

Note that 6.0.0.Final, which will be the one for specification version 0.9, is skipped intentionally in case someone want to work on it. 

### JDK Version

| SDK Version | JDK Version |
| :---: | :---: |
| 5.0.0 and after | 11 |
| 4.0.x and before | 8 | 

### Getting Started


#### Building SNAPSHOT locally

To build project and run tests locally:

```
git clone https://github.com/serverlessworkflow/sdk-java.git
mvn clean install
```

The project uses [Google's code styleguide](https://google.github.io/styleguide/javaguide.html).
Your changes should be automatically formatted during the build.

#### Maven projects:

Add the following dependencies to your pom.xml `dependencies` section:

```xml
<dependency>
    <groupId>io.serverlessworkflow</groupId>
    <artifactId>serverlessworkflow-api</artifactId>
    <version>7.0.0-SNAPSHOT</version>
</dependency>
```

#### Gradle projects:

 Add the following dependencies to your build.gradle `dependencies` section:

```text
implementation("io.serverlessworkflow:serverlessworkflow-api:7.0.0-SNAPSHOT")
```

### How to Use 

#### Creating from JSON/YAML source

You can create a Workflow instance from JSON/YAML source:

Let's say you have a simple YAML based workflow definition in a file name `simple.yaml` located in your working dir:

```yaml
document:
  dsl: 1.0.0-alpha1
  namespace: default
  name: implicit-sequence
do:
  setRed:
    set:
      colors: '${ .colors + [ "red" ] }'
  setGreen:
    set:
      colors: '${ .colors + [ "green" ] }'
  setBlue:
    set:
      colors: '${ .colors + [ "blue" ] }'

```

To parse it and create a Workflow instance you can do:

``` java

try (InputStream in = new FileInputStream("simple.yaml")) {
   Workflow workflow = WorkflowReader.readWorkflow (in, WorkflowFormat.YAML);
   // Once you have the Workflow instance you can use its API to inspect it
}
```

#### Writing a workflow

Given a workflow definition, you can store it using JSON or YAML format. 
For example, to store a workflow using json format in a file called `simple.json`, you write

``` java
try (OutputStream out = new FileOutputStream("simple.json")) {
    WorkflowWriter.writeWorkflow(out, workflow, WorkflowFormat.JSON);
}

```