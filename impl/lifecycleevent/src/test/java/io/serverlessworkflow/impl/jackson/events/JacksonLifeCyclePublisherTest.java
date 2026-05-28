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
package io.serverlessworkflow.impl.jackson.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.cloudevents.core.data.PojoCloudEventData;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowDefinitionId;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import io.serverlessworkflow.impl.lifecycle.TaskCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskResumedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskRetriedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskSuspendedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowResumedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStatusEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowSuspendedEvent;
import io.serverlessworkflow.impl.lifecycle.ce.TaskCancelledCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskCompletedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskCompletedCEDataWOutput;
import io.serverlessworkflow.impl.lifecycle.ce.TaskFailedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskResumedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskRetriedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskStartedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskStartedCEDataWInput;
import io.serverlessworkflow.impl.lifecycle.ce.TaskSuspendedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowCancelledCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowCompletedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowCompletedCEDataWOutput;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowFailedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowResumedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowStartedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowStartedCEDataWInput;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowStatusCEDataEvent;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowSuspendedCEData;
import io.serverlessworkflow.impl.model.jackson.JacksonModelFactory;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JacksonLifeCyclePublisherTest {

  private static JacksonLifeCyclePublisher publisher;
  private static WorkflowContext workflowContext;
  private static TaskContext taskContext;
  private static WorkflowModelFactory factory;

  @BeforeAll
  static void setup() {
    publisher = new JacksonLifeCyclePublisher();
    factory = new JacksonModelFactory();
    workflowContext = mock(WorkflowContext.class);
    taskContext = mock(TaskContext.class);
    WorkflowInstance instanceData = mock(WorkflowInstance.class);
    WorkflowDefinition definition = mock(WorkflowDefinition.class);
    when(workflowContext.instanceData()).thenReturn(instanceData);
    when(instanceData.id()).thenReturn("1");
    when(instanceData.input()).thenReturn(factory.fromAny(Map.of("name", "sensei")));
    when(workflowContext.definition()).thenReturn(definition);
    WorkflowPosition position = mock(WorkflowPosition.class);
    when(definition.id()).thenReturn(new WorkflowDefinitionId("test", "events", "1_0"));
    when(taskContext.position()).thenReturn(position);
    when(taskContext.output()).thenReturn(factory.fromAny(Map.of("name", "Fulanito")));
    when(taskContext.input()).thenReturn(factory.fromAny(Map.of("name", "Menganito")));
    when(position.jsonPointer()).thenReturn("do/0/set/javi");
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void testCloudEventSerialization(Object pojo) throws IOException {
    PojoCloudEventData<?> source = PojoCloudEventData.wrap(pojo, publisher::convertToBytes);
    PojoCloudEventData<?> target =
        PojoCloudEventData.wrap(
            JsonUtils.mapper().readValue(source.toBytes(), pojo.getClass()),
            publisher::convertToBytes);
    assertThat(source).isEqualTo(target);
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(new TaskCompletedCEData(new TaskCompletedEvent(workflowContext, taskContext))),
        Arguments.of(
            new TaskCompletedCEDataWOutput(new TaskCompletedEvent(workflowContext, taskContext))),
        Arguments.of(new TaskStartedCEData(new TaskStartedEvent(workflowContext, taskContext))),
        Arguments.of(
            new TaskStartedCEDataWInput(new TaskStartedEvent(workflowContext, taskContext))),
        Arguments.of(new TaskCancelledCEData(new TaskCancelledEvent(workflowContext, taskContext))),
        Arguments.of(new TaskResumedCEData(new TaskResumedEvent(workflowContext, taskContext))),
        Arguments.of(new TaskRetriedCEData(new TaskRetriedEvent(workflowContext, taskContext))),
        Arguments.of(new TaskSuspendedCEData(new TaskSuspendedEvent(workflowContext, taskContext))),
        Arguments.of(
            new TaskFailedCEData(
                new TaskFailedEvent(
                    workflowContext, taskContext, new IllegalArgumentException("NOOOO!!!!")))),
        Arguments.of(new WorkflowStartedCEData(new WorkflowStartedEvent(workflowContext))),
        Arguments.of(new WorkflowStartedCEDataWInput(new WorkflowStartedEvent(workflowContext))),
        Arguments.of(
            new WorkflowCompletedCEData(new WorkflowCompletedEvent(workflowContext, null))),
        Arguments.of(
            new WorkflowCompletedCEDataWOutput(
                new WorkflowCompletedEvent(
                    workflowContext, factory.fromAny(Map.of("name", "Javierito"))))),
        Arguments.of(new WorkflowCancelledCEData(new WorkflowCancelledEvent(workflowContext))),
        Arguments.of(
            new WorkflowFailedCEData(
                new WorkflowFailedEvent(
                    workflowContext, new IllegalArgumentException("NOOO!!!!!")))),
        Arguments.of(new WorkflowResumedCEData(new WorkflowResumedEvent(workflowContext))),
        Arguments.of(new WorkflowSuspendedCEData(new WorkflowSuspendedEvent(workflowContext))),
        Arguments.of(
            new WorkflowStatusCEDataEvent(
                new WorkflowStatusEvent(
                    workflowContext, WorkflowStatus.RUNNING, WorkflowStatus.WAITING))));
  }
}
