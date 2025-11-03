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

import static io.serverlessworkflow.fluent.agentic.Agents.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class WorkflowTests {

  @Test
  public void testAgent() throws ExecutionException, InterruptedException {
    final StorySeedAgent storySeedAgent = mock(StorySeedAgent.class);

    when(storySeedAgent.invoke(eq("A Great Story"))).thenReturn("storySeedAgent");
    when(storySeedAgent.outputKey()).thenReturn("premise");
    when(storySeedAgent.name()).thenReturn("storySeedAgent");

    Workflow workflow =
        AgentWorkflowBuilder.workflow("storyFlow")
            .tasks(d -> d.agent("story", storySeedAgent))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("title", "A Great Story");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      AgenticScope result =
          app.workflowDefinition(workflow)
              .instance(topic)
              .start()
              .get()
              .as(AgenticScope.class)
              .orElseThrow();

      assertEquals("storySeedAgent", result.readState("premise"));
    }
  }

  @Test
  public void testAgents() throws ExecutionException, InterruptedException {
    final StorySeedAgent storySeedAgent = mock(StorySeedAgent.class);
    final PlotAgent plotAgent = mock(PlotAgent.class);
    final SceneAgent sceneAgent = mock(SceneAgent.class);

    when(storySeedAgent.invoke(eq("A Great Story"))).thenReturn("storySeedAgent");
    when(storySeedAgent.outputKey()).thenReturn("premise");
    when(storySeedAgent.name()).thenReturn("storySeedAgent");

    when(plotAgent.invoke(eq("storySeedAgent"))).thenReturn("plotAgent");
    when(plotAgent.outputKey()).thenReturn("plot");
    when(plotAgent.name()).thenReturn("plotAgent");

    when(sceneAgent.invoke(eq("plotAgent"))).thenReturn("sceneAgent");
    when(sceneAgent.outputKey()).thenReturn("story");
    when(sceneAgent.name()).thenReturn("sceneAgent");

    Workflow workflow =
        AgentWorkflowBuilder.workflow("storyFlow")
            .tasks(
                d ->
                    d.agent("story", storySeedAgent)
                        .agent("plot", plotAgent)
                        .agent("scene", sceneAgent))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("title", "A Great Story");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      AgenticScope result =
          app.workflowDefinition(workflow)
              .instance(topic)
              .start()
              .get()
              .as(AgenticScope.class)
              .orElseThrow();

      assertEquals("sceneAgent", result.readState("story"));
    }
  }

  @Test
  public void testSequence() throws ExecutionException, InterruptedException {
    final StorySeedAgent storySeedAgent = mock(StorySeedAgent.class);
    final PlotAgent plotAgent = mock(PlotAgent.class);
    final SceneAgent sceneAgent = mock(SceneAgent.class);

    when(storySeedAgent.invoke(eq("A Great Story"))).thenReturn("storySeedAgent");
    when(storySeedAgent.outputKey()).thenReturn("premise");
    when(storySeedAgent.name()).thenReturn("storySeedAgent");

    when(plotAgent.invoke(eq("storySeedAgent"))).thenReturn("plotAgent");
    when(plotAgent.outputKey()).thenReturn("plot");
    when(plotAgent.name()).thenReturn("plotAgent");

    when(sceneAgent.invoke(eq("plotAgent"))).thenReturn("sceneAgent");
    when(sceneAgent.outputKey()).thenReturn("story");
    when(sceneAgent.name()).thenReturn("sceneAgent");

    Workflow workflow =
        AgentWorkflowBuilder.workflow("storyFlow")
            .tasks(d -> d.sequence("story", storySeedAgent, plotAgent, sceneAgent))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("title", "A Great Story");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      AgenticScope result =
          app.workflowDefinition(workflow)
              .instance(topic)
              .start()
              .get()
              .as(AgenticScope.class)
              .orElseThrow();

      assertEquals("sceneAgent", result.readState("story"));
    }
  }

  @Test
  public void testParallel() throws ExecutionException, InterruptedException {

    final SettingAgent setting = mock(SettingAgent.class);
    final HeroAgent hero = mock(HeroAgent.class);
    final ConflictAgent conflict = mock(ConflictAgent.class);

    when(setting.invoke(eq("sci-fi"))).thenReturn("Fake conflict response");
    when(setting.outputKey()).thenReturn("setting");
    when(setting.name()).thenReturn("setting");

    when(hero.invoke(eq("sci-fi"))).thenReturn("Fake hero response");
    when(hero.outputKey()).thenReturn("hero");
    when(hero.name()).thenReturn("hero");

    when(conflict.invoke(eq("sci-fi"))).thenReturn("Fake setting response");
    when(conflict.outputKey()).thenReturn("conflict");
    when(conflict.name()).thenReturn("conflict");

    Workflow workflow =
        AgentWorkflowBuilder.workflow("parallelFlow")
            .tasks(d -> d.parallel("story", setting, hero, conflict))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("style", "sci-fi");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Map<String, Object> result =
          app.workflowDefinition(workflow).instance(topic).start().get().asMap().orElseThrow();

      assertEquals("Fake conflict response", result.get("setting").toString());
      assertEquals("Fake hero response", result.get("hero").toString());
      assertEquals("Fake setting response", result.get("conflict").toString());
    }

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      AgenticScope result =
          app.workflowDefinition(workflow)
              .instance(topic)
              .start()
              .get()
              .as(AgenticScope.class)
              .orElseThrow();

      assertEquals("Fake conflict response", result.readState("setting").toString());
      assertEquals("Fake hero response", result.readState("hero").toString());
      assertEquals("Fake setting response", result.readState("conflict").toString());
    }
  }

  @Test
  public void testSeqAndThenParallel() throws ExecutionException, InterruptedException {
    final FactAgent factAgent = mock(FactAgent.class);
    final CultureAgent cultureAgent = mock(CultureAgent.class);
    final TechnologyAgent technologyAgent = mock(TechnologyAgent.class);

    List<String> cultureTraits =
        List.of("Alien Culture Trait 1", "Alien Culture Trait 2", "Alien Culture Trait 3");

    List<String> technologyTraits =
        List.of("Alien Technology Trait 1", "Alien Technology Trait 2", "Alien Technology Trait 3");

    when(factAgent.invoke(eq("alien"))).thenReturn("Some Fact about aliens");
    when(factAgent.outputKey()).thenReturn("fact");
    when(factAgent.name()).thenReturn("fact");

    when(cultureAgent.invoke(eq("Some Fact about aliens"))).thenReturn(cultureTraits);
    when(cultureAgent.outputKey()).thenReturn("culture");
    when(cultureAgent.name()).thenReturn("culture");

    when(technologyAgent.invoke(eq("Some Fact about aliens"))).thenReturn(technologyTraits);
    when(technologyAgent.outputKey()).thenReturn("technology");
    when(technologyAgent.name()).thenReturn("technology");
    Workflow workflow =
        AgentWorkflowBuilder.workflow("alienCultureFlow")
            .tasks(
                d ->
                    d.sequence("fact", factAgent)
                        .parallel("cultureAndTechnology", cultureAgent, technologyAgent))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("fact", "alien");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Map<String, Object> result =
          app.workflowDefinition(workflow).instance(topic).start().get().asMap().orElseThrow();

      assertEquals(cultureTraits, result.get("culture"));
      assertEquals(technologyTraits, result.get("technology"));
    }

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      AgenticScope result =
          app.workflowDefinition(workflow)
              .instance(topic)
              .start()
              .get()
              .as(AgenticScope.class)
              .orElseThrow();

      assertEquals(cultureTraits, result.readState("culture"));
      assertEquals(technologyTraits, result.readState("technology"));
    }
  }

  @Test
  @Disabled(
      "HumanInTheLoop is not a dev.langchain4j.agentic.internal.AgentSpecification, we should treat it differently once it's implemented")
  public void humanInTheLoop() throws ExecutionException, InterruptedException {
    final MeetingInvitationDraft meetingInvitationDraft = mock(MeetingInvitationDraft.class);
    when(meetingInvitationDraft.invoke(
            eq("Meeting with John Doe"),
            eq("2023-10-01"),
            eq("08:00AM"),
            eq("London"),
            eq("Discuss project updates")))
        .thenReturn("Drafted meeting invitation for John Doe");
    when(meetingInvitationDraft.outputKey()).thenReturn("draft");
    when(meetingInvitationDraft.name()).thenReturn("draft");

    final MeetingInvitationStyle meetingInvitationStyle = mock(MeetingInvitationStyle.class);
    when(meetingInvitationStyle.invoke(eq("Drafted meeting invitation for John Doe"), eq("formal")))
        .thenReturn("Styled meeting invitation for John Doe");
    when(meetingInvitationStyle.outputKey()).thenReturn("styled");
    when(meetingInvitationStyle.name()).thenReturn("styled");

    AtomicReference<String> request = new AtomicReference<>();

    HumanInTheLoop humanInTheLoop =
        AgenticServices.humanInTheLoopBuilder()
            .description(
                "What level of formality would you like? (please reply with “formal”, “casual”, or “friendly”)")
            .inputKey("style")
            .outputKey("style")
            .requestWriter(
                q ->
                    request.set(
                        "What level of formality would you like? (please reply with “formal”, “casual”, or “friendly”)"))
            .responseReader(() -> "formal")
            .build();

    Workflow workflow =
        AgentWorkflowBuilder.workflow("meetingInvitationFlow")
            .tasks(
                d ->
                    d.sequence(
                        "draft", meetingInvitationDraft, humanInTheLoop, meetingInvitationStyle))
            .build();
    Map<String, String> initialValues = new HashMap<>();
    initialValues.put("title", "Meeting with John Doe");
    initialValues.put("date", "2023-10-01");
    initialValues.put("time", "08:00AM");
    initialValues.put("location", "London");
    initialValues.put("agenda", "Discuss project updates");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      AgenticScope result =
          app.workflowDefinition(workflow)
              .instance(initialValues)
              .start()
              .get()
              .as(AgenticScope.class)
              .orElseThrow();

      assertEquals("Styled meeting invitation for John Doe", result.readState("styled"));
    }
  }
}
