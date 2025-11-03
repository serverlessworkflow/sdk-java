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

import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.AudienceEditor;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.CreativeWriter;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.StyleEditor;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Models.BASE_MODEL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.WorkflowAgentsBuilder;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class WorkflowAgentsIT {

  @Test
  void sequential_agents_tests() {
    WorkflowAgentsBuilder builder = new LC4JWorkflowBuilder();

    CreativeWriter creativeWriter =
        spy(
            AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputKey("story")
                .build());

    AudienceEditor audienceEditor =
        spy(
            AgenticServices.agentBuilder(AudienceEditor.class)
                .chatModel(BASE_MODEL)
                .outputKey("story")
                .build());

    StyleEditor styleEditor =
        spy(
            AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputKey("story")
                .build());

    UntypedAgent novelCreator =
        builder
            .sequenceBuilder()
            .subAgents(creativeWriter, audienceEditor, styleEditor)
            .outputKey("story")
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
}
