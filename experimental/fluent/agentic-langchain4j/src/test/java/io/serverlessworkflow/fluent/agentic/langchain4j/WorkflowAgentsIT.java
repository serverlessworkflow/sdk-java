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
package io.serverlessworkflow.fluent.agentic.langchain4j;

import static io.serverlessworkflow.fluent.agentic.AgentWorkflowBuilder.workflow;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newAstrologyAgent;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newAudienceEditor;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newCreativeWriter;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newFoodExpert;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newMovieExpert;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newStyleEditor;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newStyleScorer;
import static io.serverlessworkflow.fluent.agentic.AgentsUtils.newSummaryStory;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.*;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.AudienceEditor;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.CreativeWriter;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.StyleEditor;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Models.BASE_MODEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.WorkflowAgentsBuilder;
import io.serverlessworkflow.fluent.agentic.AgenticServices;
import io.serverlessworkflow.fluent.agentic.AgentsUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class WorkflowAgentsIT {

  @Test
  void sequential_agents_tests() {
    WorkflowAgentsBuilder builder = new LC4JWorkflowBuilder();

    CreativeWriter creativeWriter =
        spy(
            dev.langchain4j.agentic.AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

    AudienceEditor audienceEditor =
        spy(
            dev.langchain4j.agentic.AgenticServices.agentBuilder(AudienceEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

    StyleEditor styleEditor =
        spy(
            dev.langchain4j.agentic.AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

    UntypedAgent novelCreator =
        builder
            .sequenceBuilder()
            .subAgents(creativeWriter, audienceEditor, styleEditor)
            .outputName("story")
            .build();

    Map<String, Object> input =
        Map.of(
            "topic", "dragons and wizards",
            "style", "fantasy",
            "audience", "young adults");

    String story = (String) novelCreator.invoke(input);
    System.out.println(story);

    verify(creativeWriter).generateStory("dragons and wizards");
    verify(audienceEditor).editStory(any(), eq("young adults"));
    verify(styleEditor).editStory(any(), eq("fantasy"));
  }

  @Test
  public void sequenceHelperTest() {
    var creativeWriter = newCreativeWriter();
    var audienceEditor = newAudienceEditor();
    var styleEditor = newStyleEditor();

    AgentsUtils.NovelCreator novelCreator =
        io.serverlessworkflow.fluent.agentic.AgenticServices.of(AgentsUtils.NovelCreator.class)
            .flow(workflow("seqFlow").sequence(creativeWriter, audienceEditor, styleEditor))
            .build();

    String story = novelCreator.createNovel("dragons and wizards", "young adults", "fantasy");
    assertNotNull(story);
  }

  @Test
  public void agentAndSequenceHelperTest() {
    var creativeWriter = newCreativeWriter();
    var audienceEditor = newAudienceEditor();
    var styleEditor = newStyleEditor();

    AgentsUtils.NovelCreator novelCreator =
        io.serverlessworkflow.fluent.agentic.AgenticServices.of(AgentsUtils.NovelCreator.class)
            .flow(workflow("seqFlow").agent(creativeWriter).sequence(audienceEditor, styleEditor))
            .build();

    String story = novelCreator.createNovel("dragons and wizards", "young adults", "fantasy");
    assertNotNull(story);
  }

  @Test
  public void agentAndSequenceAndAgentHelperTest() {
    var creativeWriter = newCreativeWriter();
    var audienceEditor = newAudienceEditor();
    var styleEditor = newStyleEditor();
    var summaryStory = newSummaryStory();

    AgentsUtils.NovelCreator novelCreator =
        io.serverlessworkflow.fluent.agentic.AgenticServices.of(AgentsUtils.NovelCreator.class)
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
        AgenticServices.of(EveningPlannerAgent.class)
            .flow(workflow("parallelFlow").parallel(foodExpert, movieExpert).outputAs(planEvening))
            .build();
    List<EveningPlan> result = eveningPlannerAgent.plan("romantic");
    assertEquals(3, result.size());
  }

  @Test
  public void loopTest() {
    var creativeWriter = newCreativeWriter();
    var scorer = newStyleScorer();
    var editor = newStyleEditor();

    Predicate<AgenticScope> until = s -> s.readState("score", 0.0) >= 0.8;

    StyledWriter styledWriter =
        AgenticServices.of(StyledWriter.class)
            .flow(workflow("loopFlow").agent(creativeWriter).loop(until, scorer, editor))
            .build();

    String story = styledWriter.writeStoryWithStyle("dragons and wizards", "fantasy");
    assertNotNull(story);
  }

  @Test
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
        AgenticServices.of(Agents.HoroscopeAgent.class)
            .flow(
                workflow("humanInTheLoop")
                    .inputFrom(askSign)
                    // .tasks(tasks -> tasks.callFn(fn(askSign))) // TODO should work too
                    .agent(astrologyAgent))
            .build()
            .invoke("My name is Mario. What is my horoscope?");

    assertNotNull(result);
  }
}
