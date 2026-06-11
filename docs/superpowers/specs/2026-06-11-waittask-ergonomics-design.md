# WaitTask Ergonomics and Executor Fix

**Date**: 2026-06-11  
**Status**: Approved

## Overview

Improve the WaitTask API by adding ergonomic convenience methods to DSL/FuncDSL and fix the WaitExecutor to properly handle all three duration formats (inline, literal, expression).

## Problems

1. DSL has ergonomic `timeout*` methods but no equivalent for wait tasks
2. FuncDSL has zero wait task support
3. WaitExecutor only handles `durationInline` and `durationExpression`, crashes on `durationLiteral`
4. WaitExecutor parses expressions at build time instead of evaluating at runtime

## Solution

### 1. DSL Convenience Methods

Add to `fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java`:

**Unnamed variants:**
```java
public static TasksConfigurer waitDays(int days)
public static TasksConfigurer waitHours(int hours)
public static TasksConfigurer waitMinutes(int minutes)
public static TasksConfigurer waitSeconds(int seconds)
public static TasksConfigurer waitMillis(int milliseconds)
public static TasksConfigurer wait(Duration duration)
```

**Named variants:**
```java
public static TasksConfigurer waitDays(String name, int days)
public static TasksConfigurer waitHours(String name, int hours)
public static TasksConfigurer waitMinutes(String name, int minutes)
public static TasksConfigurer waitSeconds(String name, int seconds)
public static TasksConfigurer waitMillis(String name, int milliseconds)
public static TasksConfigurer wait(String name, Duration duration)
```

**Implementation pattern:**
```java
public static TasksConfigurer waitSeconds(int seconds) {
  return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.seconds(seconds))));
}

public static TasksConfigurer waitSeconds(String name, int seconds) {
  return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.seconds(seconds))));
}
```

The `wait(Duration)` variant converts to `DurationInline` using the existing logic from `WaitTaskBuilder.wait(Duration)`.

### 2. FuncDSL Wait Support

Add to `experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java`:

All methods from section 1, but returning `FuncTaskConfigurer` instead of `TasksConfigurer`, plus the basic wait methods:

```java
public static FuncTaskConfigurer wait(Consumer<TimeoutBuilder> duration)
public static FuncTaskConfigurer wait(String name, Consumer<TimeoutBuilder> duration)
public static FuncTaskConfigurer wait(String durationExpression)
public static FuncTaskConfigurer wait(String name, String durationExpression)
```

These delegate to `list.wait()` on the `FuncTaskItemListBuilder`, following the same pattern as other FuncDSL task methods.

### 3. WaitExecutor Duration Handling

Modify `impl/core/src/main/java/io/serverlessworkflow/impl/executors/WaitExecutor.java`:

**Hybrid approach**: Validate static durations at build time, defer expressions to runtime.

**Changes to WaitExecutorBuilder:**

Add field:
```java
private final String runtimeExpression;
```

Update constructor logic:
```java
protected WaitExecutorBuilder(
    WorkflowMutablePosition position, WaitTask task, WorkflowDefinition definition) {
  super(position, task, definition);
  
  if (task.getWait().getDurationInline() != null) {
    this.millisToWait = toLong(task.getWait().getDurationInline());
    this.runtimeExpression = null;
  } else if (task.getWait().getDurationLiteral() != null) {
    this.millisToWait = Duration.parse(task.getWait().getDurationLiteral());
    this.runtimeExpression = null;
  } else if (task.getWait().getDurationExpression() != null) {
    this.millisToWait = null;
    this.runtimeExpression = task.getWait().getDurationExpression();
  } else {
    throw new IllegalStateException("Wait task has no duration specified");
  }
}
```

**Changes to WaitExecutor:**

Add field:
```java
private final String runtimeExpression;
```

Update constructor:
```java
protected WaitExecutor(WaitExecutorBuilder builder) {
  super(builder);
  this.millisToWait = builder.millisToWait;
  this.runtimeExpression = builder.runtimeExpression;
}
```

Update `internalExecute()`:
```java
@Override
protected CompletableFuture<WorkflowModel> internalExecute(
    WorkflowContext workflow, TaskContext taskContext) {
  ((WorkflowMutableInstance) workflow.instance()).status(WorkflowStatus.WAITING);
  
  Duration waitDuration;
  if (runtimeExpression != null) {
    // Evaluate expression at runtime using workflow/task context
    String evaluatedExpression = evaluateExpression(runtimeExpression, workflow, taskContext);
    waitDuration = Duration.parse(evaluatedExpression);
  } else {
    waitDuration = millisToWait;
  }
  
  return new CompletableFuture<WorkflowModel>()
      .completeOnTimeout(taskContext.output(), waitDuration.toMillis(), TimeUnit.MILLISECONDS)
      .thenApply(this::complete);
}
```

Note: The `evaluateExpression()` method will need to be implemented or use existing expression evaluation utilities from the workflow context.

## Benefits

- Consistent API between timeout and wait methods
- Better developer experience with concise method calls
- Full support for all three duration formats
- Early validation for static durations
- Proper runtime evaluation for dynamic expressions
- FuncDSL users can now use wait tasks

## Testing Considerations

- Test all convenience methods (days, hours, minutes, seconds, millis)
- Test `wait(Duration)` conversion
- Test WaitExecutor with inline, literal, and expression durations
- Test runtime expression evaluation with various workflow contexts
- Verify build-time validation catches invalid static durations
- Verify runtime errors for invalid expression results
