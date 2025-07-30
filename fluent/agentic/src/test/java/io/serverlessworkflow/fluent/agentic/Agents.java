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

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.internal.AgentInstance;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.List;

public interface Agents {

  interface MovieExpert {

    @UserMessage(
        """
                                You are a great evening planner.
                                Propose a list of 3 movies matching the given mood.
                                The mood is {mood}.
                                Provide a list with the 3 items and nothing else.
                                """)
    @Agent
    List<String> findMovie(@V("mood") String mood);
  }

  interface SettingAgent extends AgentInstance {

    @UserMessage(
        """
              Create a vivid  {{style}} setting. It should include the time period, the state of technology,
              key locations, and a brief description of the world’s political or social situation.
              Make it imaginative, atmospheric, and suitable for a {{style}}  novel.
            """)
    @Agent(
        "Generates an imaginative setting including timeline, technology level, and world structure")
    String invoke(@V("style") String style);
  }

  interface HeroAgent extends AgentInstance {

    @UserMessage(
        """
              Invent a compelling protagonist for a {{style}} story. Describe their background, personality,
              motivations, and any unique skills or traits.
            """)
    @Agent("Creates a unique and relatable protagonist with rich backstory and motivations.")
    String invoke(@V("style") String style);
  }

  interface ConflictAgent extends AgentInstance {

    @UserMessage(
        """
                  Generate a central conflict or threat for a {{style}} plot. It can be external or
                  internal (e.g. moral dilemma, personal transformation).
                  Make it high-stakes and thematically rich.
                """)
    @Agent("Proposes a central conflict or dramatic tension to drive a compelling narrative.")
    String invoke(@V("style") String style);
  }

  interface FactAgent extends AgentInstance {

    @UserMessage(
        """
                  Generate a unique sci-fi fact about an alien civilization's {{goal}} environment or evolutionary history. Make it imaginative and specific.
            """)
    @Agent("Generates a core fact that defines the foundation of an civilization.")
    String invoke(@V("fact") String fact);
  }

  interface CultureAgent extends AgentInstance {

    @UserMessage(
        """
                  Given the following sci-fi fact about an civilization, describe 3–5 unique cultural traits, traditions, or societal structures that naturally emerge from this environment.
                  Fact:
                  {{fact}}
            """)
    @Agent("Derives cultural traits from the environmental/evolutionary fact.")
    List<String> invoke(@V("fact") String fact);
  }

  interface TechnologyAgent extends AgentInstance {

    @UserMessage(
        """
                  Given the following sci-fi fact about an alien civilization, describe 3–5 technologies or engineering solutions they might have developed. Focus on tools, transportation, communication, and survival systems.
                  Fact:
                  {{fact}}
            """)
    @Agent("Derives plausible technological inventions from the fact.")
    List<String> invoke(@V("fact") String fact);
  }

  interface StorySeedAgent extends AgentInstance {

    @UserMessage(
        """
                  You are a science fiction writer. Given the following title, come up with a short story premise. Describe the world, the central concept, and the thematic direction (e.g., dystopia, exploration, AI ethics).
                  Title: {{title}}
            """)
    @Agent("Generates a high-level sci-fi premise based on a title.")
    String invoke(@V("title") String title);
  }

  interface PlotAgent extends AgentInstance {

    @UserMessage(
        """
                  Using the following premise, outline a three-act structure for a science fiction short story. Include a brief description of the main character, the inciting incident, the rising conflict, and the resolution.
                  Premise:
                  {{premise}}
                """)
    @Agent("Transforms a premise into a structured sci-fi plot.")
    String invoke(@V("premise") String premise);
  }

  interface SceneAgent extends AgentInstance {

    @UserMessage(
        """
                  Write the opening scene of a science fiction short story based on the following plot outline. Introduce the main character and immerse the reader in the setting. Use vivid, cinematic language.
                  Plot:
                  {{plot}}
                """)
    @Agent("Generates the opening scene of the story from a plot outline.")
    String invoke(@V("plot") String plot);
  }
}
