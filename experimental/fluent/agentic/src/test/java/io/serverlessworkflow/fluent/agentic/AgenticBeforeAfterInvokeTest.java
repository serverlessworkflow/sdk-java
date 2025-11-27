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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class AgenticBeforeAfterInvokeTest {

  @Test
  void testAgentBeforeAfter() throws ExecutionException, InterruptedException {
    Agents.ChatBot chatBot = mock(Agents.ChatBot.class);
    when(chatBot.chat(eq("A Great Story"))).thenReturn("Once upon a time...");

    Workflow wf =
        AgentWorkflowBuilder.workflow()
            .tasks(
                t ->
                    t.agent(chatBot)
                        .inputFrom(
                            agenticScope -> {
                              assertTrue(agenticScope.state().containsKey("userInput"));
                              assertEquals("A Great Story", agenticScope.readState("userInput"));
                              agenticScope.writeState("testData", "someValue");
                            })
                        .outputAs(
                            agenticScope -> {
                              assertTrue(agenticScope.state().containsKey("userInput"));
                              assertEquals("A Great Story", agenticScope.readState("userInput"));
                              assertTrue(agenticScope.state().containsKey("testData"));
                              assertEquals("someValue", agenticScope.readState("testData"));
                              assertEquals("Once upon a time...", agenticScope.readState("input"));
                              agenticScope.writeState("outputData", "outputValue");
                            }))
            .build();

    Map<String, Object> in = new HashMap<>();
    in.put("userInput", "A Great Story");

    Map<String, Object> out;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      out = app.workflowDefinition(wf).instance(in).start().get().asMap().orElseThrow();
    }

    assertTrue(out.containsKey("outputData"));
    assertEquals("outputValue", out.get("outputData"));
    assertTrue(out.containsKey("userInput"));
    assertEquals("A Great Story", out.get("userInput"));
    assertTrue(out.containsKey("testData"));
    assertEquals("someValue", out.get("testData"));
    assertTrue(out.containsKey("input"));
    assertEquals("Once upon a time...", out.get("input"));
  }

  @Test
  void testSeqBeforeAfter() throws ExecutionException, InterruptedException {
    Agents.CreativeWriter creativeWriter = mock(Agents.CreativeWriter.class);
    when(creativeWriter.generateStory((eq("dragons and wizards"))))
        .thenReturn("A fantasy story...");

    Agents.StyleEditor styleEditor = mock(Agents.StyleEditor.class);
    when(styleEditor.editStory(eq("A fantasy story..."), eq("young adults")))
        .thenReturn("An engaging fantasy story for young adults...");

    Agents.StyleScorer styleScorer = mock(Agents.StyleScorer.class);
    when(styleScorer.scoreStyle(eq("An engaging fantasy story for young adults..."), eq("funny")))
        .thenReturn(8.0);

    Workflow wf =
        AgentWorkflowBuilder.workflow()
            .tasks(
                t ->
                    t.sequence(creativeWriter, styleEditor, styleScorer)
                        .inputFrom(
                            agenticScope -> {
                              if (agenticScope.readState("step") == null) {
                                agenticScope.writeState("topic", "dragons and wizards");
                                agenticScope.writeState("step", "out_1");
                              } else if (agenticScope.readState("step").equals("in_2")) {
                                agenticScope.writeState("story", agenticScope.readState("input"));
                                agenticScope.writeState("style", "young adults");
                                agenticScope.writeState("step", "out_2");
                              } else if (agenticScope.readState("step").equals("in_3")) {
                                agenticScope.writeState("story", agenticScope.readState("input"));
                                agenticScope.writeState("style", "funny");
                                agenticScope.writeState("step", "out_3");
                              } else {
                                throw new RuntimeException("We should not reach here");
                              }
                            })
                        .outputAs(
                            agenticScope -> {
                              if (agenticScope.readState("step").equals("out_1")) {
                                assertEquals("A fantasy story...", agenticScope.readState("input"));
                                agenticScope.writeState("step", "in_2");
                              } else if (agenticScope.readState("step").equals("out_2")) {
                                assertEquals(
                                    "An engaging fantasy story for young adults...",
                                    agenticScope.readState("input"));
                                agenticScope.writeState("step", "in_3");
                              } else if (agenticScope.readState("step").equals("out_3")) {
                                assertEquals(8.0, agenticScope.readState("input"));
                              } else {
                                throw new RuntimeException("We should not reach here");
                              }
                            }))
            .build();

    Map<String, Object> out;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      out = app.workflowDefinition(wf).instance(Map.of()).start().get().asMap().orElseThrow();
    }

    assertEquals(8.0, out.get("input"));
    assertEquals("An engaging fantasy story for young adults...", out.get("story"));
    assertEquals("funny", out.get("style"));
  }

  @Test
  void testParallelBeforeAfter() throws ExecutionException, InterruptedException {
    Agents.ChatBot chatBot = mock(Agents.ChatBot.class);
    when(chatBot.chat(eq("A Great Story"))).thenReturn("Once upon a time...");

    Agents.MovieExpert movieExpert = mock(Agents.MovieExpert.class);
    when(movieExpert.findMovie(eq("si-fi"))).thenReturn(List.of("Movie A", "Movie B"));

    Workflow wf =
        AgentWorkflowBuilder.workflow()
            .tasks(
                t ->
                    t.parallel(chatBot, movieExpert)
                        .inputFrom(
                            agenticScope -> {
                              agenticScope.writeState("userInput", "A Great Story");
                              agenticScope.writeState("mood", "si-fi");
                            }))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.workflowDefinition(wf).instance(Map.of()).start().get().asJavaObject();
    }
  }

  @Test
  void testLoopBeforeAfter() throws ExecutionException, InterruptedException {
    Agents.MovieExpert expert = mock(Agents.MovieExpert.class);
    when(expert.findMovie(eq("si-fi"))).thenReturn(List.of("Movie A", "Movie B"));
    AtomicInteger atomicInteger = new AtomicInteger(0);

    Workflow wf =
        AgentWorkflowBuilder.workflow()
            .tasks(
                d ->
                    d.loop(
                        l ->
                            l.subAgents(expert)
                                .inputFrom(a -> atomicInteger.incrementAndGet())
                                .outputAs(
                                    agenticScope ->
                                        agenticScope.writeState("count", atomicInteger.get()))
                                .exitCondition(exit -> ((Integer) exit.readState("count")) == 10)))
            .build();

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(wf)
              .instance(Map.of("mood", "si-fi", "count", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();
    }

    assertEquals(10, result.get("count"));
  }
}
