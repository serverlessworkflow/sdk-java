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

import static io.serverlessworkflow.fluent.agentic.Models.BASE_MODEL;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;

import dev.langchain4j.agentic.AgentServices;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import org.junit.jupiter.api.Test;

public class AgenticWorkflowBuilderTest {

  @Test
  public void verifyAgentCall() {
    Agents.MovieExpert movieExpert =
        spy(
            AgentServices.agentBuilder(Agents.MovieExpert.class)
                .outputName("movies")
                .chatModel(BASE_MODEL)
                .build());

    Workflow workflow =
        AgenticWorkflowBuilder.workflow()
            .tasks(tasks -> tasks.agent("myAgent", movieExpert))
            .build();

    assertNotNull(workflow);
    assertInstanceOf(CallJava.class, workflow.getDo().get(0).getTask().getCallTask().get());
  }
}
