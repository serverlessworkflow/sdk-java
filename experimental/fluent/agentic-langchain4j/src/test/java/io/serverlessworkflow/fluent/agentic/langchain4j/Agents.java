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

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class Agents {

  public interface CreativeWriter {

    @UserMessage(
        """
                You are a creative writer.
                Generate a draft of a story long no more than 3 sentence around the given topic.
                Return only the story and nothing else.
                The topic is {{topic}}.
                """)
    @Agent("Generate a story based on the given topic")
    String generateStory(@V("topic") String topic);
  }

  public interface AudienceEditor {

    @UserMessage(
        """
            You are a professional editor.
            Analyze and rewrite the following story to better align with the target audience of {{audience}}.
            Return only the story and nothing else.
            The story is "{{story}}".
            """)
    @Agent("Edit a story to better fit a given audience")
    String editStory(@V("story") String story, @V("audience") String audience);
  }

  public interface StyleEditor {

    @UserMessage(
        """
                You are a professional editor.
                Analyze and rewrite the following story to better fit and be more coherent with the {{style}} style.
                Return only the story and nothing else.
                The story is "{{story}}".
                """)
    @Agent("Edit a story to better fit a given style")
    String editStory(@V("story") String story, @V("style") String style);
  }

}
