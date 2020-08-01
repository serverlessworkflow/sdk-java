![Verify JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Verify%20JAVA%20SDK/badge.svg)
![Deploy JAVA SDK](https://github.com/serverlessworkflow/sdk-java/workflows/Deploy%20JAVA%20SDK/badge.svg)

# Serverless Workflow Specification - Java SDK

Provides the Java API/SPI and Model Validation for the [Serverless Workflow Specification](https://github.com/serverlessworkflow/specification)

With the SDK you can:
* Parse workflow JSON and YAML definitions
* Programatically build workflow definitions
* Validate workflow definitions (both schema and workflow integrity validation)

Serverless Workflow Java SDK is **not** a workflow runtime implementation but can be used by Java runtime implementations
to parse and validate workflow definitions.

### Status

This SDK is considered work in progress. We intend to release versions which match the future releases 
of the Serverless Workflow specification. Currently the SDK features match those of the current 
"master" specification branch.

### Getting Started

#### Building locally

To build project and run tests locally:

```
git clone https://github.com/serverlessworkflow/sdk-java.git
mvn clean install
```

Then to use it in your project pom.xml add:

* API dependency

```xml
<dependency>
    <groupId>io.serverlessworkflow</groupId>
    <artifactId>serverlessworkflow-api</artifactId>
    <version>0.2-SNAPSHOT</version>
</dependency>
```

* SPI dependency

```xml
<dependency>
    <groupId>io.serverlessworkflow</groupId>
    <artifactId>serverlessworkflow-spi</artifactId>
    <version>0.2-SNAPSHOT</version>
</dependency>
```

* Validation dependency

```xml
<dependency>
    <groupId>io.serverlessworkflow</groupId>
    <artifactId>serverlessworkflow-validation</artifactId>
    <version>0.2-SNAPSHOT</version>
</dependency>
```

#### Get dependencies from Nexus

Our SNAPSHOT versions are published to the Sonatype repositories.
Make sure you enable snapshots in your Maven settings.xml 
or you can specify in your pom.xml repositories section:

```xml
<repository>
    <id>oss.sonatype.org-snapshot</id>
    <url>http://oss.sonatype.org/content/repositories/snapshots</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

And use the dependencies:

```xml
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-api</artifactId>
  <version>0.2-SNAPSHOT</version>
</dependency>
```

```xml
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-spi</artifactId>
  <version>0.2-SNAPSHOT</version>
</dependency>
```

```xml
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-validation</artifactId>
  <version>0.2-SNAPSHOT</version>
</dependency>
```

### How to Use 

#### Creating from JSON/YAML source

You can create a Workflow instance from JSON/YAML source:

Let's say you have a simple YAML based workflow definition:

```yaml
id: greeting
version: '1.0'
name: Greeting Workflow
description: Greet Someone
functions:
- name: greetingFunction
  resource: functionResourse
states:
- name: Greet
  type: operation
  start:
    kind: default
  actionMode: sequential
  actions:
  - functionRef:
      refName: greetingFunction
      parameters:
        name: "$.greet.name"
    actionDataFilter:
      dataResultsPath: "$.payload.greeting"
  stateDataFilter:
    dataOutputPath: "$.greeting"
  end:
    kind: default
```

To parse it and create a Workflow intance you can do:

``` java
Workflow workflow = Workflow.fromSource(source);
```

where 'source' is the above mentioned YAML definition.

The fromSource static method can take in definitions in both JSON and YAML formats.

Once you have the Workflow instance you can use its API to inspect it, for example:

``` java
assertNotNull(workflow);
assertEquals("greeting", workflow.getId());
assertEquals("Greeting Workflow", workflow.getName());

assertNotNull(workflow.getFunctions());
assertEquals(1, workflow.getFunctions().size());
assertEquals("greetingFunction", workflow.getFunctions().get(0).getName());

assertNotNull(workflow.getStates());
assertEquals(1, workflow.getStates().size());
assertTrue(workflow.getStates().get(0) instanceof OperationState);

OperationState operationState = (OperationState) workflow.getStates().get(0);
assertEquals("Greet", operationState.getName());
assertEquals(DefaultState.Type.OPERATION, operationState.getType());

...
```

#### Using builder API

You can also programmatically create Workflow instances, for example:

``` java
Workflow testWorkflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withEvents(Arrays.asList(
                        new EventDefinition().withName("testEvent").withSource("testSource").withType("testType"))
                )
                .withFunctions(Arrays.asList(
                        new FunctionDefinition().withName("testFunction").withResource("testResource").withType("testType"))
                )
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );
```

This will create a test workflow that defines an event, a function and a single Delay State.

You can use the workflow instance to get its JSON/YAML definition as well:

``` java
assertNotNull(Workflow.toJson(testWorkflow));
assertNotNull(Workflow.toYaml(testWorkflow));
```

#### Using Workflow Validation

Validation allows you to performe Json Schema validation against the JSON/YAML workflow definitions.
Once you have a `Workflow` instance, you can also run integrity checks.

You can validate a Workflow JSON/YAML definition to get validation errors:

``` java
WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
List<ValidationError> validationErrors = workflowValidator.setSource("WORKFLOW_MODEL_JSON/YAML").validate();
```

Where `WORKFLOW_MODEL_JSON/YAML` is the actual workflow model JSON or YAML definition.

Or you can just check if it is valid (without getting specific errors):

``` java
WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
boolean isValidWorkflow = workflowValidator.setSource("WORKFLOW_MODEL_JSON/YAML").isValid();
```

If you build your Workflow programmatically, you can validate it as well:

``` java
Workflow workflow = new Workflow().withId("test-workflow").withVersion("1.0")
.withStates(Arrays.asList(
        new DelayState().withName("delayState").withType(DELAY)
                .withStart(
                        new Start().withKind(Start.Kind.DEFAULT)
                )
                .withEnd(
                        new End().withKind(End.Kind.DEFAULT)
                )
                .withTimeDelay("PT1M")
        )
);

WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
List<ValidationError> validationErrors = workflowValidator.setWorkflow(workflow).validate();
```