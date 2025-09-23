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
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.conditional;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.doTasks;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.tasks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LC4JEquivalenceIT {

  @Test
  @DisplayName("Sequential agents via DSL.sequence(...)")
  public void sequentialWorkflow() {
    var a1 = AgentsUtils.newCreativeWriter();
    var a2 = AgentsUtils.newAudienceEditor();
    var a3 = AgentsUtils.newStyleEditor();

    Workflow wf = workflow("seqFlow").tasks(tasks -> tasks.sequence("process", a1, a2, a3)).build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(3);

    assertThat(items.get(0).getName()).isEqualTo("process-0");
    assertThat(items.get(1).getName()).isEqualTo("process-1");
    assertThat(items.get(2).getName()).isEqualTo("process-2");
    items.forEach(it -> assertThat(it.getTask().getCallTask()).isInstanceOf(CallTaskJava.class));

    Map<String, Object> input =
        Map.of(
            "topic", "dragons and wizards",
            "style", "fantasy",
            "audience", "young adults");

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertThat(result).containsKey("story");
  }

  @Test
  @DisplayName("Looping agents via DSL.loop(...)")
  public void loopWorkflow() {

    var scorer = AgentsUtils.newStyleScorer();
    var editor = AgentsUtils.newStyleEditor();

    Workflow wf =
        AgentWorkflowBuilder.workflow("retryFlow")
            .loop("reviewLoop", c -> c.readState("score", 0).doubleValue() >= 0.8, scorer, editor)
            .build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(1);

    var fn = (ForTaskFunction) items.get(0).getTask().getForTask();
    assertThat(fn.getDo()).isNotNull();
    assertThat(fn.getDo()).hasSize(2);
    fn.getDo()
        .forEach(si -> assertThat(si.getTask().getCallTask()).isInstanceOf(CallTaskJava.class));

    Map<String, Object> input =
        Map.of(
            "story", "dragons and wizards",
            "style", "comedy");

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertThat(result).containsKey("story");
  }

  @Test
  @DisplayName("Looping agents via DSL.loop(...)")
  public void loopWorkflowWithMaxIterations() {
    var scorer = AgentsUtils.newStyleScorer();
    var editor = AgentsUtils.newStyleEditor();

    Workflow wf =
        AgentWorkflowBuilder.workflow("maxFlow")
            .tasks(
                d ->
                    d.loop(
                        "limit",
                        l ->
                            l.maxIterations(5)
                                .exitCondition(c -> c.readState("score", 0).doubleValue() >= 0.8)
                                .subAgents("sub", scorer, editor)))
            .build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(1);

    var fn = (ForTaskFunction) items.get(0).getTask().getForTask();
    assertThat(fn.getDo()).isNotNull();
    assertThat(fn.getDo()).hasSize(2);
    fn.getDo()
        .forEach(si -> assertThat(si.getTask().getCallTask()).isInstanceOf(CallTaskJava.class));

    Map<String, Object> input =
        Map.of(
            "story", "dragons and wizards",
            "style", "comedy");

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertThat(result).containsKey("story");
  }

  @Test
  @DisplayName("Parallel agents via DSL.parallel(...)")
  public void parallelWorkflow() {
    var a1 = AgentsUtils.newFoodExpert();
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

    Map<String, Object> input = Map.of("mood", "I am hungry and bored");

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertEquals("Fake conflict response", result.get("meals"));
    assertEquals("Fake conflict response", result.get("movies"));
  }

  // TODO
  @Test
  @DisplayName("Error handling with agents")
  public void errorHandling() {
    var a1 = AgentsUtils.newCreativeWriter();
    var a2 = AgentsUtils.newAudienceEditor();
    var a3 = AgentsUtils.newStyleEditor();

    Workflow wf = workflow("seqFlow").tasks(tasks -> tasks.sequence("process", a1, a2, a3)).build();

    List<TaskItem> items = wf.getDo();
    assertThat(items).hasSize(3);

    assertThat(items.get(0).getName()).isEqualTo("process-0");
    assertThat(items.get(1).getName()).isEqualTo("process-1");
    assertThat(items.get(2).getName()).isEqualTo("process-2");
    items.forEach(it -> assertThat(it.getTask().getCallTask()).isInstanceOf(CallTaskJava.class));

    Map<String, Object> input =
        Map.of(
            "style", "fantasy",
            "audience", "young adults");

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertThat(result).containsKey("story");
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("Conditional agents via choice(...)")
  public void conditionalWorkflow() {

    var category = AgentsUtils.newCategoryRouter();
    var a1 = AgentsUtils.newMedicalExpert();
    var a2 = AgentsUtils.newTechnicalExpert();
    var a3 = AgentsUtils.newLegalExpert();

    Workflow wf =
        workflow("conditional")
            .sequence("process", category)
            .tasks(
                t ->
                    t.switchCase(
                        p ->
                            p.onPredicate(
                                item ->
                                    item.when(
                                            m ->
                                                "unknown"
                                                    .equals(
                                                        ((Map<String, Object>) m).get("category")))
                                        .then(FlowDirectiveEnum.END))))
            .tasks(
                doTasks(
                    conditional(
                        m -> "medical".equals(((Map<String, Object>) m).get("category")), a1),
                    conditional(
                        m -> "technical".equals(((Map<String, Object>) m).get("category")), a2),
                    conditional(
                        m -> "legal".equals(((Map<String, Object>) m).get("category")), a3)))
            .build();

    Map<String, Object> input = Map.of("question", "What is the best treatment for a common cold?");

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertThat(result).containsKey("response");
  }

  @Test
  @DisplayName("Human in the loop")
  public void humanInTheLoop() {

    AtomicReference<String> request = new AtomicReference<>();

    HumanInTheLoop humanInTheLoop =
        AgenticServices.humanInTheLoopBuilder()
            .description("Please provide the horoscope request")
            .inputName("request")
            .outputName("sign")
            .requestWriter(q -> request.set("My name is Mario. What is my horoscope?"))
            .responseReader(() -> "piscis")
            .build();

    var a1 = AgentsUtils.newAstrologyAgent();

    Workflow wf = workflow("seqFlow").sequence("process", a1, humanInTheLoop).build();

    assertThat(wf.getDo()).hasSize(2);

    Map<String, Object> input = Map.of("request", "My name is Mario. What is my horoscope?");

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertThat(request.get()).isEqualTo("My name is Mario. What is my horoscope?");
    assertThat(result).containsEntry("sign", "piscis");
  }
}
