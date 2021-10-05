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
package io.serverlessworkflow.utils;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.start.Start;
import io.serverlessworkflow.api.states.CallbackState;
import io.serverlessworkflow.api.states.DefaultState;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.SwitchState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Provides common utility methods to provide most often needed answers from a workflow */
public final class WorkflowUtils {
  private static final int DEFAULT_STARTING_STATE_POSITION = 0;

  /**
   * Gets State matching Start state.If start is not present returns first state otherwise returns
   * null
   *
   * @param workflow workflow
   * @return {@code state} when present else returns {@code null}
   */
  public static State getStartingState(Workflow workflow) {
    if (workflow == null || workflow.getStates() == null || workflow.getStates().isEmpty()) {
      return null;
    }

    Start start = workflow.getStart();
    if (start == null) {
      return workflow.getStates().get(DEFAULT_STARTING_STATE_POSITION);
    } else {
      return workflow.getStates().stream()
          .filter(state -> state.getName().equals(start.getStateName()))
          .findFirst()
          .get();
    }
  }

  /**
   * Gets List of States matching stateType
   *
   * @param workflow
   * @param stateType
   * @return {@code List<State>}. Returns {@code null} when workflow is null.
   */
  public static List<State> getStates(Workflow workflow, DefaultState.Type stateType) {
    if (workflow == null || workflow.getStates() == null) {
      return null;
    }

    return workflow.getStates().stream()
        .filter(state -> state.getType() == stateType)
        .collect(Collectors.toList());
  }

  /**
   * @return {@code List<io.serverlessworkflow.api.events.EventDefinition>}. Returns {@code NULL}
   *     when workflow is null or when workflow does not contain events
   */
  public static List<EventDefinition> getDefinedConsumedEvents(Workflow workflow) {
    return getDefinedEvents(workflow, EventDefinition.Kind.CONSUMED);
  }

  /**
   * @return {@code List<io.serverlessworkflow.api.events.EventDefinition>}. Returns {@code NULL}
   *     when workflow is null or when workflow does not contain events
   */
  public static List<EventDefinition> getDefinedProducedEvents(Workflow workflow) {
    return getDefinedEvents(workflow, EventDefinition.Kind.PRODUCED);
  }

  /**
   * Gets list of event definition matching eventKind
   *
   * @param workflow
   * @return {@code List<io.serverlessworkflow.api.events.EventDefinition>}. Returns {@code NULL}
   *     when workflow is null or when workflow does not contain events
   */
  public static List<EventDefinition> getDefinedEvents(
      Workflow workflow, EventDefinition.Kind eventKind) {
    if (workflow == null || workflow.getEvents() == null) {
      return null;
    }
    List<EventDefinition> eventDefs = workflow.getEvents().getEventDefs();
    if (eventDefs == null) {
      return null;
    }

    return eventDefs.stream()
        .filter(eventDef -> eventDef.getKind() == eventKind)
        .collect(Collectors.toList());
  }

  /** @return {@code int} Returns count of defined event count matching eventKind */
  public static int getDefinedEventsCount(Workflow workflow, EventDefinition.Kind eventKind) {
    List<EventDefinition> definedEvents = getDefinedEvents(workflow, eventKind);
    return definedEvents == null ? 0 : definedEvents.size();
  }

  /** @return {@code int} Returns count of Defined Consumed Event Count */
  public static int getDefinedConsumedEventsCount(Workflow workflow) {
    return getDefinedEventsCount(workflow, EventDefinition.Kind.CONSUMED);
  }

  /** @return {@code int} Returns count of Defined Produced Event Count */
  public static int getDefinedProducedEventsCount(Workflow workflow) {
    return getDefinedEventsCount(workflow, EventDefinition.Kind.PRODUCED);
  }

  /**
   * Gets Consumed Events of parent workflow Iterates through states in parent workflow and collects
   * all the ConsumedEvents. Sub Workflows of the Workflow <strong>are not</strong> considered for
   * getting Consumed Events
   *
   * @return Returns {@code List<EventDefinition>}
   */
  public static List<EventDefinition> getWorkflowConsumedEvents(Workflow workflow) {
    return getWorkflowEventDefinitions(workflow, EventDefinition.Kind.CONSUMED);
  }

  /**
   * Gets Produced Events of parent workflow Iterates through states in parent workflow and collects
   * all the Produced Events.
   *
   * @return Returns {@code List<EventDefinition>}
   */
  public static List<EventDefinition> getWorkflowProducedEvents(Workflow workflow) {
    if (workflow == null || workflow.getStates() == null || workflow.getStates().size() == 0) {
      return null;
    }
    List<EventDefinition> definedProducedEvents =
        getDefinedEvents(workflow, EventDefinition.Kind.PRODUCED);
    Set<String> uniqueEvents = new HashSet<>();
    for (State state : workflow.getStates()) {
      End end = state.getEnd();
      if (end == null) continue;
      if (end.getProduceEvents() != null || end.getProduceEvents().size() != 0) {
        end.getProduceEvents()
            .forEach(produceEvent -> uniqueEvents.add(produceEvent.getEventRef()));
      }
    }
    return definedProducedEvents.stream()
        .filter(eventDefinition -> uniqueEvents.contains(eventDefinition.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Gets Events of parent workflow matching {@code EventDefinition.Kind} Iterates through states in
   * parent workflow and collects all the events matching {@code EventDefinition.Kind} .
   *
   * @return Returns {@code List<EventDefinition>}
   */
  private static List<EventDefinition> getWorkflowEventDefinitions(
      Workflow workflow, EventDefinition.Kind eventKind) {
    if (workflow == null || workflow.getStates() == null || workflow.getStates().size() == 0) {
      return null;
    }
    List<EventDefinition> definedConsumedEvents = getDefinedEvents(workflow, eventKind);
    if (definedConsumedEvents == null) return null;
    Set<String> uniqEventReferences = new HashSet<>();
    List<String> eventReferencesFromState = getWorkflowConsumedEventsFromState(workflow);
    uniqEventReferences.addAll(eventReferencesFromState);
    return definedConsumedEvents.stream()
        .filter(x -> uniqEventReferences.contains(x.getName()))
        .collect(Collectors.toList());
  }

  private static List<String> getWorkflowConsumedEventsFromState(Workflow workflow) {
    List<String> eventReferences = new ArrayList<>();
    for (State state : workflow.getStates()) {
      if (state instanceof SwitchState) {
        SwitchState switchState = (SwitchState) state;
        if (switchState.getEventConditions() != null) {
          switchState
              .getEventConditions()
              .forEach(eventCondition -> eventReferences.add(eventCondition.getEventRef()));
        }
      } else if (state instanceof CallbackState) {
        CallbackState callbackState = (CallbackState) state;
        if (callbackState.getEventRef() != null) eventReferences.add(callbackState.getEventRef());
      } else if (state instanceof EventState) {
        EventState eventState = (EventState) state;
        if (eventState.getOnEvents() != null) {
          eventState
              .getOnEvents()
              .forEach(onEvents -> eventReferences.addAll(onEvents.getEventRefs()));
        }
      }
    }
    return eventReferences;
  }

  /**
   * @return Returns {@code int } Count of the workflow consumed events. <strong>Does not</strong>
   *     consider sub-workflows
   */
  public static int getWorkflowConsumedEventsCount(Workflow workflow) {
    List<EventDefinition> workflowConsumedEvents = getWorkflowConsumedEvents(workflow);
    return workflowConsumedEvents == null ? 0 : workflowConsumedEvents.size();
  }

  /**
   * @return Returns {@code int} Count of the workflow produced events. <strong>Does not</strong>
   *     consider sub-workflows in the count
   */
  public static int getWorkflowProducedEventsCount(Workflow workflow) {
    List<EventDefinition> workflowProducedEvents = getWorkflowProducedEvents(workflow);
    return workflowProducedEvents == null ? 0 : workflowProducedEvents.size();
  }
}
