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
package io.serverlessworkflow.impl.lifecycle.ce;

import static io.serverlessworkflow.impl.lifecycle.ce.WorkflowDefinitionCEData.ref;
import static io.serverlessworkflow.impl.lifecycle.ce.WorkflowErrorCEData.error;

import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.core.data.PojoCloudEventData.ToBytes;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.events.CloudEventUtils;
import io.serverlessworkflow.impl.events.EventPublisher;
import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskEvent;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import java.time.OffsetDateTime;

public abstract class AbstractLifeCyclePublisher implements WorkflowExecutionListener {

  @Override
  public void onTaskStarted(TaskStartedEvent ev) {
    eventPublisher(ev)
        .publish(
            builder()
                .withData(
                    cloudEventData(
                        new TaskStartedCEData(id(ev), pos(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType("io.serverlessworkflow.task.started.v1")
                .build());
  }

  @Override
  public void onTaskCompleted(TaskCompletedEvent ev) {
    eventPublisher(ev)
        .publish(
            builder()
                .withData(
                    cloudEventData(
                        new TaskCompletedCEData(
                            id(ev), pos(ev), ref(ev), ev.eventDate(), output(ev)),
                        this::convert))
                .withType("io.serverlessworkflow.task.completed.v1")
                .build());
  }

  @Override
  public void onTaskFailed(TaskFailedEvent ev) {
    eventPublisher(ev)
        .publish(
            builder()
                .withData(
                    cloudEventData(
                        new TaskFailedCEData(id(ev), pos(ev), ref(ev), ev.eventDate(), error(ev)),
                        this::convert))
                .withType("io.serverlessworkflow.task.faulted.v1")
                .build());
  }

  @Override
  public void onWorkflowStarted(WorkflowStartedEvent ev) {
    eventPublisher(ev)
        .publish(
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowStartedCEData(id(ev), ref(ev), ev.eventDate()), this::convert))
                .withType("io.serverlessworkflow.workflow.started.v1")
                .build());
  }

  @Override
  public void onWorkflowCompleted(WorkflowCompletedEvent ev) {
    eventPublisher(ev)
        .publish(
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowCompletedCEData(id(ev), ref(ev), ev.eventDate(), output(ev)),
                        this::convert))
                .withType("io.serverlessworkflow.workflow.completed.v1")
                .build());
  }

  @Override
  public void onWorkflowFailed(WorkflowFailedEvent ev) {
    eventPublisher(ev)
        .publish(
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowFailedCEData(id(ev), ref(ev), ev.eventDate(), error(ev)),
                        this::convert))
                .withType("io.serverlessworkflow.workflow.faulted.v1")
                .build());
  }

  protected abstract byte[] convert(WorkflowStartedCEData data);

  protected abstract byte[] convert(WorkflowCompletedCEData data);

  protected abstract byte[] convert(TaskStartedCEData data);

  protected abstract byte[] convert(TaskCompletedCEData data);

  protected abstract byte[] convert(TaskFailedCEData data);

  protected abstract byte[] convert(WorkflowFailedCEData data);

  private static <T> CloudEventData cloudEventData(T data, ToBytes<T> toBytes) {
    return PojoCloudEventData.wrap(data, toBytes);
  }

  private static CloudEventBuilder builder() {
    return CloudEventBuilder.v1()
        .withId(CloudEventUtils.id())
        .withSource(CloudEventUtils.source())
        .withTime(OffsetDateTime.now());
  }

  private static String id(WorkflowEvent ev) {
    return ev.workflowContext().instanceData().id();
  }

  private static String pos(TaskEvent ev) {
    return ev.taskContext().position().jsonPointer();
  }

  private static Object output(WorkflowEvent ev) {
    return from(ev.workflowContext().instanceData().output());
  }

  private static EventPublisher eventPublisher(WorkflowEvent ev) {
    return ev.workflowContext().definition().application().eventPublisher();
  }

  private static Object output(TaskEvent ev) {
    return from(ev.taskContext().output());
  }

  private static Object from(WorkflowModel model) {
    return model.asJavaObject();
  }
}
