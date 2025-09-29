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
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.fn;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.*;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.AudienceEditor;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.CreativeWriter;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.StyleEditor;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Models.BASE_MODEL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.WorkflowAgentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import io.serverlessworkflow.fluent.agentic.AgenticServices;
import io.serverlessworkflow.fluent.agentic.AgentsUtils;
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
    var creativeWriter = AgentsUtils.newCreativeWriter();
    var audienceEditor = AgentsUtils.newAudienceEditor();
    var styleEditor = AgentsUtils.newStyleEditor();

    NovelCreator novelCreator = AgenticServices.of(NovelCreator.class)
            .flow(workflow("seqFlow")
                    .sequence(creativeWriter, audienceEditor, styleEditor)
            ).build();

    String story = novelCreator.createNovel("dragons and wizards", "young adults", "fantasy");
    assertNotNull(story);
  }

  @Test
  public void parallelWorkflow() {
    var foodExpert = AgentsUtils.newFoodExpert();
    var movieExpert = AgentsUtils.newMovieExpert();

    EveningPlannerAgent eveningPlannerAgent = AgenticServices.of(EveningPlannerAgent.class)
            .flow(workflow("parallelFlow")
                    .parallel(foodExpert, movieExpert)
            ).build();
    List<EveningPlan> result = eveningPlannerAgent.plan("romantic");
    assertTrue(result.size() > 0);
  }

  @Test
  public void loopTest() {
    var creativeWriter = AgentsUtils.newCreativeWriter();
    var scorer = AgentsUtils.newStyleScorer();
    var editor = AgentsUtils.newStyleEditor();

    Predicate<AgenticScope> until = s -> s.readState("score", 0).doubleValue() >= 0.8;


    StyledWriter styledWriter = AgenticServices.of(StyledWriter.class)
            .flow(workflow("loopFlow").agent(creativeWriter)
                    .loop(until, scorer, editor)
            ).build();

    String story = styledWriter.writeStoryWithStyle("dragons and wizards", "fantasy");
    assertNotNull(story);
  }

  @Test
  public void humanInTheLoop() {
    var astrologyAgent = AgentsUtils.newAstrologyAgent();

    var askSign = new Function<Map<String, Object>, Map<String, Object>>() {
      @Override
      public Map<String, Object> apply(Map<String, Object> holder) {
        System.out.println("What's your star sign?");
        //var sign = System.console().readLine();
        holder.put("sign", "piscis");
        return holder;
      }
    };

    String result = AgenticServices.of(HoroscopeAgent.class)
            .flow(workflow("humanInTheLoop")
                    .tasks(tasks -> tasks.callFn(fn(askSign)))
                    .agent(astrologyAgent))
            .build()
            .invoke("My name is Mario. What is my horoscope?");

    assertNotNull(result);

  }

}
