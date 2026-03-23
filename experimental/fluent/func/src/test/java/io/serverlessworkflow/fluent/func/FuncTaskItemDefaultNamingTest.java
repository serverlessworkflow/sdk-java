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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.TaskItem;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FuncTaskItemDefaultNamingTest {

  @Test
  void testFuncForEachTaskAutoNaming() {
    // Testing the Func domain builder directly to ensure the functional DSL
    // behaves identically to the spec DSL regarding default naming.
    FuncTaskItemListBuilder funcBuilder = new FuncTaskItemListBuilder(0);

    funcBuilder.forEach(
        null,
        f ->
            f.each("item")
                .in("$.list")
                // Inner tasks inside the Func ForEach loop
                .tasks(
                    tb ->
                        tb.set(null, "$.a = 1") // Using the Func set(name, expr) shortcut
                            .http(null, hb -> hb.endpoint("http://func"))));

    List<TaskItem> topItems = funcBuilder.build();
    assertEquals(1, topItems.size());
    assertEquals("for-0", topItems.get(0).getName(), "Top level Func ForEach should be for-0");

    // Fetch the inner tasks of the Func 'for' loop
    List<TaskItem> nestedItems = topItems.get(0).getTask().getForTask().getDo();
    assertNotNull(nestedItems, "Nested Func forEach items must not be null");
    assertEquals(2, nestedItems.size());

    // Verify inner builder list indexes independently starting at 0
    assertEquals("set-0", nestedItems.get(0).getName());
    assertEquals("http-1", nestedItems.get(1).getName());
  }

  @Test
  void testFuncForkTaskMultipleBranchesAppends() {
    FuncForkTaskBuilder forkBuilder = new FuncForkTaskBuilder();

    // 1. Call branches() - list is initially empty, offset should be 0
    forkBuilder.branches(b -> b.set(null, "$.a = 1"));

    // 2. Call branch() - list has 1 item, so it should use index 1
    forkBuilder.branch((Object x) -> x);

    // 3. Call branches() again - list has 2 items, offset should be 2
    forkBuilder.branches(b -> b.set(null, "$.b = 2"));

    // Build and verify
    List<TaskItem> branches = forkBuilder.build().getFork().getBranches();

    assertEquals(3, branches.size(), "All branches should be appended to the list");

    assertEquals("set-0", branches.get(0).getName(), "First branches() call starts at 0");
    assertEquals("branch-1", branches.get(1).getName(), "branch() call picks up index 1");
    assertEquals("set-2", branches.get(2).getName(), "Second branches() call picks up index 2");
  }

  @Test
  void testFuncForTaskMultipleTasksAppends() {
    io.serverlessworkflow.fluent.func.FuncForTaskBuilder forBuilder =
        new io.serverlessworkflow.fluent.func.FuncForTaskBuilder();

    forBuilder.each("item").in("$.list");

    // 1. Fluent builder - list is empty, offset is 0
    forBuilder.tasks(tb -> tb.set(null, "$.a = 1"));

    // 2. Functional LoopFunction shortcut - list has 1 item, index should be 1
    forBuilder.tasks(null, (Object ctx, Object item) -> ctx);

    // 3. Fluent builder again - list has 2 items, offset should be 2
    forBuilder.tasks(tb -> tb.http(null, hb -> hb.endpoint("http://test")));

    // Build and verify
    List<TaskItem> forTasks = forBuilder.build().getDo();

    assertEquals(3, forTasks.size(), "All tasks should be appended to the loop");

    assertEquals("set-0", forTasks.get(0).getName(), "First fluent block starts at 0");
    assertEquals("for-task-1", forTasks.get(1).getName(), "Functional task picks up index 1");
    assertEquals("http-2", forTasks.get(2).getName(), "Second fluent block picks up index 2");
  }
}
