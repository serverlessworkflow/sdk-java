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
package io.serverlessworkflow.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.events.OnEvents;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.interfaces.WorkflowValidator;
import io.serverlessworkflow.api.states.*;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.switchconditions.EventCondition;
import io.serverlessworkflow.api.validation.ValidationError;
import io.serverlessworkflow.api.validation.WorkflowSchemaLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowValidatorImpl implements WorkflowValidator {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowValidatorImpl.class);
  private boolean schemaValidationEnabled = true;
  private List<ValidationError> validationErrors = new ArrayList<>();
  private Schema workflowSchema = WorkflowSchemaLoader.getWorkflowSchema();
  private String source;
  private Workflow workflow;

  @Override
  public WorkflowValidator setWorkflow(Workflow workflow) {
    this.workflow = workflow;
    return this;
  }

  @Override
  public WorkflowValidator setSource(String source) {
    this.source = source;
    return this;
  }

  @Override
  public List<ValidationError> validate() {
    validationErrors.clear();
    if (workflow == null) {
      try {
        if (schemaValidationEnabled && source != null) {
          try {
            if (!source.trim().startsWith("{")) {
              // convert yaml to json to validate
              ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
              Object obj = yamlReader.readValue(source, Object.class);

              ObjectMapper jsonWriter = new ObjectMapper();

              workflowSchema.validate(new JSONObject(jsonWriter.writeValueAsString(obj)));
            } else {
              workflowSchema.validate(new JSONObject(source));
            }
          } catch (ValidationException e) {
            e.getCausingExceptions().stream()
                .map(ValidationException::getMessage)
                .forEach(
                    m -> {
                      if ((!m.equals("#/functions: expected type: JSONObject, found: JSONArray")
                          && !m.equals("#/events: expected type: JSONObject, found: JSONArray")
                          && !m.equals("#/start: expected type: JSONObject, found: String")
                          && !m.equals("#/retries: expected type: JSONObject, found: JSONArray"))) {
                        addValidationError(m, ValidationError.SCHEMA_VALIDATION);
                      }
                    });
          }
        }
      } catch (Exception e) {
        logger.error("Schema validation exception: " + e.getMessage());
      }
    }

    // if there are schema validation errors
    // there is no point of doing the workflow validation
    if (validationErrors.size() > 0) {
      return validationErrors;
    } else {
      if (workflow == null) {
        workflow = Workflow.fromSource(source);
      }

      List<FunctionDefinition> functions =
          workflow.getFunctions() != null ? workflow.getFunctions().getFunctionDefs() : null;

      List<EventDefinition> events =
          workflow.getEvents() != null ? workflow.getEvents().getEventDefs() : null;

      if (workflow.getId() == null || workflow.getId().trim().isEmpty()) {
        addValidationError("Workflow id should not be empty", ValidationError.WORKFLOW_VALIDATION);
      }

      if (workflow.getName() == null || workflow.getName().trim().isEmpty()) {
        addValidationError(
            "Workflow name should not be empty", ValidationError.WORKFLOW_VALIDATION);
      }

      if (workflow.getStart() == null) {
        addValidationError(
            "Workflow must define a starting state", ValidationError.WORKFLOW_VALIDATION);
      }

      if (workflow.getVersion() == null || workflow.getVersion().trim().isEmpty()) {
        addValidationError(
            "Workflow version should not be empty", ValidationError.WORKFLOW_VALIDATION);
      }

      if (workflow.getStates() == null || workflow.getStates().isEmpty()) {
        addValidationError("No states found", ValidationError.WORKFLOW_VALIDATION);
      }

      if (workflow.getStates() != null && !workflow.getStates().isEmpty()) {
        boolean existingStateWithStartProperty = false;
        String startProperty = workflow.getStart().getStateName();
        for (State s : workflow.getStates()) {
          if (s.getName().equals(startProperty)) {
            existingStateWithStartProperty = true;
            break;
          }
        }
        if (!existingStateWithStartProperty) {
          addValidationError(
              "No state name found that matches the workflow start definition",
              ValidationError.WORKFLOW_VALIDATION);
        }
      }

      Validation validation = new Validation();
      if (workflow.getStates() != null && !workflow.getStates().isEmpty()) {
        workflow
            .getStates()
            .forEach(
                s -> {
                  if (s.getName() != null && s.getName().trim().isEmpty()) {
                    addValidationError(
                        "State name should not be empty", ValidationError.WORKFLOW_VALIDATION);
                  } else {
                    validation.addState(s.getName());
                  }

                  if (s.getEnd() != null) {
                    validation.addEndState();
                  }

                  if (s instanceof OperationState) {
                    OperationState operationState = (OperationState) s;

                    List<Action> actions = operationState.getActions();
                    for (Action action : actions) {
                      if (action.getFunctionRef() != null) {
                        if (action.getFunctionRef().getRefName().isEmpty()) {
                          addValidationError(
                              "Operation State action functionRef should not be null or empty",
                              ValidationError.WORKFLOW_VALIDATION);
                        }

                        if (!haveFunctionDefinition(
                            action.getFunctionRef().getRefName(), functions)) {
                          addValidationError(
                              "Operation State action functionRef does not reference an existing workflow function definition",
                              ValidationError.WORKFLOW_VALIDATION);
                        }
                      }

                      if (action.getEventRef() != null) {
                        if (action.getEventRef().getTriggerEventRef().isEmpty()) {
                          addValidationError(
                              "Operation State action trigger eventRef does not reference an existing workflow event definition",
                              ValidationError.WORKFLOW_VALIDATION);
                        }

                        if (action.getEventRef().getResultEventRef().isEmpty()) {
                          addValidationError(
                              "Operation State action results eventRef does not reference an existing workflow event definition",
                              ValidationError.WORKFLOW_VALIDATION);
                        }

                        if (!haveEventsDefinition(
                            action.getEventRef().getTriggerEventRef(), events)) {
                          addValidationError(
                              "Operation State action trigger event def does not reference an existing workflow event definition",
                              ValidationError.WORKFLOW_VALIDATION);
                        }

                        if (!haveEventsDefinition(
                            action.getEventRef().getResultEventRef(), events)) {
                          addValidationError(
                              "Operation State action results event def does not reference an existing workflow event definition",
                              ValidationError.WORKFLOW_VALIDATION);
                        }
                      }
                    }
                  }

                  if (s instanceof EventState) {
                    EventState eventState = (EventState) s;
                    if (eventState.getOnEvents() == null || eventState.getOnEvents().size() < 1) {
                      addValidationError(
                          "Event State has no eventActions defined",
                          ValidationError.WORKFLOW_VALIDATION);
                    }
                    List<OnEvents> eventsActionsList = eventState.getOnEvents();
                    for (OnEvents onEvents : eventsActionsList) {

                      List<String> eventRefs = onEvents.getEventRefs();
                      if (eventRefs == null || eventRefs.size() < 1) {
                        addValidationError(
                            "Event State eventsActions has no event refs",
                            ValidationError.WORKFLOW_VALIDATION);
                      } else {
                        for (String eventRef : eventRefs) {
                          if (!haveEventsDefinition(eventRef, events)) {
                            addValidationError(
                                "Event State eventsActions eventRef does not match a declared workflow event definition",
                                ValidationError.WORKFLOW_VALIDATION);
                          }
                        }
                      }
                    }
                  }

                  if (s instanceof SwitchState) {
                    SwitchState switchState = (SwitchState) s;
                    if ((switchState.getDataConditions() == null
                            || switchState.getDataConditions().size() < 1)
                        && (switchState.getEventConditions() == null
                            || switchState.getEventConditions().size() < 1)) {
                      addValidationError(
                          "Switch state should define either data or event conditions",
                          ValidationError.WORKFLOW_VALIDATION);
                    }

                    if (switchState.getDefaultCondition() == null) {
                      addValidationError(
                          "Switch state should define a default transition",
                          ValidationError.WORKFLOW_VALIDATION);
                    }

                    if (switchState.getEventConditions() != null
                        && switchState.getEventConditions().size() > 0) {
                      List<EventCondition> eventConditions = switchState.getEventConditions();
                      for (EventCondition ec : eventConditions) {
                        if (!haveEventsDefinition(ec.getEventRef(), events)) {
                          addValidationError(
                              "Switch state event condition eventRef does not reference a defined workflow event",
                              ValidationError.WORKFLOW_VALIDATION);
                        }
                        if (ec.getEnd() != null) {
                          validation.addEndState();
                        }
                      }
                    }

                    if (switchState.getDataConditions() != null
                        && switchState.getDataConditions().size() > 0) {
                      List<DataCondition> dataConditions = switchState.getDataConditions();
                      for (DataCondition dc : dataConditions) {
                        if (dc.getEnd() != null) {
                          validation.addEndState();
                        }
                      }
                    }
                  }

                  if (s instanceof SleepState) {
                    SleepState sleepState = (SleepState) s;
                    if (sleepState.getDuration() == null || sleepState.getDuration().length() < 1) {
                      addValidationError(
                          "Sleep state should have a non-empty time delay",
                          ValidationError.WORKFLOW_VALIDATION);
                    }
                  }

                  if (s instanceof ParallelState) {
                    ParallelState parallelState = (ParallelState) s;

                    if (parallelState.getBranches() == null
                        || parallelState.getBranches().size() < 2) {
                      addValidationError(
                          "Parallel state should have at lest two branches",
                          ValidationError.WORKFLOW_VALIDATION);
                    }
                  }

                  if (s instanceof InjectState) {
                    InjectState injectState = (InjectState) s;
                    if (injectState.getData() == null || injectState.getData().isEmpty()) {
                      addValidationError(
                          "InjectState should have non-null data",
                          ValidationError.WORKFLOW_VALIDATION);
                    }
                  }

                  if (s instanceof ForEachState) {
                    ForEachState forEachState = (ForEachState) s;
                    if (forEachState.getInputCollection() == null
                        || forEachState.getInputCollection().isEmpty()) {
                      addValidationError(
                          "ForEach state should have a valid inputCollection",
                          ValidationError.WORKFLOW_VALIDATION);
                    }

                    if (forEachState.getIterationParam() == null
                        || forEachState.getIterationParam().isEmpty()) {
                      addValidationError(
                          "ForEach state should have a valid iteration parameter",
                          ValidationError.WORKFLOW_VALIDATION);
                    }
                  }

                  if (s instanceof CallbackState) {
                    CallbackState callbackState = (CallbackState) s;

                    if (!haveEventsDefinition(callbackState.getEventRef(), events)) {
                      addValidationError(
                          "CallbackState event ref does not reference a defined workflow event definition",
                          ValidationError.WORKFLOW_VALIDATION);
                    }

                    if (haveFunctionDefinition(
                        callbackState.getAction().getFunctionRef().getRefName(), functions)) {
                      addValidationError(
                          "CallbackState action function ref does not reference a defined workflow function definition",
                          ValidationError.WORKFLOW_VALIDATION);
                    }
                  }
                });

        if (validation.endStates == 0) {
          addValidationError("No end state found.", ValidationError.WORKFLOW_VALIDATION);
        }
      }

      return validationErrors;
    }
  }

  @Override
  public boolean isValid() {
    return validate().size() < 1;
  }

  @Override
  public WorkflowValidator setSchemaValidationEnabled(boolean schemaValidationEnabled) {
    this.schemaValidationEnabled = schemaValidationEnabled;
    return this;
  }

  @Override
  public WorkflowValidator reset() {
    workflow = null;
    validationErrors.clear();
    schemaValidationEnabled = true;
    return this;
  }

  private boolean haveFunctionDefinition(String functionName, List<FunctionDefinition> functions) {
    if (functions != null) {
      FunctionDefinition fun =
          functions.stream().filter(f -> f.getName().equals(functionName)).findFirst().orElse(null);

      return fun == null ? false : true;
    } else {
      return false;
    }
  }

  private boolean haveEventsDefinition(String eventName, List<EventDefinition> events) {
    if (events != null) {
      EventDefinition eve =
          events.stream().filter(e -> e.getName().equals(eventName)).findFirst().orElse(null);

      return eve == null ? false : true;
    } else {
      return false;
    }
  }

  private void addValidationError(String message, String type) {
    ValidationError mainError = new ValidationError();
    mainError.setMessage(message);
    mainError.setType(type);
    validationErrors.add(mainError);
  }

  private class Validation {

    final Set<String> events = new HashSet<>();
    final Set<String> functions = new HashSet<>();
    final Set<String> states = new HashSet<>();
    Integer endStates = 0;

    void addFunction(String name) {
      if (functions.contains(name)) {
        addValidationError(
            "Function does not have an unique name: " + name, ValidationError.WORKFLOW_VALIDATION);
      } else {
        functions.add(name);
      }
    }

    void addEvent(String name) {
      if (events.contains(name)) {
        addValidationError(
            "Event does not have an unique name: " + name, ValidationError.WORKFLOW_VALIDATION);
      } else {
        events.add(name);
      }
    }

    void addState(String name) {
      if (states.contains(name)) {
        addValidationError(
            "State does not have an unique name: " + name, ValidationError.WORKFLOW_VALIDATION);
      } else {
        states.add(name);
      }
    }

    void addEndState() {
      endStates++;
    }
  }
}
