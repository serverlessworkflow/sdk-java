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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.cognisphere.Cognisphere;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.OutputAsFunction;
import io.serverlessworkflow.fluent.agentic.AgentsUtils;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SequentialAgentServiceImplTest {
  @Test
  void shouldBuildEmptyWorkflow_byDefault() {
    // given
    SequentialAgentServiceImpl<DummyAgent> service =
        SequentialAgentServiceImpl.builder(DummyAgent.class);

    // when
    Workflow wf = service.getDefinition();

    // then
    assertNotNull(wf, "Workflow definition should not be null");
    assertTrue(wf.getDo().isEmpty(), "There should be no tasks by default");
  }

  @Test
  void shouldApplyBeforeCallConsumer_toInput() {
    // given
    SequentialAgentServiceImpl<DummyAgent> service =
        (SequentialAgentServiceImpl<DummyAgent>)
            SequentialAgentServiceImpl.builder(DummyAgent.class)
                .beforeCall(ctx -> ctx.writeState("foo", "bar"));

    // when
    Workflow wf = service.getDefinition();

    // then
    assertNotNull(wf.getInput(), "Input should not be null");
  }

  @Test
  void shouldSetOutputName_inDocumentMetadata() {
    // given
    String outputName = "myOutputName";
    SequentialAgentServiceImpl<DummyAgent> service =
        (SequentialAgentServiceImpl<DummyAgent>)
            SequentialAgentServiceImpl.builder(DummyAgent.class).outputName(outputName);

    // when
    Workflow wf = service.getDefinition();

    // then
    assertNotNull(wf.getDocument().getName(), "Workflow name should not be null");
    assertNotNull(wf.getOutput().getAs(), "Workflow outputAs should not be null");
    assertInstanceOf(OutputAsFunction.class, wf.getOutput().getAs());
  }

  @Test
  void shouldSetOutputFunction_extension() {
    // given
    Function<Cognisphere, Object> fn = ctx -> 42;
    SequentialAgentServiceImpl<DummyAgent> service =
        (SequentialAgentServiceImpl<DummyAgent>)
            SequentialAgentServiceImpl.builder(DummyAgent.class).output(fn);

    // when
    Workflow wf = service.getDefinition();

    // then
    assertNotNull(wf.getOutput(), "Output should not be null");
  }

  @Test
  void shouldBuildSequenceTasks_withSubAgents() {
    // given
    Object agentA = AgentsUtils.newMovieExpert();
    Object agentB = AgentsUtils.newMovieExpert();
    SequentialAgentServiceImpl<DummyAgent> service =
        (SequentialAgentServiceImpl<DummyAgent>)
            SequentialAgentServiceImpl.builder(DummyAgent.class).subAgents(agentA, agentB);

    // when
    Workflow wf = service.getDefinition();

    // then
    assertEquals(2, wf.getDo().size(), "There should be exactly two tasks");

    wf.getDo()
        .forEach(
            t -> {
              assertNotNull(t, "Task should not be null");
              Object task = t.getTask().getCallTask().get();
              assertInstanceOf(
                  CallJava.CallJavaFunction.class, task, "Task should be a CallTaskJava");
            });
  }

  static class DummyAgent {}
}
