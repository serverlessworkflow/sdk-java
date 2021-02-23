/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.serverlessworkflow.api.test;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.defaultdef.DefaultDefinition;
import io.serverlessworkflow.api.exectimeout.ExecTimeout;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.functions.FunctionRef;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.SubflowState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.test.utils.WorkflowTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MarkupToWorkflowTest {

    @ParameterizedTest
    @ValueSource(strings = {"/examples/applicantrequest.json", "/examples/applicantrequest.yml",
            "/examples/carauctionbids.json", "/examples/carauctionbids.yml",
            "/examples/creditcheck.json", "/examples/creditcheck.yml",
            "/examples/eventbasedgreeting.json", "/examples/eventbasedgreeting.yml",
            "/examples/finalizecollegeapplication.json", "/examples/finalizecollegeapplication.yml",
            "/examples/greeting.json", "/examples/greeting.yml",
            "/examples/helloworld.json", "/examples/helloworld.yml",
            "/examples/jobmonitoring.json", "/examples/jobmonitoring.yml",
            "/examples/monitorpatient.json", "/examples/monitorpatient.yml",
            "/examples/parallel.json", "/examples/parallel.yml",
            "/examples/provisionorder.json", "/examples/provisionorder.yml",
            "/examples/sendcloudevent.json", "/examples/sendcloudevent.yml",
            "/examples/solvemathproblems.json", "/examples/solvemathproblems.yml",
            "/examples/foreachstatewithactions.json", "/examples/foreachstatewithactions.yml",
            "/examples/periodicinboxcheck.json", "/examples/periodicinboxcheck.yml",
            "/examples/vetappointmentservice.json", "/examples/vetappointmentservice.yml",
            "/examples/eventbasedtransition.json", "/examples/eventbasedtransition.yml",
            "/examples/roomreadings.json", "/examples/roomreadings.yml",
            "/examples/checkcarvitals.json", "/examples/checkcarvitals.yml"
    })
    public void testSpecExamplesParsing(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/applicantrequest.json", "/features/applicantrequest.yml"})
    public void testSpecFreatureFunctionRef(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);

        assertNotNull(workflow.getFunctions());
        assertTrue(workflow.getFunctions().getFunctionDefs().size() == 1);

        assertNotNull(workflow.getRetries());
        assertTrue(workflow.getRetries().getRetryDefs().size() == 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/vetappointment.json", "/features/vetappointment.yml"})
    public void testSpecFreatureEventRef(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);

        assertNotNull(workflow.getEvents());
        assertTrue(workflow.getEvents().getEventDefs().size() == 2);

        assertNotNull(workflow.getRetries());
        assertTrue(workflow.getRetries().getRetryDefs().size() == 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/compensationworkflow.json", "/features/compensationworkflow.yml"})
    public void testSpecFreatureCompensation(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() == 2);

        State firstState = workflow.getStates().get(0);
        assertTrue(firstState instanceof EventState);
        assertNotNull(firstState.getCompensatedBy());
        assertEquals("CancelPurchase", firstState.getCompensatedBy());

        State secondState = workflow.getStates().get(1);
        assertTrue(secondState instanceof OperationState);
        OperationState operationState = (OperationState) secondState;

        assertTrue(operationState.isUsedForCompensation());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/functiontypes.json", "/features/functiontypes.yml"})
    public void testFunctionTypes(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() == 1);

        State state = workflow.getStates().get(0);
        assertTrue(state instanceof OperationState);

        List<FunctionDefinition> functionDefs = workflow.getFunctions().getFunctionDefs();
        assertNotNull(functionDefs);
        assertTrue(functionDefs.size() == 2);

        FunctionDefinition restFunc = functionDefs.get(0);
        assertEquals(restFunc.getType(), FunctionDefinition.Type.REST);

        FunctionDefinition restFunc2 = functionDefs.get(1);
        assertEquals(restFunc2.getType(), FunctionDefinition.Type.EXPRESSION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/transitions.json", "/features/transitions.yml"})
    public void testTransitions(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() == 1);

        State state = workflow.getStates().get(0);
        assertTrue(state instanceof SwitchState);

        SwitchState switchState = (SwitchState) workflow.getStates().get(0);
        assertNotNull(switchState.getDataConditions());
        List<DataCondition> dataConditions = switchState.getDataConditions();

        assertEquals(2, dataConditions.size());

        DataCondition cond1 = switchState.getDataConditions().get(0);
        assertNotNull(cond1.getTransition());
        assertEquals("StartApplication", cond1.getTransition().getNextState());
        assertNotNull(cond1.getTransition().getProduceEvents());
        assertTrue(cond1.getTransition().getProduceEvents().isEmpty());
        assertFalse(cond1.getTransition().isCompensate());


        DataCondition cond2 = switchState.getDataConditions().get(1);
        assertNotNull(cond2.getTransition());
        assertEquals("RejectApplication", cond2.getTransition().getNextState());
        assertNotNull(cond2.getTransition().getProduceEvents());
        assertEquals(1, cond2.getTransition().getProduceEvents().size());
        assertFalse(cond2.getTransition().isCompensate());


        assertNotNull(switchState.getDefault());
        DefaultDefinition defaultDefinition = switchState.getDefault();
        assertNotNull(defaultDefinition.getTransition());
        assertEquals("RejectApplication", defaultDefinition.getTransition().getNextState());
        assertNotNull(defaultDefinition.getTransition().getProduceEvents());
        assertTrue(defaultDefinition.getTransition().getProduceEvents().isEmpty());
        assertTrue(defaultDefinition.getTransition().isCompensate());

    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/functionrefs.json", "/features/functionrefs.yml"})
    public void testFunctionRefs(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() == 1);

        State state = workflow.getStates().get(0);
        assertTrue(state instanceof OperationState);

        OperationState operationState = (OperationState) workflow.getStates().get(0);
        assertNotNull(operationState.getActions());
        assertEquals(2, operationState.getActions().size());

        Action action1 = operationState.getActions().get(0);
        assertNotNull(action1);
        assertNotNull(action1.getFunctionRef());
        FunctionRef functionRef1 = action1.getFunctionRef();
        assertEquals("creditCheckFunction", functionRef1.getRefName());
        assertEquals(0, functionRef1.getParameters().size());

        Action action2 = operationState.getActions().get(1);
        assertNotNull(action2);
        assertNotNull(action2.getFunctionRef());
        FunctionRef functionRef2 = action2.getFunctionRef();
        assertEquals("sendRejectionEmailFunction", functionRef2.getRefName());
        assertEquals(1, functionRef2.getParameters().size());
        assertEquals("{{ $.customer }}", functionRef2.getParameters().get("applicant").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/keepactiveexectimeout.json", "/features/keepactiveexectimeout.yml"})
    public void testKeepActiveExecTimeout(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertTrue(workflow.isKeepActive());
        assertNotNull(workflow.getExecTimeout());

        ExecTimeout execTimeout = workflow.getExecTimeout();
        assertEquals("PT1H", execTimeout.getInterval());
        assertEquals("GenerateReport", execTimeout.getRunBefore());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/checkcarvitals.json", "/features/checkcarvitals.yml"})
    public void testSubflowStateRepeat(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() == 2);

        State state = workflow.getStates().get(1);
        assertTrue(state instanceof SubflowState);

        SubflowState subflowState = (SubflowState) workflow.getStates().get(1);
        assertNotNull(subflowState.getRepeat());
        assertEquals(10, subflowState.getRepeat().getMax());
        assertTrue(subflowState.getRepeat().isContinueOnError());
        assertNotNull(subflowState.getRepeat().getStopOnEvents());
        assertEquals(1, subflowState.getRepeat().getStopOnEvents().size());
        assertEquals("CarTurnedOffEvent", subflowState.getRepeat().getStopOnEvents().get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/functionrefjsonparams.json", "/features/functionrefjsonparams.yml"})
    public void testFunctionRefJsonParams(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() == 1);
        assertTrue(workflow.getStates().get(0) instanceof OperationState);

        OperationState operationState = (OperationState) workflow.getStates().get(0);
        assertNotNull(operationState.getActions());
        assertEquals(1, operationState.getActions().size());
        List<Action> actions = operationState.getActions();
        assertNotNull(actions.get(0).getFunctionRef());
        assertEquals("addPet", actions.get(0).getFunctionRef().getRefName());

        JsonNode params = actions.get(0).getFunctionRef().getParameters();
        assertNotNull(params);
        assertEquals(4, params.size());
        assertEquals(123, params.get("id").intValue());
        assertEquals("My Address, 123 MyCity, MyCountry", params.get("address").asText());
        assertEquals("${ .owner.name }", params.get("owner").asText());
        assertEquals("Pluto", params.get("body").get("name").asText());
        assertEquals("${ .pet.tagnumber }", params.get("body").get("tag").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/functionrefnoparams.json", "/features/functionrefnoparams.yml"})
    public void testFunctionRefNoParams(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() == 1);
        assertTrue(workflow.getStates().get(0) instanceof OperationState);

        OperationState operationState = (OperationState) workflow.getStates().get(0);
        assertNotNull(operationState.getActions());
        assertEquals(2, operationState.getActions().size());
        List<Action> actions = operationState.getActions();
        assertNotNull(actions.get(0).getFunctionRef());
        assertNotNull(actions.get(1).getFunctionRef());
        assertEquals("addPet", actions.get(0).getFunctionRef().getRefName());
        assertEquals("addPet", actions.get(1).getFunctionRef().getRefName());

        JsonNode params = actions.get(0).getFunctionRef().getParameters();
        assertNull(params);
        JsonNode params2 = actions.get(1).getFunctionRef().getParameters();
        assertNull(params);
    }
}
