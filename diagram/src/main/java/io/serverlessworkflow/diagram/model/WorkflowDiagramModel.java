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
package io.serverlessworkflow.diagram.model;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.*;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.switchconditions.EventCondition;
import io.serverlessworkflow.diagram.utils.WorkflowDiagramUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorkflowDiagramModel {
    private Workflow workflow;

    private String title;
    private String legend;
    private String footer;
    private List<ModelStateDef> modelStateDefs = new ArrayList<>();
    private List<ModelState> modelStates = new ArrayList<>();
    private List<ModelConnection> modelConnections = new ArrayList<>();
    private boolean showLegend;

    public WorkflowDiagramModel(Workflow workflow, boolean showLegend) {
        this.workflow = workflow;
        this.showLegend = showLegend;
        inspect(workflow);
    }

    private void inspect(Workflow workflow) {
        // title
        setTitle(workflow.getName());
        if (workflow.getVersion() != null && workflow.getVersion().trim().length() > 0) {
            StringBuilder titleBuf = new StringBuilder()
                    .append(workflow.getName())
                    .append(WorkflowDiagramUtils.versionSeparator)
                    .append(workflow.getVersion());
            setTitle(titleBuf.toString());
        }

        // legend
        if (workflow.getDescription() != null && workflow.getDescription().trim().length() > 0) {
            StringBuilder legendBuff = new StringBuilder()
                    .append(WorkflowDiagramUtils.legendStart)
                    .append(workflow.getDescription())
                    .append(WorkflowDiagramUtils.legendEnd);
            setLegend(legendBuff.toString());
        } else {
            setLegend("");
        }

        // footer
        setFooter(WorkflowDiagramUtils.footer);

        // state definitions
        inspectStateDefinitions(workflow);

        // states info
        inspectStatesInfo(workflow);

        // states connections
        inspectStatesConnections(workflow);

    }

    private void inspectStateDefinitions(Workflow workflow) {
        for (State state : workflow.getStates()) {
            modelStateDefs.add(new ModelStateDef(state.getName(), state.getType().value()));
        }
    }

    private void inspectStatesConnections(Workflow workflow) {
        State workflowStartState = WorkflowDiagramUtils.getWorkflowStartState(workflow);
        modelConnections.add(new ModelConnection(WorkflowDiagramUtils.wfStart, workflowStartState.getName(), ""));

        List<State> workflowStates = workflow.getStates();
        for (State state : workflowStates) {
            if (state instanceof SwitchState) {
                SwitchState switchState = (SwitchState) state;
                if (switchState.getDataConditions() != null && switchState.getDataConditions().size() > 0) {
                    for (DataCondition dataCondition : switchState.getDataConditions()) {

                        if (dataCondition.getTransition() != null) {
                            if (dataCondition.getTransition().getProduceEvents() != null && dataCondition.getTransition().getProduceEvents().size() > 0) {
                                List<String> producedEvents = dataCondition.getTransition().getProduceEvents().stream()
                                        .map(t -> t.getEventRef())
                                        .collect(Collectors.toList());

                                String desc = "";
                                if (dataCondition.getName() != null && dataCondition.getName().trim().length() > 0) {
                                    desc = dataCondition.getName();
                                }
                                desc += " Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                                modelConnections.add(new ModelConnection(switchState.getName(), dataCondition.getTransition().getNextState(), desc));
                            } else {
                                String desc = "";
                                if (dataCondition.getName() != null && dataCondition.getName().trim().length() > 0) {
                                    desc = dataCondition.getName();
                                }
                                modelConnections.add(new ModelConnection(switchState.getName(), dataCondition.getTransition().getNextState(), desc));
                            }
                        }

                        if (dataCondition.getEnd() != null) {
                            if (dataCondition.getEnd().getProduceEvents() != null && dataCondition.getEnd().getProduceEvents().size() > 0) {
                                List<String> producedEvents = dataCondition.getEnd().getProduceEvents().stream()
                                        .map(t -> t.getEventRef())
                                        .collect(Collectors.toList());

                                String desc = "";
                                if (dataCondition.getName() != null && dataCondition.getName().trim().length() > 0) {
                                    desc = dataCondition.getName();
                                }
                                desc += " Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                                modelConnections.add(new ModelConnection(switchState.getName(), WorkflowDiagramUtils.wfEnd, desc));
                            } else {
                                String desc = "";
                                if (dataCondition.getName() != null && dataCondition.getName().trim().length() > 0) {
                                    desc = dataCondition.getName();
                                }
                                modelConnections.add(new ModelConnection(switchState.getName(), WorkflowDiagramUtils.wfEnd, desc));
                            }
                        }

                    }
                }

                if (switchState.getEventConditions() != null && switchState.getEventConditions().size() > 0) {
                    for (EventCondition eventCondition : switchState.getEventConditions()) {

                        if (eventCondition.getTransition() != null) {
                            if (eventCondition.getTransition().getProduceEvents() != null && eventCondition.getTransition().getProduceEvents().size() > 0) {
                                List<String> producedEvents = eventCondition.getTransition().getProduceEvents().stream()
                                        .map(t -> t.getEventRef())
                                        .collect(Collectors.toList());

                                String desc = "";
                                if (eventCondition.getName() != null && eventCondition.getName().trim().length() > 0) {
                                    desc = eventCondition.getName();
                                }
                                desc += " Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                                modelConnections.add(new ModelConnection(switchState.getName(), eventCondition.getTransition().getNextState(), desc));
                            } else {
                                String desc = "";
                                if (eventCondition.getName() != null && eventCondition.getName().trim().length() > 0) {
                                    desc = eventCondition.getName();
                                }
                                modelConnections.add(new ModelConnection(switchState.getName(), eventCondition.getTransition().getNextState(), desc));
                            }
                        }

                        if (eventCondition.getEnd() != null) {
                            if (eventCondition.getEnd().getProduceEvents() != null && eventCondition.getEnd().getProduceEvents().size() > 0) {
                                List<String> producedEvents = eventCondition.getEnd().getProduceEvents().stream()
                                        .map(t -> t.getEventRef())
                                        .collect(Collectors.toList());

                                String desc = "";
                                if (eventCondition.getName() != null && eventCondition.getName().trim().length() > 0) {
                                    desc = eventCondition.getName();
                                }
                                desc += " Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                                modelConnections.add(new ModelConnection(switchState.getName(), WorkflowDiagramUtils.wfEnd, desc));
                            } else {
                                String desc = "";
                                if (eventCondition.getName() != null && eventCondition.getName().trim().length() > 0) {
                                    desc = eventCondition.getName();
                                }
                                modelConnections.add(new ModelConnection(switchState.getName(), WorkflowDiagramUtils.wfEnd, desc));
                            }
                        }

                    }
                }

                // default
                if (switchState.getDefaultCondition() != null) {
                    if (switchState.getDefaultCondition().getTransition() != null) {
                        if (switchState.getDefaultCondition().getTransition().getProduceEvents() != null && switchState.getDefaultCondition().getTransition().getProduceEvents().size() > 0) {
                            List<String> producedEvents = switchState.getDefaultCondition().getTransition().getProduceEvents().stream()
                                    .map(t -> t.getEventRef())
                                    .collect(Collectors.toList());

                            String desc = "default - ";
                            desc += " Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                            modelConnections.add(new ModelConnection(switchState.getName(), switchState.getDefaultCondition().getTransition().getNextState(), desc));
                        } else {
                            String desc = "default";
                            modelConnections.add(new ModelConnection(switchState.getName(), switchState.getDefaultCondition().getTransition().getNextState(), desc));
                        }
                    }

                    if (switchState.getDefaultCondition().getEnd() != null) {
                        if (switchState.getDefaultCondition().getEnd().getProduceEvents() != null && switchState.getDefaultCondition().getEnd().getProduceEvents().size() > 0) {
                            List<String> producedEvents = switchState.getDefaultCondition().getEnd().getProduceEvents().stream()
                                    .map(t -> t.getEventRef())
                                    .collect(Collectors.toList());

                            String desc = "default - ";
                            desc += " Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                            modelConnections.add(new ModelConnection(switchState.getName(), WorkflowDiagramUtils.wfEnd, desc));
                        } else {
                            String desc = "default";
                            modelConnections.add(new ModelConnection(switchState.getName(), WorkflowDiagramUtils.wfEnd, desc));
                        }
                    }
                }
            } else {
                if (state.getTransition() != null) {
                    if (state.getTransition().getProduceEvents() != null && state.getTransition().getProduceEvents().size() > 0) {
                        List<String> producedEvents = state.getTransition().getProduceEvents().stream()
                                .map(t -> t.getEventRef())
                                .collect(Collectors.toList());

                        String desc = "Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                        modelConnections.add(new ModelConnection(state.getName(), state.getTransition().getNextState(), desc));
                    } else {
                        modelConnections.add(new ModelConnection(state.getName(), state.getTransition().getNextState(), ""));
                    }
                }

                if (state.getEnd() != null) {
                    if (state.getEnd().getProduceEvents() != null && state.getEnd().getProduceEvents().size() > 0) {
                        List<String> producedEvents = state.getEnd().getProduceEvents().stream()
                                .map(t -> t.getEventRef())
                                .collect(Collectors.toList());

                        String desc = "Produced Events: " + producedEvents.stream().collect(Collectors.joining(","));
                        modelConnections.add(new ModelConnection(state.getName(), WorkflowDiagramUtils.wfEnd, desc));
                    } else {
                        modelConnections.add(new ModelConnection(state.getName(), WorkflowDiagramUtils.wfEnd, ""));
                    }
                }
            }
        }
    }

    private void inspectStatesInfo(Workflow workflow) {
        List<State> workflowStates = workflow.getStates();
        for (State state : workflowStates) {
            ModelState modelState = new ModelState(state.getName());

            if (state instanceof EventState) {
                EventState eventState = (EventState) state;

                List<String> events = eventState.getOnEvents().stream()
                        .flatMap(t -> t.getEventRefs().stream())
                        .collect(Collectors.toList());

                modelState.addInfo("Type: Event State");
                modelState.addInfo("Events: " + events.stream().collect(Collectors.joining(" ")));

            }

            if (state instanceof OperationState) {
                OperationState operationState = (OperationState) state;

                modelState.addInfo("Type: Operation State");
                modelState.addInfo("Action mode: " + Optional.ofNullable(operationState.getActionMode()).orElse(OperationState.ActionMode.SEQUENTIAL));
                modelState.addInfo("Num. of actions: " + Optional.ofNullable(operationState.getActions().size()).orElse(0));
            }

            if (state instanceof SwitchState) {
                SwitchState switchState = (SwitchState) state;


                modelState.addInfo("Type: Switch State");
                if (switchState.getDataConditions() != null && switchState.getDataConditions().size() > 0) {
                    modelState.addInfo("Condition type: data-based");
                    modelState.addInfo("Num. of conditions: " + switchState.getDataConditions().size());
                }

                if (switchState.getEventConditions() != null && switchState.getEventConditions().size() > 0) {
                    modelState.addInfo("Condition type: event-based");
                    modelState.addInfo("Num. of conditions: " + switchState.getEventConditions().size());
                }

                if (switchState.getDefaultCondition() != null) {
                    if (switchState.getDefaultCondition().getTransition() != null) {
                        modelState.addInfo("Default to: " + switchState.getDefaultCondition().getTransition().getNextState());
                    }

                    if (switchState.getDefaultCondition().getEnd() != null) {
                        modelState.addInfo("Default to: End");
                    }
                }

            }

            if (state instanceof DelayState) {
                DelayState delayState = (DelayState) state;

                modelState.addInfo("Type: Delay State");
                modelState.addInfo("Delay: " + delayState.getTimeDelay());
            }

            if (state instanceof ParallelState) {
                ParallelState parallelState = (ParallelState) state;

                modelState.addInfo("Type: Parallel State");
                modelState.addInfo("Completion type: \"" + parallelState.getCompletionType().value() + "\"");
                modelState.addInfo("Num. of branches: " + parallelState.getBranches().size());
            }

            if (state instanceof InjectState) {
                modelState.addInfo("Type: Inject State");
            }

            if (state instanceof ForEachState) {
                ForEachState forEachState = (ForEachState) state;

                modelState.addInfo("Type: ForEach State");
                modelState.addInfo("Input collection: " + forEachState.getInputCollection());
                if (forEachState.getActions() != null && forEachState.getActions().size() > 0) {
                    modelState.addInfo("Num. of actions: " + forEachState.getActions().size());
                }

                if (forEachState.getWorkflowId() != null && forEachState.getWorkflowId().length() > 0) {
                    modelState.addInfo("Workflow ID: " + forEachState.getWorkflowId());
                }
            }

            if (state instanceof CallbackState) {
                CallbackState callbackState = (CallbackState) state;

                modelState.addInfo("Type: Callback State");
                modelState.addInfo("Callback function: " + callbackState.getAction().getFunctionRef().getRefName());
                modelState.addInfo("Callback event: " + callbackState.getEventRef());
            }

            modelStates.add(modelState);
        }
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLegend() {
        return legend;
    }

    public void setLegend(String legend) {
        this.legend = legend;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public List<ModelState> getModelStates() {
        return modelStates;
    }

    public void setModelStates(List<ModelState> modelStates) {
        this.modelStates = modelStates;
    }

    public List<ModelConnection> getModelConnections() {
        return modelConnections;
    }

    public void setModelConnections(List<ModelConnection> modelConnections) {
        this.modelConnections = modelConnections;
    }

    public List<ModelStateDef> getModelStateDefs() {
        return modelStateDefs;
    }

    public void setModelStateDefs(List<ModelStateDef> modelStateDefs) {
        this.modelStateDefs = modelStateDefs;
    }

    public boolean getShowLegend() {
        return showLegend;
    }
}
