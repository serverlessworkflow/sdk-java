# WaitTask Ergonomics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ergonomic convenience methods for wait tasks to DSL and FuncDSL, matching the existing timeout* pattern.

**Architecture:** Add static helper methods that delegate to existing wait builder infrastructure. Methods follow the established pattern of returning TasksConfigurer/FuncTaskConfigurer and using inline duration builders.

**Tech Stack:** Java, JUnit 5, AssertJ

---

## File Structure

**Files to modify:**
- `fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java` - Add wait convenience methods
- `experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java` - Add wait convenience methods and basic wait support

**Test files to create:**
- `fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java` - Comprehensive tests for DSL wait methods
- `experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java` - Comprehensive tests for FuncDSL wait methods

---

### Task 1: Add DSL waitSeconds and waitMinutes methods with tests

**Files:**
- Modify: `fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java`
- Create: `fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java`

- [ ] **Step 1: Write failing test for waitSeconds**

Create `fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java`:

```java
/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverlessworkflow.fluent.spec.dsl;

import static io.serverlessworkflow.fluent.spec.dsl.DSL.waitSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import org.junit.jupiter.api.Test;

public class DSLWaitTest {

  @Test
  public void when_wait_seconds_unnamed() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(waitSeconds(30))
            .build();

    assertThat(wf.getDo()).hasSize(1);
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();

    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline).isNotNull();
    assertThat(inline.getSeconds()).isEqualTo(30);
    assertThat(inline.getMinutes()).isZero();
    assertThat(inline.getHours()).isZero();
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=DSLWaitTest#when_wait_seconds_unnamed -pl fluent/spec`

Expected: FAIL with "cannot resolve method 'waitSeconds'"

- [ ] **Step 3: Implement waitSeconds methods in DSL.java**

Add to `fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java` after the existing wait methods (around line 871):

```java
  /**
   * Create a {@link TasksConfigurer} that adds a {@code wait} task configured with seconds.
   *
   * <p>Example: {@code tasks(waitSeconds(30))}
   *
   * @param seconds wait duration in seconds
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitSeconds(int seconds) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.seconds(seconds))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a named {@code wait} task configured with seconds.
   *
   * <p>Example: {@code tasks(waitSeconds("pause", 30))}
   *
   * @param name task name
   * @param seconds wait duration in seconds
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitSeconds(String name, int seconds) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.seconds(seconds))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code wait} task configured with minutes.
   *
   * <p>Example: {@code tasks(waitMinutes(5))}
   *
   * @param minutes wait duration in minutes
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitMinutes(int minutes) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.minutes(minutes))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a named {@code wait} task configured with minutes.
   *
   * <p>Example: {@code tasks(waitMinutes("pause", 5))}
   *
   * @param name task name
   * @param minutes wait duration in minutes
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitMinutes(String name, int minutes) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.minutes(minutes))));
  }
```

- [ ] **Step 4: Add test for named waitSeconds**

Add to `DSLWaitTest.java`:

```java
  @Test
  public void when_wait_seconds_named() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(waitSeconds("pause", 45))
            .build();

    assertThat(wf.getDo()).hasSize(1);
    assertThat(wf.getDo().get(0).getName()).isEqualTo("pause");

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();

    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline).isNotNull();
    assertThat(inline.getSeconds()).isEqualTo(45);
  }
```

- [ ] **Step 5: Add test for waitMinutes**

Add to `DSLWaitTest.java`:

```java
  @Test
  public void when_wait_minutes_unnamed() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(waitMinutes(10))
            .build();

    assertThat(wf.getDo()).hasSize(1);
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();

    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline).isNotNull();
    assertThat(inline.getMinutes()).isEqualTo(10);
    assertThat(inline.getSeconds()).isZero();
  }

  @Test
  public void when_wait_minutes_named() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(waitMinutes("delay", 15))
            .build();

    assertThat(wf.getDo()).hasSize(1);
    assertThat(wf.getDo().get(0).getName()).isEqualTo("delay");

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline.getMinutes()).isEqualTo(15);
  }
```

- [ ] **Step 6: Run all tests to verify they pass**

Run: `mvn test -Dtest=DSLWaitTest -pl fluent/spec`

Expected: All 4 tests PASS

- [ ] **Step 7: Commit**

```bash
git add fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java \
        fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java
git commit -m "feat: add waitSeconds and waitMinutes convenience methods to DSL"
```

---

### Task 2: Add DSL waitHours, waitDays, and waitMillis methods

**Files:**
- Modify: `fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java`
- Modify: `fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java`

- [ ] **Step 1: Write failing tests**

Add to `DSLWaitTest.java`:

```java
  @Test
  public void when_wait_hours_unnamed() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.waitHours(2))
            .build();

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline.getHours()).isEqualTo(2);
    assertThat(inline.getMinutes()).isZero();
  }

  @Test
  public void when_wait_hours_named() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.waitHours("longPause", 3))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("longPause");
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask.getWait().getDurationInline().getHours()).isEqualTo(3);
  }

  @Test
  public void when_wait_days_unnamed() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.waitDays(1))
            .build();

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline.getDays()).isEqualTo(1);
    assertThat(inline.getHours()).isZero();
  }

  @Test
  public void when_wait_days_named() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.waitDays("dailyDelay", 5))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("dailyDelay");
    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getDays())
        .isEqualTo(5);
  }

  @Test
  public void when_wait_millis_unnamed() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.waitMillis(500))
            .build();

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline.getMilliseconds()).isEqualTo(500);
    assertThat(inline.getSeconds()).isZero();
  }

  @Test
  public void when_wait_millis_named() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.waitMillis("shortPause", 250))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("shortPause");
    assertThat(
            wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getMilliseconds())
        .isEqualTo(250);
  }
```

Add import at top of file:
```java
import static io.serverlessworkflow.fluent.spec.dsl.DSL.waitDays;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.waitHours;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.waitMillis;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.waitMinutes;
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -Dtest=DSLWaitTest -pl fluent/spec`

Expected: FAIL with "cannot resolve method" errors

- [ ] **Step 3: Implement waitHours, waitDays, and waitMillis**

Add to `DSL.java` after the waitMinutes methods:

```java
  /**
   * Create a {@link TasksConfigurer} that adds a {@code wait} task configured with hours.
   *
   * <p>Example: {@code tasks(waitHours(2))}
   *
   * @param hours wait duration in hours
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitHours(int hours) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.hours(hours))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a named {@code wait} task configured with hours.
   *
   * <p>Example: {@code tasks(waitHours("longPause", 2))}
   *
   * @param name task name
   * @param hours wait duration in hours
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitHours(String name, int hours) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.hours(hours))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code wait} task configured with days.
   *
   * <p>Example: {@code tasks(waitDays(1))}
   *
   * @param days wait duration in days
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitDays(int days) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.days(days))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a named {@code wait} task configured with days.
   *
   * <p>Example: {@code tasks(waitDays("dailyDelay", 1))}
   *
   * @param name task name
   * @param days wait duration in days
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitDays(String name, int days) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.days(days))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code wait} task configured with milliseconds.
   *
   * <p>Example: {@code tasks(waitMillis(500))}
   *
   * @param milliseconds wait duration in milliseconds
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitMillis(int milliseconds) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.milliseconds(milliseconds))));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a named {@code wait} task configured with
   * milliseconds.
   *
   * <p>Example: {@code tasks(waitMillis("shortPause", 500))}
   *
   * @param name task name
   * @param milliseconds wait duration in milliseconds
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer waitMillis(String name, int milliseconds) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.milliseconds(milliseconds))));
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=DSLWaitTest -pl fluent/spec`

Expected: All 10 tests PASS

- [ ] **Step 5: Commit**

```bash
git add fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java \
        fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java
git commit -m "feat: add waitHours, waitDays, and waitMillis convenience methods to DSL"
```

---

### Task 3: Add DSL wait(Duration) method

**Files:**
- Modify: `fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java`
- Modify: `fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java`

- [ ] **Step 1: Write failing test**

Add to `DSLWaitTest.java`:

```java
  @Test
  public void when_wait_with_duration_unnamed() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.wait(java.time.Duration.ofMinutes(5).plusSeconds(30)))
            .build();

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline).isNotNull();
    assertThat(inline.getMinutes()).isEqualTo(5);
    assertThat(inline.getSeconds()).isEqualTo(30);
  }

  @Test
  public void when_wait_with_duration_named() {
    Workflow wf =
        WorkflowBuilder.workflow("waitFlow", "myNs", "1.0.0")
            .tasks(DSL.wait("customDelay", java.time.Duration.ofHours(1).plusMinutes(15)))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("customDelay");
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    DurationInline inline = waitTask.getWait().getDurationInline();
    assertThat(inline.getHours()).isEqualTo(1);
    assertThat(inline.getMinutes()).isEqualTo(15);
  }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -Dtest=DSLWaitTest#when_wait_with_duration_unnamed -pl fluent/spec`

Expected: FAIL (ambiguous method call or compile error)

- [ ] **Step 3: Implement wait(Duration) methods**

Add to `DSL.java` after the waitMillis methods:

```java
  /**
   * Create a {@link TasksConfigurer} that adds a {@code wait} task configured with a Java {@link
   * java.time.Duration}.
   *
   * <p>The Duration is converted to DurationInline format with days, hours, minutes, seconds, and
   * milliseconds.
   *
   * <p>Example: {@code tasks(wait(Duration.ofMinutes(5).plusSeconds(30)))}
   *
   * @param duration wait duration as a Java Duration
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer wait(java.time.Duration duration) {
    return list -> list.wait(w -> w.wait(duration));
  }

  /**
   * Create a {@link TasksConfigurer} that adds a named {@code wait} task configured with a Java
   * {@link java.time.Duration}.
   *
   * <p>The Duration is converted to DurationInline format with days, hours, minutes, seconds, and
   * milliseconds.
   *
   * <p>Example: {@code tasks(wait("pause", Duration.ofMinutes(5).plusSeconds(30)))}
   *
   * @param name task name
   * @param duration wait duration as a Java Duration
   * @return a {@link TasksConfigurer} that adds a WaitTask
   */
  public static TasksConfigurer wait(String name, java.time.Duration duration) {
    return list -> list.wait(name, w -> w.wait(duration));
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=DSLWaitTest -pl fluent/spec`

Expected: All 12 tests PASS

- [ ] **Step 5: Commit**

```bash
git add fluent/spec/src/main/java/io/serverlessworkflow/fluent/spec/dsl/DSL.java \
        fluent/spec/src/test/java/io/serverlessworkflow/fluent/spec/dsl/DSLWaitTest.java
git commit -m "feat: add wait(Duration) convenience method to DSL"
```

---

### Task 4: Add FuncDSL basic wait support

**Files:**
- Modify: `experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java`
- Create: `experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java`

- [ ] **Step 1: Write failing test for basic wait**

Create `experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java`:

```java
/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverlessworkflow.fluent.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.tasks;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.wait;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.timeoutSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Workflow;
import org.junit.jupiter.api.Test;

public class FuncDSLWaitTest {

  @Test
  public void when_wait_with_string_expression() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(wait("PT5S")))
            .build();

    assertThat(wf.getDo()).hasSize(1);
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();
    assertThat(waitTask.getWait().get()).isEqualTo("PT5S");
  }

  @Test
  public void when_wait_with_timeout_builder() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(wait(timeoutSeconds(10))))
            .build();

    assertThat(wf.getDo()).hasSize(1);
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();
    assertThat(waitTask.getWait().getDurationInline().getSeconds()).isEqualTo(10);
  }

  @Test
  public void when_wait_named_with_string() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(wait("pause", "PT15S")))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("pause");
    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().get()).isEqualTo("PT15S");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=FuncDSLWaitTest -pl experimental/fluent/func`

Expected: FAIL with "cannot resolve method 'wait'"

- [ ] **Step 3: Implement basic wait methods in FuncDSL**

Add to `experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java` in the appropriate section (after similar task methods, around line 1088):

```java
  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with a duration
   * expression.
   *
   * <p>Example: {@code tasks(wait("PT5M"))}
   *
   * @param durationExpression duration expression or ISO 8601 literal
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer wait(String durationExpression) {
    return list -> list.wait(w -> w.wait(durationExpression));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with a
   * duration expression.
   *
   * <p>Example: {@code tasks(wait("pause", "PT5M"))}
   *
   * @param name task name
   * @param durationExpression duration expression or ISO 8601 literal
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer wait(String name, String durationExpression) {
    return list -> list.wait(name, w -> w.wait(durationExpression));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with an inline
   * duration builder.
   *
   * <p>Example: {@code tasks(wait(timeoutSeconds(30)))}
   *
   * @param duration timeout builder consumer
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer wait(Consumer<TimeoutBuilder> duration) {
    return list -> list.wait(w -> w.wait(duration));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with an
   * inline duration builder.
   *
   * <p>Example: {@code tasks(wait("pause", timeoutSeconds(30)))}
   *
   * @param name task name
   * @param duration timeout builder consumer
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer wait(String name, Consumer<TimeoutBuilder> duration) {
    return list -> list.wait(name, w -> w.wait(duration));
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=FuncDSLWaitTest -pl experimental/fluent/func`

Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java \
        experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java
git commit -m "feat: add basic wait methods to FuncDSL"
```

---

### Task 5: Add FuncDSL waitSeconds and waitMinutes convenience methods

**Files:**
- Modify: `experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java`
- Modify: `experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java`

- [ ] **Step 1: Write failing tests**

Add to `FuncDSLWaitTest.java`:

```java
  @Test
  public void when_wait_seconds_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitSeconds(30)))
            .build();

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    var inline = waitTask.getWait().getDurationInline();
    assertThat(inline).isNotNull();
    assertThat(inline.getSeconds()).isEqualTo(30);
    assertThat(inline.getMinutes()).isZero();
  }

  @Test
  public void when_wait_seconds_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitSeconds("pause", 45)))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("pause");
    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getSeconds())
        .isEqualTo(45);
  }

  @Test
  public void when_wait_minutes_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitMinutes(10)))
            .build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertThat(inline.getMinutes()).isEqualTo(10);
    assertThat(inline.getSeconds()).isZero();
  }

  @Test
  public void when_wait_minutes_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitMinutes("delay", 15)))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("delay");
    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getMinutes())
        .isEqualTo(15);
  }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -Dtest=FuncDSLWaitTest -pl experimental/fluent/func`

Expected: FAIL with "cannot resolve method" errors

- [ ] **Step 3: Implement waitSeconds and waitMinutes**

Add to `FuncDSL.java` after the basic wait methods:

```java
  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with seconds.
   *
   * <p>Example: {@code tasks(waitSeconds(30))}
   *
   * @param seconds wait duration in seconds
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitSeconds(int seconds) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.seconds(seconds))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with
   * seconds.
   *
   * <p>Example: {@code tasks(waitSeconds("pause", 30))}
   *
   * @param name task name
   * @param seconds wait duration in seconds
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitSeconds(String name, int seconds) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.seconds(seconds))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with minutes.
   *
   * <p>Example: {@code tasks(waitMinutes(5))}
   *
   * @param minutes wait duration in minutes
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitMinutes(int minutes) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.minutes(minutes))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with
   * minutes.
   *
   * <p>Example: {@code tasks(waitMinutes("pause", 5))}
   *
   * @param name task name
   * @param minutes wait duration in minutes
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitMinutes(String name, int minutes) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.minutes(minutes))));
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=FuncDSLWaitTest -pl experimental/fluent/func`

Expected: All 7 tests PASS

- [ ] **Step 5: Commit**

```bash
git add experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java \
        experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java
git commit -m "feat: add waitSeconds and waitMinutes to FuncDSL"
```

---

### Task 6: Add remaining FuncDSL convenience methods

**Files:**
- Modify: `experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java`
- Modify: `experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java`

- [ ] **Step 1: Write failing tests**

Add to `FuncDSLWaitTest.java`:

```java
  @Test
  public void when_wait_hours_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitHours(2)))
            .build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertThat(inline.getHours()).isEqualTo(2);
  }

  @Test
  public void when_wait_hours_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitHours("longPause", 3)))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("longPause");
    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getHours())
        .isEqualTo(3);
  }

  @Test
  public void when_wait_days_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitDays(1)))
            .build();

    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getDays())
        .isEqualTo(1);
  }

  @Test
  public void when_wait_days_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitDays("dailyDelay", 5)))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("dailyDelay");
    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getDays())
        .isEqualTo(5);
  }

  @Test
  public void when_wait_millis_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitMillis(500)))
            .build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertThat(inline.getMilliseconds()).isEqualTo(500);
  }

  @Test
  public void when_wait_millis_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitMillis("shortPause", 250)))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("shortPause");
    assertThat(
            wf.getDo()
                .get(0)
                .getTask()
                .getWaitTask()
                .getWait()
                .getDurationInline()
                .getMilliseconds())
        .isEqualTo(250);
  }

  @Test
  public void when_wait_with_duration() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.wait(java.time.Duration.ofMinutes(5).plusSeconds(30))))
            .build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertThat(inline.getMinutes()).isEqualTo(5);
    assertThat(inline.getSeconds()).isEqualTo(30);
  }

  @Test
  public void when_wait_with_duration_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.wait("custom", java.time.Duration.ofHours(1))))
            .build();

    assertThat(wf.getDo().get(0).getName()).isEqualTo("custom");
    assertThat(wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getHours())
        .isEqualTo(1);
  }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -Dtest=FuncDSLWaitTest -pl experimental/fluent/func`

Expected: FAIL with "cannot resolve method" errors

- [ ] **Step 3: Implement waitHours, waitDays, waitMillis, and wait(Duration)**

Add to `FuncDSL.java` after the waitMinutes methods:

```java
  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with hours.
   *
   * <p>Example: {@code tasks(waitHours(2))}
   *
   * @param hours wait duration in hours
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitHours(int hours) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.hours(hours))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with hours.
   *
   * <p>Example: {@code tasks(waitHours("longPause", 2))}
   *
   * @param name task name
   * @param hours wait duration in hours
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitHours(String name, int hours) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.hours(hours))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with days.
   *
   * <p>Example: {@code tasks(waitDays(1))}
   *
   * @param days wait duration in days
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitDays(int days) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.days(days))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with days.
   *
   * <p>Example: {@code tasks(waitDays("dailyDelay", 1))}
   *
   * @param name task name
   * @param days wait duration in days
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitDays(String name, int days) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.days(days))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with
   * milliseconds.
   *
   * <p>Example: {@code tasks(waitMillis(500))}
   *
   * @param milliseconds wait duration in milliseconds
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitMillis(int milliseconds) {
    return list -> list.wait(w -> w.wait(t -> t.duration(d -> d.milliseconds(milliseconds))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with
   * milliseconds.
   *
   * <p>Example: {@code tasks(waitMillis("shortPause", 500))}
   *
   * @param name task name
   * @param milliseconds wait duration in milliseconds
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer waitMillis(String name, int milliseconds) {
    return list -> list.wait(name, w -> w.wait(t -> t.duration(d -> d.milliseconds(milliseconds))));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a {@code wait} task configured with a Java
   * {@link java.time.Duration}.
   *
   * <p>The Duration is converted to DurationInline format.
   *
   * <p>Example: {@code tasks(wait(Duration.ofMinutes(5).plusSeconds(30)))}
   *
   * @param duration wait duration as a Java Duration
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer wait(java.time.Duration duration) {
    return list -> list.wait(w -> w.wait(duration));
  }

  /**
   * Create a {@link FuncTaskConfigurer} that adds a named {@code wait} task configured with a Java
   * {@link java.time.Duration}.
   *
   * <p>The Duration is converted to DurationInline format.
   *
   * <p>Example: {@code tasks(wait("pause", Duration.ofMinutes(5)))}
   *
   * @param name task name
   * @param duration wait duration as a Java Duration
   * @return a {@link FuncTaskConfigurer} that adds a WaitTask
   */
  public static FuncTaskConfigurer wait(String name, java.time.Duration duration) {
    return list -> list.wait(name, w -> w.wait(duration));
  }
```

- [ ] **Step 4: Run all tests to verify they pass**

Run: `mvn test -Dtest=FuncDSLWaitTest -pl experimental/fluent/func`

Expected: All 15 tests PASS

- [ ] **Step 5: Run all fluent tests to ensure no regressions**

Run: `mvn test -pl fluent/spec,experimental/fluent/func`

Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add experimental/fluent/func/src/main/java/io/serverlessworkflow/fluent/func/dsl/FuncDSL.java \
        experimental/fluent/func/src/test/java/io/serverlessworkflow/fluent/func/FuncDSLWaitTest.java
git commit -m "feat: add waitHours, waitDays, waitMillis, and wait(Duration) to FuncDSL"
```

---

## Summary

This plan implements ergonomic wait convenience methods for both DSL and FuncDSL:

**DSL.java additions:**
- `waitSeconds(int)` / `waitSeconds(String, int)`
- `waitMinutes(int)` / `waitMinutes(String, int)`
- `waitHours(int)` / `waitHours(String, int)`
- `waitDays(int)` / `waitDays(String, int)`
- `waitMillis(int)` / `waitMillis(String, int)`
- `wait(Duration)` / `wait(String, Duration)`

**FuncDSL.java additions:**
- All basic wait methods (string expression, TimeoutBuilder consumer)
- All convenience methods from DSL (matching signatures but returning FuncTaskConfigurer)

**Test Coverage:**
- 12 tests for DSL wait methods
- 15 tests for FuncDSL wait methods
- Tests verify both named and unnamed variants
- Tests verify Duration conversion to DurationInline
