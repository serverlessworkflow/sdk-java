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
import static io.serverlessworkflow.fluent.agentic.Agents.*;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.*;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newAstrologyAgent;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newAudienceEditor;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newCreativeWriter;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newFoodExpert;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newMovieExpert;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newStyleEditor;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newStyleScorer;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newSummaryStory;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.conditional;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.doTasks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.langchain4j.agentic.scope.AgenticScope;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AgenticWorkflowHelperIT {

  @Test
  @DisplayName("Sequential agents via DSL.sequence(...)")
  public void sequentialWorkflow() {
    var creativeWriter = newCreativeWriter();
    var audienceEditor = newAudienceEditor();
    var styleEditor = newStyleEditor();

    NovelCreator novelCreator =
        AgenticWorkflow.of(NovelCreator.class)
            .flow(workflow("seqFlow").sequence(creativeWriter, audienceEditor, styleEditor))
            .build();

    String story = novelCreator.createNovel("dragons and wizards", "young adults", "fantasy");
    assertNotNull(story);
  }

  @Test
  @DisplayName("Parallel agents via DSL.parallel(...)")
  public void parallelWorkflow() {
    var foodExpert = newFoodExpert();
    var movieExpert = newMovieExpert();

    Function<AgenticScope, List<EveningPlan>> planEvening =
        input -> {
          List<String> movies = (List<String>) input.readState("movies");
          List<String> meals = (List<String>) input.readState("meals");

          int max = Math.min(movies.size(), meals.size());
          return IntStream.range(0, max)
              .mapToObj(i -> new EveningPlan(movies.get(i), meals.get(i)))
              .toList();
        };

    EveningPlannerAgent eveningPlannerAgent =
        AgenticWorkflow.of(EveningPlannerAgent.class)
            .flow(workflow("parallelFlow").parallel(foodExpert, movieExpert).outputAs(planEvening))
            .build();
    List<EveningPlan> result = eveningPlannerAgent.plan("romantic");
    assertEquals(3, result.size());
  }

  @Test
  @DisplayName("Loop test with condition")
  public void loopTest() {
    var creativeWriter = newCreativeWriter();
    var styleScorer = newStyleScorer();
    var styleEditor = newStyleEditor();

    Predicate<AgenticScope> until = s -> s.readState("score", 0.0) >= 0.8;

    StyledWriter styledWriter =
        AgenticWorkflow.of(StyledWriter.class)
            .flow(workflow("loopFlow").agent(creativeWriter).loop(until, styleScorer, styleEditor))
            .build();

    String story = styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");
    assertNotNull(story);
  }

  @Test
  @DisplayName("Looping agents via DSL.loop(...)")
  public void loopWorkflowWithMaxIterations() {
    var creativeWriter = newCreativeWriter();
    var audienceEditor = newAudienceEditor();
    var styleEditor = newStyleEditor();
    var summaryStory = newSummaryStory();

    NovelCreator novelCreator =
        AgenticWorkflow.of(NovelCreator.class)
            .flow(
                workflow("seqFlow")
                    .agent(creativeWriter)
                    .sequence(audienceEditor, styleEditor)
                    .agent(summaryStory))
            .build();

    String story = novelCreator.createNovel("dragons and wizards", "young adults", "fantasy");
    assertNotNull(story);
  }

  @Test
  @DisplayName("Human in the loop")
  public void humanInTheLoop() {
    var astrologyAgent = newAstrologyAgent();

    var askSign =
        new Function<AgenticScope, AgenticScope>() {
          @Override
          public AgenticScope apply(AgenticScope holder) {
            System.out.println("What's your star sign?");
            // var sign = System.console().readLine();
            holder.writeState("sign", "piscis");
            return holder;
          }
        };

    String result =
        AgenticWorkflow.of(HoroscopeAgent.class)
            .flow(
                workflow("humanInTheLoop")
                    .inputFrom(askSign)
                    // .tasks(tasks -> tasks.callFn(fn(askSign))) // TODO should work too
                    .agent(astrologyAgent))
            .build()
            .invoke("My name is Mario. What is my horoscope?");

    assertNotNull(result);
  }

  @Test
  @DisplayName("Error handling with agents")
  public void errorHandling() {
    var creativeWriter = newCreativeWriter();
    var audienceEditor = newAudienceEditor();
    var styleEditor = newStyleEditor();

    Workflow wf =
        workflow("seqFlow")
            .sequence("process", creativeWriter, audienceEditor, styleEditor)
            .build();

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

  @Test
  @DisplayName("Conditional agents via choice(...)")
  public void conditionalWorkflow() {

    var category = newCategoryRouter();
    var medicalExpert = newMedicalExpert();
    var technicalExpert = newTechnicalExpert();
    var legalExpert = newLegalExpert();

    Workflow wf =
        workflow("conditional")
            .sequence("process", category)
            .tasks(
                doTasks(
                    conditional(RequestCategory.MEDICAL::equals, medicalExpert),
                    conditional(RequestCategory.TECHNICAL::equals, technicalExpert),
                    conditional(RequestCategory.LEGAL::equals, legalExpert)))
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
}
