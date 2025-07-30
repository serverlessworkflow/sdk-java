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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WorkflowTests {

  @Test
  public void testSequence() throws ExecutionException, InterruptedException {
    final StorySeedAgent storySeedAgent = mock(StorySeedAgent.class);
    final PlotAgent plotAgent = mock(PlotAgent.class);
    final SceneAgent sceneAgent = mock(SceneAgent.class);

    when(storySeedAgent.invoke(eq("A Great Story"))).thenReturn("storySeedAgent");
    when(storySeedAgent.outputName()).thenReturn("premise");

    when(plotAgent.invoke(eq("storySeedAgent"))).thenReturn("plotAgent");
    when(plotAgent.outputName()).thenReturn("plot");

    when(sceneAgent.invoke(eq("plotAgent"))).thenReturn("plotAgent");
    when(sceneAgent.outputName()).thenReturn("story");

    Workflow workflow =
        AgentWorkflowBuilder.workflow("storyFlow")
            .tasks(d -> d.sequence("story", storySeedAgent, plotAgent, sceneAgent))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("title", "A Great Story");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      String result =
          app.workflowDefinition(workflow).instance(topic).start().get().asText().orElseThrow();

      assertEquals("plotAgent", result);
    }
  }

  @Test
  public void testParallel() throws ExecutionException, InterruptedException {

    final SettingAgent setting = mock(SettingAgent.class);
    final HeroAgent hero = mock(HeroAgent.class);
    final ConflictAgent conflict = mock(ConflictAgent.class);

    when(setting.invoke(eq("sci-fi"))).thenReturn("Fake conflict response");
    when(setting.outputName()).thenReturn("setting");

    when(hero.invoke(eq("sci-fi"))).thenReturn("Fake hero response");
    when(hero.outputName()).thenReturn("hero");

    when(conflict.invoke(eq("sci-fi"))).thenReturn("Fake setting response");
    when(conflict.outputName()).thenReturn("conflict");

    Workflow workflow =
        AgentWorkflowBuilder.workflow("parallelFlow")
            .tasks(d -> d.parallel("story", setting, hero, conflict))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("style", "sci-fi");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Map<String, Object> result =
          app.workflowDefinition(workflow).instance(topic).start().get().asMap().orElseThrow();

      assertEquals(3, result.size());
      assertTrue(result.containsKey("branch-0-story"));
      assertTrue(result.containsKey("branch-1-story"));
      assertTrue(result.containsKey("branch-2-story"));

      Set<String> values =
          result.values().stream().map(Object::toString).collect(Collectors.toSet());

      assertTrue(values.contains("Fake conflict response"));
      assertTrue(values.contains("Fake hero response"));
      assertTrue(values.contains("Fake setting response"));
    }
  }

  @Test
  // TODO: callFn must be replace with a .output() method once it's available
  public void testSeqAndThenParallel() throws ExecutionException, InterruptedException {
    final FactAgent factAgent = mock(FactAgent.class);
    final CultureAgent cultureAgent = mock(CultureAgent.class);
    final TechnologyAgent technologyAgent = mock(TechnologyAgent.class);

    List<String> cultureTraits =
        List.of("Alien Culture Trait 1", "Alien Culture Trait 2", "Alien Culture Trait 3");

    List<String> technologyTraits =
        List.of("Alien Technology Trait 1", "Alien Technology Trait 2", "Alien Technology Trait 3");

    when(factAgent.invoke(eq("alien"))).thenReturn("Some Fact about aliens");
    when(factAgent.outputName()).thenReturn("fact");

    when(cultureAgent.invoke(eq("Some Fact about aliens"))).thenReturn(cultureTraits);
    when(cultureAgent.outputName()).thenReturn("culture");

    when(technologyAgent.invoke(eq("Some Fact about aliens"))).thenReturn(technologyTraits);
    when(technologyAgent.outputName()).thenReturn("technology");
    Workflow workflow =
        AgentWorkflowBuilder.workflow("alienCultureFlow")
            .tasks(
                d ->
                    d.sequence("fact", factAgent)
                        .callFn(
                            f ->
                                f.function(
                                    (Function<String, Map<String, String>>)
                                        fact -> {
                                          Map<String, String> result = new HashMap<>();
                                          result.put("fact", fact);
                                          return result;
                                        }))
                        .parallel("cultureAndTechnology", cultureAgent, technologyAgent))
            .build();

    Map<String, String> topic = new HashMap<>();
    topic.put("fact", "alien");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Map<String, Object> result =
          app.workflowDefinition(workflow).instance(topic).start().get().asMap().orElseThrow();

      assertEquals(2, result.size());
      assertTrue(result.containsKey("branch-0-cultureAndTechnology"));
      assertTrue(result.containsKey("branch-1-cultureAndTechnology"));

      assertEquals(cultureTraits, result.get("branch-0-cultureAndTechnology"));
      assertEquals(technologyTraits, result.get("branch-1-cultureAndTechnology"));
    }
  }
}
