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
import dev.langchain4j.agentic.internal.AgentSpecification;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.List;

public interface Agents {

  interface ChatBot {
    @UserMessage(
        """
                        You are a happy chat bot, reply to my message:
                        {{userInput}}.
                        """)
    @Agent
    String chat(@V("userInput") String userInput);
  }

  interface MovieExpert {

    @UserMessage(
        """
                        You are a great evening planner.
                        Propose a list of 3 movies matching the given mood.
                        The mood is {{mood}}.
                        Provide a list with the 3 items and nothing else.
                        """)
    @Agent
    List<String> findMovie(@V("mood") String mood);
  }

  interface SettingAgent extends AgentSpecification {

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

  interface HeroAgent extends AgentSpecification {

    @UserMessage(
        """
                          Invent a compelling protagonist for a {{style}} story. Describe their background, personality,
                          motivations, and any unique skills or traits.
                        """)
    @Agent("Creates a unique and relatable protagonist with rich backstory and motivations.")
    String invoke(@V("style") String style);
  }

  interface ConflictAgent extends AgentSpecification {

    @UserMessage(
        """
                          Generate a central conflict or threat for a {{style}} plot. It can be external or
                          internal (e.g. moral dilemma, personal transformation).
                          Make it high-stakes and thematically rich.
                        """)
    @Agent("Proposes a central conflict or dramatic tension to drive a compelling narrative.")
    String invoke(@V("style") String style);
  }

  interface FactAgent extends AgentSpecification {

    @UserMessage(
        """
                              Generate a unique sci-fi fact about an alien civilization's {{goal}} environment or evolutionary history. Make it imaginative and specific.
                        """)
    @Agent("Generates a core fact that defines the foundation of an civilization.")
    String invoke(@V("fact") String fact);
  }

  interface CultureAgent extends AgentSpecification {

    @UserMessage(
        """
                              Given the following sci-fi fact about an civilization, describe 3–5 unique cultural traits, traditions, or societal structures that naturally emerge from this environment.
                              Fact:
                              {{fact}}
                        """)
    @Agent("Derives cultural traits from the environmental/evolutionary fact.")
    List<String> invoke(@V("fact") String fact);
  }

  interface TechnologyAgent extends AgentSpecification {

    @UserMessage(
        """
                              Given the following sci-fi fact about an alien civilization, describe 3–5 technologies or engineering solutions they might have developed. Focus on tools, transportation, communication, and survival systems.
                              Fact:
                              {{fact}}
                        """)
    @Agent("Derives plausible technological inventions from the fact.")
    List<String> invoke(@V("fact") String fact);
  }

  interface StorySeedAgent extends AgentSpecification {

    @UserMessage(
        """
                              You are a science fiction writer. Given the following title, come up with a short story premise. Describe the world, the central concept, and the thematic direction (e.g., dystopia, exploration, AI ethics).
                              Title: {{title}}
                        """)
    @Agent("Generates a high-level sci-fi premise based on a title.")
    String invoke(@V("title") String title);
  }

  interface PlotAgent extends AgentSpecification {

    @UserMessage(
        """
                          Using the following premise, outline a three-act structure for a science fiction short story. Include a brief description of the main character, the inciting incident, the rising conflict, and the resolution.
                          Premise:
                          {{premise}}
                        """)
    @Agent("Transforms a premise into a structured sci-fi plot.")
    String invoke(@V("premise") String premise);
  }

  interface SceneAgent extends AgentSpecification {

    @UserMessage(
        """
                          Write the opening scene of a science fiction short story based on the following plot outline. Introduce the main character and immerse the reader in the setting. Use vivid, cinematic language.
                          Plot:
                          {{plot}}
                        """)
    @Agent("Generates the opening scene of the story from a plot outline.")
    String invoke(@V("plot") String plot);
  }

  interface MeetingInvitationDraft extends AgentSpecification {

    @UserMessage(
        """
                          You are a professional meeting invitation writer. Draft a concise and clear meeting invitation email based on the following details:
                          Subject: {{subject}}
                          Date: {{date}}
                          Time: {{time}}
                          Location: {{location}}
                          Agenda: {{agenda}}
                        """)
    @Agent("Drafts a professional meeting invitation email.")
    String invoke(
        @V("subject") String subject,
        @V("date") String date,
        @V("time") String time,
        @V("location") String location,
        @V("agenda") String agenda);
  }

  interface MeetingInvitationStyle extends AgentSpecification {

    @UserMessage(
        """
                          You are a professional meeting invitation writer. Rewrite the following meeting invitation email to better fit the {{style}} style:
                          Original Invitation: {{invitation}}
                        """)
    @Agent("Edits a meeting invitation email to better fit a given style.")
    String invoke(@V("invitation") String invitation, @V("style") String style);
  }

  interface EmailDrafter {

    @UserMessage(
        """
                You are a precise email drafting assistant.

                GOAL
                - Draft a professional email that achieves the stated purpose.
                - Keep it concise and skimmable.

                INPUT
                recipient_name: {{recipientName}}
                sender_name: {{senderName}}
                purpose: {{purpose}}             // e.g., follow-up, scheduling, proposal, apology, onboarding
                key_points: {{keyPoints}}        // bullet list or comma-separated facts
                tone: {{tone}}                   // e.g., friendly, neutral, formal
                length: {{length}}               // short|medium
                call_to_action: {{cta}}          // e.g., "reply with a time", "confirm receipt", or empty
                signature: {{signature}}         // prebuilt block; do NOT invent
                allowed_domains: {{allowedDomains}} // e.g., ["acme.com","example.com"]
                known_links: {{links}}           // URLs you may use; if not in allowed_domains, do not include

                HARD RULES
                - Never fabricate facts, prices, or promises.
                - Only include links from allowed_domains and only those listed in known_links.
                - Do not include internal/confidential URLs.
                - If you lack a detail, write a neutral placeholder (e.g., "[DATE]").
                - Keep subject <= 60 characters if possible.
                - One clear CTA max.

                OUTPUT
                Return ONLY a compact JSON object with keys:
                {
                  "subject": "...",
                  "body_plain": "...",
                  "links": ["..."]      // subset of known_links, or empty
                }
                No markdown, no explanations, no extra text.
                """)
    @Agent("Drafts a new outbound email from structured inputs; returns JSON.")
    String draftNew(
        @V("recipientName") String recipientName,
        @V("senderName") String senderName,
        @V("purpose") String purpose,
        @V("keyPoints") List<String> keyPoints,
        @V("tone") String tone,
        @V("length") String length,
        @V("cta") String cta,
        @V("signature") String signature,
        @V("allowedDomains") List<String> allowedDomains,
        @V("links") List<String> links);
  }
}
