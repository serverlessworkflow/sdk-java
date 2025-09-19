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

import static io.serverlessworkflow.fluent.agentic.AgentWorkflowBuilder.workflow;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgentDslWorkflowTest {

  @Test
  @DisplayName("Sequential agents via DSL.sequence(...)")
  void dslSequentialAgents() {
    var a1 = AgentsUtils.newMovieExpert();
    var a2 = AgentsUtils.newMovieExpert();
    var a3 = AgentsUtils.newMovieExpert();

    Workflow wf = workflow("seqFlow").tasks(tasks -> tasks.sequence("process", a1, a2, a3)).build();

    this.assertSequentialAgents(wf);
  }

  @Test
  @DisplayName("Sequential agents via DSL.sequence(...)")
  void dslSequentialAgentsShortcut() {
    var a1 = AgentsUtils.newMovieExpert();
    var a2 = AgentsUtils.newMovieExpert();
    var a3 = AgentsUtils.newMovieExpert();

    Workflow wf = workflow("seqFlow").sequence("process", a1, a2, a3).build();

    this.assertSequentialAgents(wf);
  }

  private void assertSequentialAgents(Workflow wf) {
    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(3);
    // names should be process-0, process-1, process-2
    assertThat(items.get(0).getName()).isEqualTo("process-0");
    assertThat(items.get(1).getName()).isEqualTo("process-1");
    assertThat(items.get(2).getName()).isEqualTo("process-2");
    // each is a CallTaskJava under the hood
    items.forEach(it -> assertThat(it.getTask().getCallTask()).isInstanceOf(CallTaskJava.class));
  }

  @Test
  @DisplayName("Bare Java‑bean call via DSL.callFn(...)")
  void dslCallFnBare() {
    Workflow wf =
        workflow("beanCall")
            .tasks(tasks -> tasks.callFn("plainCall", fn -> fn.function(ctx -> "pong")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(1);
    TaskItem ti = items.get(0);
    assertThat(ti.getName()).isEqualTo("plainCall");
    assertThat(ti.getTask().getCallTask()).isInstanceOf(CallTaskJava.class);
  }

  @Test
  void dslLoopAgents() {
    var scorer = AgentsUtils.newMovieExpert(); // re‑using MovieExpert as a stand‑in for scoring
    var editor = AgentsUtils.newMovieExpert(); // likewise

    Workflow wf =
        AgentWorkflowBuilder.workflow("retryFlow")
            .loop("reviewLoop", c -> c.readState("score", 0).doubleValue() > 0.75, scorer, editor)
            .build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(1);

    var fn = (ForTaskFunction) items.get(0).getTask().getForTask();
    assertThat(fn.getDo()).isNotNull();
    assertThat(fn.getDo()).hasSize(2);
    fn.getDo()
        .forEach(si -> assertThat(si.getTask().getCallTask()).isInstanceOf(CallTaskJava.class));
  }

  @Test
  void dslParallelAgents() {
    var a1 = AgentsUtils.newMovieExpert();
    var a2 = AgentsUtils.newMovieExpert();

    Workflow wf = workflow("forkFlow").parallel("fanout", a1, a2).build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(1);

    var fork = items.get(0).getTask().getForkTask();
    // two branches created
    assertThat(fork.getFork().getBranches()).hasSize(2);
    // branch names follow "branch-{index}-{name}"
    assertThat(fork.getFork().getBranches().get(0).getName()).isEqualTo("branch-0-fanout");
    assertThat(fork.getFork().getBranches().get(1).getName()).isEqualTo("branch-1-fanout");
  }

  @Test
  void dslMixSpecAndAgent() {
    var agent = AgentsUtils.newMovieExpert();

    Workflow wf =
        workflow("mixedFlow")
            .tasks(
                tasks ->
                    tasks
                        .set("init", s -> s.expr("$.initialized = true"))
                        .agent("callAgent", agent)
                        .set("done", "$.status = 'OK'"))
            .build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(3);
    // init is a SetTask
    assertThat(items.get(0).getTask().getSetTask()).isNotNull();
    // agent call becomes a CallTaskJava
    assertThat(items.get(1).getTask().getCallTask()).isInstanceOf(CallTaskJava.class);
    // done is another SetTask
    assertThat(items.get(2).getTask().getSetTask()).isNotNull();
  }
}
