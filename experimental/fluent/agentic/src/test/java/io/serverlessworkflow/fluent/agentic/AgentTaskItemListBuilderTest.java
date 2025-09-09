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
package io.serverlessworkflow.fluent.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Structural tests for AgentTaskItemListBuilder. */
class AgentTaskItemListBuilderTest {

  @Test
  @DisplayName("agent(name,obj) adds a CallTaskJava-backed TaskItem")
  void testAgentAddsCallTask() {
    AgentTaskItemListBuilder b = new AgentTaskItemListBuilder();
    Agents.MovieExpert agent = AgentsUtils.newMovieExpert();

    b.agent("my-agent", agent);

    List<TaskItem> items = b.build();
    assertThat(items).hasSize(1);
    TaskItem item = items.get(0);
    assertThat(item.getName()).isEqualTo("my-agent");

    Task task = item.getTask();
    assertThat(task.getCallTask()).isInstanceOf(CallTaskJava.class);
  }

  @Test
  @DisplayName("sequence(name, agents...) expands to N CallTask items, in order")
  void testSequence() {
    AgentTaskItemListBuilder b = new AgentTaskItemListBuilder();
    Agents.MovieExpert a1 = AgentsUtils.newMovieExpert();
    Agents.MovieExpert a2 = AgentsUtils.newMovieExpert();
    Agents.MovieExpert a3 = AgentsUtils.newMovieExpert();

    b.sequence("seq", a1, a2, a3);

    List<TaskItem> items = b.build();
    assertThat(items).hasSize(3);
    assertThat(items.get(0).getName()).isEqualTo("seq-0");
    assertThat(items.get(1).getName()).isEqualTo("seq-1");
    assertThat(items.get(2).getName()).isEqualTo("seq-2");

    // All must be call branche
    items.forEach(it -> assertThat(it.getTask().getCallTask().get()).isNotNull());
  }

  @Test
  @DisplayName("loop(name, builder) produces a ForTaskFunction with inner call branche")
  void testLoop() {
    AgentTaskItemListBuilder b = new AgentTaskItemListBuilder();
    Agents.MovieExpert scorer = AgentsUtils.newMovieExpert();
    Agents.MovieExpert editor = AgentsUtils.newMovieExpert();

    b.loop("rev-loop", loop -> loop.subAgents("inner", scorer, editor));

    List<TaskItem> items = b.build();
    assertThat(items).hasSize(1);

    TaskItem loopItem = items.get(0);
    ForTaskFunction forFn = (ForTaskFunction) loopItem.getTask().getForTask();
    assertThat(forFn).isNotNull();
    assertThat(forFn.getDo()).hasSize(2); // scorer + editor inside
    assertThat(forFn.getDo().get(0).getTask().getCallTask().get()).isNotNull();
  }
}
