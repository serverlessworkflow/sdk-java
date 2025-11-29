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
import static org.mockito.Mockito.spy;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.service.V;

public final class AgentsUtils {

  private AgentsUtils() {}

  public static Agents.MovieExpert newMovieExpert() {
    return spy(
        AgenticServices.agentBuilder(Agents.MovieExpert.class)
            .outputKey("movies")
            .chatModel(BASE_MODEL)
            .build());
  }

  public static Agents.MovieExpert newMovieExpert2() {
    return spy(
        AgenticServices.agentBuilder(Agents.MovieExpert.class)
            .outputKey("movies")
            .name("findMovie2")
            .chatModel(BASE_MODEL)
            .build());
  }

  public static Agents.CreativeWriter newCreativeWriter() {
    return spy(
        AgenticServices.agentBuilder(Agents.CreativeWriter.class)
            .outputKey("story")
            .chatModel(BASE_MODEL)
            .build());
  }

  public static Agents.AudienceEditor newAudienceEditor() {
    return spy(
        AgenticServices.agentBuilder(Agents.AudienceEditor.class)
            .outputKey("story")
            .chatModel(BASE_MODEL)
            .build());
  }

  public static Agents.StyleEditor newStyleEditor() {
    return spy(
        AgenticServices.agentBuilder(Agents.StyleEditor.class)
            .outputKey("story")
            .chatModel(BASE_MODEL)
            .build());
  }

  public static Agents.SummaryStory newSummaryStory() {
    return spy(
        AgenticServices.agentBuilder(Agents.SummaryStory.class)
            .outputKey("story")
            .chatModel(BASE_MODEL)
            .build());
  }

  public static Agents.StyleScorer newStyleScorer() {
    return spy(
        AgenticServices.agentBuilder(Agents.StyleScorer.class)
            .outputKey("score")
            .chatModel(BASE_MODEL)
            .build());
  }

  public static Agents.FoodExpert newFoodExpert() {
    return spy(
        AgenticServices.agentBuilder(Agents.FoodExpert.class)
            .chatModel(BASE_MODEL)
            .outputKey("meals")
            .build());
  }

  public static Agents.AstrologyAgent newAstrologyAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.AstrologyAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("horoscope")
            .build());
  }

  public static Agents.CategoryRouter newCategoryRouter() {
    return spy(
        AgenticServices.agentBuilder(Agents.CategoryRouter.class)
            .chatModel(BASE_MODEL)
            .outputKey("category")
            .build());
  }

  public static Agents.MedicalExpert newMedicalExpert() {
    return spy(
        AgenticServices.agentBuilder(Agents.MedicalExpert.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.TechnicalExpert newTechnicalExpert() {
    return spy(
        AgenticServices.agentBuilder(Agents.TechnicalExpert.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.LegalExpert newLegalExpert() {
    return spy(
        AgenticServices.agentBuilder(Agents.LegalExpert.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public interface NovelCreator {

    @Agent
    String createNovel(
        @V("topic") String topic, @V("audience") String audience, @V("style") String style);
  }

  public static Agents.StorySeedAgent newStorySeedAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.StorySeedAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.PlotAgent newPlotAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.PlotAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.SceneAgent newSceneAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.SceneAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.SettingAgent newSettingAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.SettingAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.HeroAgent newHeroAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.HeroAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.ConflictAgent newConflictAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.ConflictAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.FactAgent newFactAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.FactAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.CultureAgent newCultureAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.CultureAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }

  public static Agents.TechnologyAgent newTechnologyAgent() {
    return spy(
        AgenticServices.agentBuilder(Agents.TechnologyAgent.class)
            .chatModel(BASE_MODEL)
            .outputKey("response")
            .build());
  }
}
