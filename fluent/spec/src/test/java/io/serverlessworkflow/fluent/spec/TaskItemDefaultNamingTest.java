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
package io.serverlessworkflow.fluent.spec;

import static io.serverlessworkflow.fluent.spec.dsl.DSL.http;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.ForkTaskConfiguration;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.Workflow;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * * Unit tests to verify that TaskItems without explicit names are automatically assigned a
 * deterministic name in the format "taskType-index" (e.g., "set-0", "http-1").
 */
public class TaskItemDefaultNamingTest {

  @Test
  void testTopLevelDoListAutoNaming() {
    Workflow wf =
        WorkflowBuilder.workflow("flowAutoNameTopLevel")
            .tasks(
                d ->
                    d.set(null, s -> s.expr("$.foo = 'bar'"))
                        .http(null, http().GET().endpoint("http://example.com"))
                        .emit("", e -> e.event(ev -> ev.type("test.event")))
                        .set("explicitName", s -> s.expr("$.x = 1")) // Explicit name should be kept
                        .fork(null, ForkTaskBuilder::build) // No-op fork to check index 4
                )
            .build();

    List<TaskItem> items = wf.getDo();
    assertNotNull(items, "Do list must not be null");
    assertEquals(5, items.size(), "There should be five tasks");

    // Verify deterministic auto-naming based on list insertion order
    assertEquals("set-0", items.get(0).getName(), "First task should be set-0");
    assertEquals("http-1", items.get(1).getName(), "Second task should be http-1");
    assertEquals("emit-2", items.get(2).getName(), "Third task (empty string) should be emit-2");

    // Verify explicit names are not overwritten
    assertEquals("explicitName", items.get(3).getName(), "Explicit name must be preserved");

    // Verify index correctly counts past explicit names
    assertEquals("fork-4", items.get(4).getName(), "Fifth task should be fork-4");
  }

  @Test
  void testNestedTryCatchTaskAutoNaming() {
    Workflow wf =
        WorkflowBuilder.workflow("flowAutoNameTryCatch")
            .tasks(
                d ->
                    d.tryCatch(
                        null,
                        t ->
                            t.tryHandler(
                                tb ->
                                    tb.set(null, s -> s.expr("$.start = true"))
                                        .http(null, http().GET().endpoint("http://test"))
                                        .set(null, s -> s.expr("$.end = true")))))
            .build();

    List<TaskItem> topItems = wf.getDo();
    assertEquals(1, topItems.size());
    assertEquals("try-0", topItems.get(0).getName(), "Top level tryCatch should be tryCatch-0");

    TryTask tryTask = topItems.get(0).getTask().getTryTask();
    assertNotNull(tryTask, "TryTask should be present");

    List<TaskItem> nestedTryItems = tryTask.getTry();
    assertNotNull(nestedTryItems, "Nested try items must not be null");
    assertEquals(3, nestedTryItems.size());

    // Verify inner builder list indexes independently of the outer builder
    assertEquals("set-0", nestedTryItems.get(0).getName());
    assertEquals("http-1", nestedTryItems.get(1).getName());
    assertEquals("set-2", nestedTryItems.get(2).getName());
  }

  @Test
  void testNestedForEachTaskAutoNaming() {
    Workflow wf =
        WorkflowBuilder.workflow("flowAutoNameForEach")
            .tasks(
                d ->
                    d.forEach(
                        null,
                        f ->
                            f.each("item")
                                .in("$.list")
                                // Assuming the DSL exposes `do_` or `tasks` for inner ForEach logic
                                .tasks(
                                    tb ->
                                        tb.http(null, http().POST().endpoint("http://test"))
                                            .set(null, s -> s.expr("$.processed = true")))))
            .build();

    List<TaskItem> topItems = wf.getDo();
    assertEquals(1, topItems.size());
    assertEquals("for-0", topItems.get(0).getName());

    // Fetch the inner 'do' tasks of the 'for' loop
    List<TaskItem> nestedForItems = topItems.get(0).getTask().getForTask().getDo();
    assertNotNull(nestedForItems, "Nested forEach items must not be null");
    assertEquals(2, nestedForItems.size());

    assertEquals("http-0", nestedForItems.get(0).getName());
    assertEquals("set-1", nestedForItems.get(1).getName());
  }

  @Test
  void testNestedForkTaskAutoNaming() {
    Workflow wf =
        WorkflowBuilder.workflow("flowAutoNameFork")
            .tasks(
                d ->
                    d.fork(
                        null,
                        f ->
                            f.branches(
                                b ->
                                    b.set(null, s -> s.expr("$.a = 1"))
                                        .set(null, s -> s.expr("$.a = 2"))
                                        .http(null, http().GET().endpoint("http://b")))))
            .build();

    List<TaskItem> topItems = wf.getDo();
    assertEquals(1, topItems.size(), "Should have one top-level task");
    assertEquals("fork-0", topItems.get(0).getName(), "Top level fork should be fork-0");

    // Retrieve the ForkTaskConfiguration
    ForkTaskConfiguration forkConfig = topItems.get(0).getTask().getForkTask().getFork();
    assertNotNull(forkConfig, "Fork configuration must not be null");

    // The branches are just a List<TaskItem> built by the inner TaskItemListBuilder
    List<TaskItem> branches = forkConfig.getBranches();
    assertNotNull(branches, "Branches list must not be null");
    assertEquals(3, branches.size(), "Should have 3 branch tasks");

    // Verify the inner TaskItemListBuilder isolated its own 0-based index
    assertEquals("set-0", branches.get(0).getName(), "First branch should be set-0");
    assertEquals("set-1", branches.get(1).getName(), "Second branch should be set-1");
    assertEquals("http-2", branches.get(2).getName(), "Third branch should be http-2");
  }
}
