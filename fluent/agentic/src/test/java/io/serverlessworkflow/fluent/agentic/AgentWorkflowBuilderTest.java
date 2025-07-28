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

import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newMovieExpert;
import static io.serverlessworkflow.fluent.agentic.Models.BASE_MODEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;

import dev.langchain4j.agentic.AgentServices;
import dev.langchain4j.agentic.Cognisphere;
import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgentWorkflowBuilderTest {

  @Test
  public void verifyAgentCall() {
    Agents.MovieExpert movieExpert =
        spy(
            AgentServices.agentBuilder(Agents.MovieExpert.class)
                .outputName("movies")
                .chatModel(BASE_MODEL)
                .build());

    Workflow workflow =
        AgentWorkflowBuilder.workflow().tasks(tasks -> tasks.agent("myAgent", movieExpert)).build();

    assertNotNull(workflow);
    assertEquals(1, workflow.getDo().size());
    assertInstanceOf(CallJava.class, workflow.getDo().get(0).getTask().getCallTask().get());
  }

  @Test
  void sequenceAgents() {
    Agents.MovieExpert movieExpert = newMovieExpert();
    Workflow wf =
        AgentWorkflowBuilder.workflow("seqFlow")
            .tasks(d -> d.sequence("lineup", movieExpert, movieExpert))
            .build();

    assertThat(wf.getDo()).hasSize(2);
    assertThat(wf.getDo().get(0).getName()).isEqualTo("lineup-0");
    assertThat(wf.getDo().get(1).getName()).isEqualTo("lineup-1");
    wf.getDo()
        .forEach(
            ti -> {
              assertThat(ti.getTask().getCallTask()).isNotNull();
              assertThat(ti.getTask().getCallTask().get()).isNotNull();
            });
  }

  @Test
  void mixSpecAndAgent() {
    Workflow wf =
        AgentWorkflowBuilder.workflow("mixFlow")
            .tasks(
                d ->
                    d.set("init", s -> s.expr("$.mood = 'comedy'"))
                        .agent("pickMovies", newMovieExpert())
                        .set("done", "$.done = true"))
            .build();

    assertThat(wf.getDo()).hasSize(3);
    assertThat(wf.getDo().get(0).getTask().getSetTask()).isNotNull();
    assertThat(wf.getDo().get(1).getTask().getCallTask().get()).isNotNull();
    assertThat(wf.getDo().get(2).getTask().getSetTask()).isNotNull();
  }

  @Test
  void loopOnlyAgents() {
    Agents.MovieExpert expert = newMovieExpert();

    Workflow wf =
        AgentWorkflowBuilder.workflow().tasks(d -> d.loop(l -> l.subAgents(expert))).build();

    assertNotNull(wf);
    assertThat(wf.getDo()).hasSize(1);

    TaskItem ti = wf.getDo().get(0);
    Task t = ti.getTask();
    assertThat(t.getForTask()).isInstanceOf(ForTaskFunction.class);

    ForTaskFunction fn = (ForTaskFunction) t.getForTask();
    assertNotNull(fn.getDo());
    assertThat(fn.getDo()).hasSize(1);
    assertNotNull(fn.getDo().get(0).getTask().getCallTask().get());
  }

  @Test
  void loopWithMaxIterationsAndExitCondition() {
    Agents.MovieExpert expert = newMovieExpert();

    AtomicInteger max = new AtomicInteger(4);
    Predicate<Cognisphere> exit =
        cog -> {
          // stop when we already have at least one movie picked in state
          var movies = cog.readState("movies", null);
          return movies != null;
        };

    Workflow wf =
        AgentWorkflowBuilder.workflow("loop-ctrl")
            .tasks(
                d ->
                    d.loop(
                        "refineMovies",
                        l ->
                            l.maxIterations(max.get())
                                .exitCondition(exit)
                                .subAgents("picker", expert)))
            .build();

    TaskItem ti = wf.getDo().get(0);
    ForTaskFunction fn = (ForTaskFunction) ti.getTask().getForTask();

    assertNotNull(fn.getCollection(), "Synthetic collection should exist for maxIterations");
    assertNotNull(fn.getWhilePredicate(), "While predicate set from exitCondition");
    assertThat(fn.getDo()).hasSize(1);
  }

  @Test
  @DisplayName("parallel() creates one ForkTask with N callFn branches")
  void parallelAgents() {
    Agents.MovieExpert a1 = AgentsUtils.newMovieExpert();
    Agents.MovieExpert a2 = AgentsUtils.newMovieExpert();
    Agents.MovieExpert a3 = AgentsUtils.newMovieExpert();

    Workflow wf =
        AgentWorkflowBuilder.workflow("parallelFlow")
            .tasks(d -> d.parallel("p", a1, a2, a3))
            .build();

    assertThat(wf.getDo()).hasSize(1);
    TaskItem top = wf.getDo().get(0);
    Task task = top.getTask();
    assertThat(task.getForkTask()).isInstanceOf(ForkTask.class);

    ForkTask fork = task.getForkTask();
    assertThat(fork.getFork().getBranches()).hasSize(3);

    fork.getFork()
        .getBranches()
        .forEach(
            branch -> {
              assertThat(branch.getTask().getCallTask().get()).isInstanceOf(CallJava.class);
            });
  }
}
