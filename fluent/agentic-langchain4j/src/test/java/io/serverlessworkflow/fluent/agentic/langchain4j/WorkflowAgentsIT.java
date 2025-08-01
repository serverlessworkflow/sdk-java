package io.serverlessworkflow.fluent.agentic.langchain4j;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.langchain4j.agentic.AgentServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.WorkflowAgentsBuilder;

import static io.serverlessworkflow.fluent.agentic.langchain4j.Models.BASE_MODEL;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.CreativeWriter;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.AudienceEditor;
import static io.serverlessworkflow.fluent.agentic.langchain4j.Agents.StyleEditor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WorkflowAgentsIT {

    @Test
    void sequential_agents_tests() {
        WorkflowAgentsBuilder builder = new LC4JWorkflowBuilder();

        CreativeWriter creativeWriter = spy(AgentServices.agentBuilder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

        AudienceEditor audienceEditor = spy(AgentServices.agentBuilder(AudienceEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

        StyleEditor styleEditor = spy(AgentServices.agentBuilder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

        UntypedAgent novelCreator = builder.sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .outputName("story")
                .build();

        Map<String, Object> input = Map.of(
                "topic", "dragons and wizards",
                "style", "fantasy",
                "audience", "young adults"
        );

        String story = (String) novelCreator.invoke(input);
        System.out.println(story);

        verify(creativeWriter).generateStory("dragons and wizards");
        verify(audienceEditor).editStory(any(), eq("young adults"));
        verify(styleEditor).editStory(any(), eq("fantasy"));
    }
}
