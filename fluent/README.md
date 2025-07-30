# CNCF Serverless Workflow SDK Java — Fluent DSL

> A programmatic, type‑safe Java API for building and running Serverless Workflows (and agentic workflows) without writing YAML.

---

## 📦 Modules

| Module         | Purpose                                                                                       |
| -------------- | --------------------------------------------------------------------------------------------- |
| **spec**       | Core DSL implementing the [Serverless Workflow Specification](https://github.com/serverlessworkflow/specification). Purely compliant fluent API. |
| **func**       | Java‑centric “functional” DSL on top of **spec**: adds `Function<>`/`Predicate<>` support, `callFn` for Java method calls, and richer flow controls.    |
| **agentic**    | **Experimental** proof‑of‑concept DSL built on **func** for LangChain4j agentic workflows: `agent`, `sequence`, `loop`, `parallel`, etc.     |

---

## 🔧 Getting Started

Add the modules you need to your Maven `pom.xml` (replace versions as appropriate):

```xml
<!-- 
    Replace ${version.io.serverlessworkflow} with the actual released version:
    https://github.com/serverlessworkflow/sdk-java/releases 
-->
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-fluent-spec</artifactId>
  <version>${version.io.serverlessworkflow}</version>
</dependency>
<dependency>
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-fluent-func</artifactId>
  <version>${version.io.serverlessworkflow}</version>
</dependency>
<dependency>  <!-- optional, experimental -->
  <groupId>io.serverlessworkflow</groupId>
  <artifactId>serverlessworkflow-fluent-agentic</artifactId>
  <version>${version.io.serverlessworkflow}</version>
</dependency>
```

---

## 📖 Module Reference

### 1. Spec Fluent

Fully compliant with the CNCF Serverless Workflow spec.\
Use it when you want a 1:1 mapping of the YAML DSL in Java.

```java
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;

Workflow wf = WorkflowBuilder
    .workflow("flowDo")
    .tasks(tasks ->
        tasks
          .set("initCtx", "$.foo = 'bar'")
          .forEach("item", f -> f
              .each("item")
              .at("$.list")
          )
    )
    .build();
```

> [!NOTE]
> We rename reserved keywords (`for`, `do`, `if`, `while`, `switch`, `try`) to safe identifiers (`forEach`, `tasks`, `when`, etc.).

---

### 2. Func Fluent

A Java‑first DSL that builds on **spec**, adding:

- `callFn`: invoke arbitrary Java `Function<>` handlers
- `Predicate<>` **guards** via `when(Predicate)`
- Built‑in `Function`/`Predicate` support instead of JQ expressions

```java
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;

Workflow wf = FuncWorkflowBuilder
    .workflow("callJavaFlow")
    .tasks(tasks ->
        tasks.callFn("invokeHandler", call -> call
            // e.g. call.className("com.acme.Handler")
            //     .method("handle")
            //     .arg("key", "value")
            .function(ctx -> {
                // your code here
            })
        )
    )
    .build();
```

> [!WARNING]
> The **func** DSL is *not* spec‑compliant. It adds Java‑specific tasks and control‑flow extensions for in‑JVM execution.

---

### 3. Agentic Fluent *(Experimental)*

Built on **func** for LangChain4j agentic workflows. Adds:

- `agent(instance)`: invoke a LangChain4j agent
- `sequence(...)`: run agents in order
- `loop(cfg)`: retry or repeated agent calls
- `parallel(...)`: fork agent calls concurrently

```java
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.agentic.AgentWorkflowBuilder;

var scorer = AgentsUtils.newMovieExpert();
var editor = AgentsUtils.newMovieExpert();

Workflow wf = AgentWorkflowBuilder
    .workflow("retryFlow")
    .tasks(tasks -> tasks.loop(
        "reviewLoop",
        loop -> loop
          .maxIterations(5)
          .exitCondition(c -> c.readState("score", 0).doubleValue() > 0.75)
          .subAgents("reviewer", scorer, editor)
    ))
    .build();
```

---

## 🚀 Real‑World Example: Order Fulfillment

```java
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.agentic.AgentWorkflowBuilder;
import java.util.function.Predicate;

public class OrderFulfillment {

    static class InventoryAgent { /* … */ }
    static class NotificationAgent { /* … */ }
    static class ShippingAgent { /* … */ }

    public Workflow buildWorkflow() {

        Predicate<Object> inventoryOk = state ->
            Boolean.TRUE.equals(((java.util.Map<?,?>) state).get("inventoryAvailable"));

        return AgentWorkflowBuilder
            .workflow("OrderFulfillment")
            .tasks(tasks -> tasks

                // 1. initialize state
                .set("init", s -> s.expr("$.orderId = '.input.oriderId'"))

                // 2. check inventory
                .agent("checkInventory", new InventoryAgent())

                // 3. pull result into a flag
                .set("inventoryAvailable", s -> s.expr("$.checkInventory.available"))

                // 4. retry until in stock (max 3 attempts)
                .loop("retryIfOutOfStock", loop -> loop
                    .maxIterations(3)
                    .exitCondition(inventoryOk)
                    .subAgents("inventoryChecker", new InventoryAgent())
                )

                // 5. notify systems in parallel
                .parallel("notifyAll",
                    new NotificationAgent(),
                    new ShippingAgent()
                )

                // 6. mark order complete
                .set("complete", s -> s.expr("$.status = 'COMPLETED'"))
            )
            .build();
    }
}
```

---

## 🛠️ Next Steps & Roadmap

- **Error handling**: retries, back‑off, `onError` handlers
- **Timers & delays**: `wait`, per‑task `timeout`
- **Sub‑workflows** & composition: call one workflow from another
- **Event tasks**: `onEvent`, `sendEvent`
- **Human‑in‑the‑Loop**: approval/notification steps

Contributions welcome! Check out our [CONTRIBUTING.md](../CONTRIBUTING.md) and join the CNCF Slack channel for **Serverless Workflow**.

---

## 📜 License

Apache 2.0 © Serverless Workflow Authors
