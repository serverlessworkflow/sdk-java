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

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.core.data.PojoCloudEventData.ToBytes;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.events.CloudEventUtils;
import io.serverlessworkflow.impl.lifecycle.TaskCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskEvent;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskResumedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskRetriedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskSuspendedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowResumedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowSuspendedEvent;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractLifeCyclePublisher implements WorkflowExecutionListener {

  private static final String TASK_STARTED = "io.serverlessworkflow.task.started.v1";
  private static final String TASK_COMPLETED = "io.serverlessworkflow.task.completed.v1";
  private static final String TASK_SUSPENDED = "io.serverlessworkflow.task.suspended.v1";
  private static final String TASK_RESUMED = "io.serverlessworkflow.task.resumed.v1";
  private static final String TASK_FAULTED = "io.serverlessworkflow.task.faulted.v1";
  private static final String TASK_CANCELLED = "io.serverlessworkflow.task.cancelled.v1";
  private static final String TASK_RETRIED = "io.serverlessworkflow.task.retried.v1";

  private static final String WORKFLOW_STARTED = "io.serverlessworkflow.workflow.started.v1";
  private static final String WORKFLOW_COMPLETED = "io.serverlessworkflow.workflow.completed.v1";
  private static final String WORKFLOW_SUSPENDED = "io.serverlessworkflow.workflow.suspended.v1";
  private static final String WORKFLOW_RESUMED = "io.serverlessworkflow.workflow.resumed.v1";
  private static final String WORKFLOW_FAULTED = "io.serverlessworkflow.workflow.faulted.v1";
  private static final String WORKFLOW_CANCELLED = "io.serverlessworkflow.workflow.cancelled.v1";

  public static Collection<String> getLifeCycleTypes() {
    return Set.of(
        TASK_STARTED,
        TASK_COMPLETED,
        TASK_SUSPENDED,
        TASK_RESUMED,
        TASK_FAULTED,
        TASK_CANCELLED,
        TASK_RETRIED,
        WORKFLOW_STARTED,
        WORKFLOW_COMPLETED,
        WORKFLOW_SUSPENDED,
        WORKFLOW_RESUMED,
        WORKFLOW_FAULTED,
        WORKFLOW_CANCELLED);
  }

  @Override
  public void onTaskStarted(TaskStartedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new TaskStartedCEData(id(ev), pos(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType(TASK_STARTED)
                .build());
  }

  @Override
  public void onTaskRetried(TaskRetriedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new TaskRetriedCEData(id(ev), pos(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType(TASK_STARTED)
                .build());
  }

  @Override
  public void onTaskCompleted(TaskCompletedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new TaskCompletedCEData(
                            id(ev), pos(ev), ref(ev), ev.eventDate(), output(ev)),
                        this::convert))
                .withType(TASK_COMPLETED)
                .build());
  }

  @Override
  public void onTaskSuspended(TaskSuspendedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new TaskSuspendedCEData(id(ev), pos(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType(TASK_SUSPENDED)
                .build());
  }

  @Override
  public void onTaskResumed(TaskResumedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new TaskResumedCEData(id(ev), pos(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType(TASK_RESUMED)
                .build());
  }

  @Override
  public void onTaskCancelled(TaskCancelledEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new TaskCancelledCEData(id(ev), pos(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType(TASK_CANCELLED)
                .build());
  }

  @Override
  public void onTaskFailed(TaskFailedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new TaskFailedCEData(id(ev), pos(ev), ref(ev), ev.eventDate(), error(ev)),
                        this::convert))
                .withType(TASK_FAULTED)
                .build());
  }

  @Override
  public void onWorkflowStarted(WorkflowStartedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowStartedCEData(id(ev), ref(ev), ev.eventDate()), this::convert))
                .withType(WORKFLOW_STARTED)
                .build());
  }

  @Override
  public void onWorkflowSuspended(WorkflowSuspendedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowSuspendedCEData(id(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType(WORKFLOW_SUSPENDED)
                .build());
  }

  @Override
  public void onWorkflowCancelled(WorkflowCancelledEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowCancelledCEData(id(ev), ref(ev), ev.eventDate()),
                        this::convert))
                .withType(WORKFLOW_CANCELLED)
                .build());
  }

  @Override
  public void onWorkflowResumed(WorkflowResumedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowResumedCEData(id(ev), ref(ev), ev.eventDate()), this::convert))
                .withType(WORKFLOW_RESUMED)
                .build());
  }

  @Override
  public void onWorkflowCompleted(WorkflowCompletedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowCompletedCEData(
                            id(ev), ref(ev), ev.eventDate(), from(event.output())),
                        this::convert))
                .withType(WORKFLOW_COMPLETED)
                .build());
  }

  @Override
  public void onWorkflowFailed(WorkflowFailedEvent event) {
    publish(
        event,
        ev ->
            builder()
                .withData(
                    cloudEventData(
                        new WorkflowFailedCEData(id(ev), ref(ev), ev.eventDate(), error(ev)),
                        this::convert))
                .withType(WORKFLOW_FAULTED)
                .build());
  }

  protected byte[] convert(WorkflowStartedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(WorkflowCompletedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(TaskStartedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(TaskCompletedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(TaskFailedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(WorkflowFailedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(WorkflowSuspendedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(WorkflowResumedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(TaskRetriedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(WorkflowCancelledCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(TaskSuspendedCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(TaskCancelledCEData data) {
    return convertToBytes(data);
  }

  protected byte[] convert(TaskResumedCEData data) {
    return convertToBytes(data);
  }

  protected abstract <T> byte[] convertToBytes(T data);

  protected <T extends WorkflowEvent> void publish(T ev, Function<T, CloudEvent> ceFunction) {
    WorkflowApplication appl = ev.workflowContext().definition().application();
    if (appl.isLifeCycleCEPublishingEnabled()) {
      publish(appl, ceFunction.apply(ev));
    }
  }

  /* By default, generated cloud events are published, if user has not disabled them at application level,
   * using application event publishers. That might be changed if needed by children by overriding this method
   */
  protected void publish(WorkflowApplication application, CloudEvent ce) {
    application.eventPublishers().forEach(p -> p.publishLifeCycle(ce));
  }

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

  private static Object output(TaskEvent ev) {
    return from(ev.taskContext().output());
  }

  private static Object from(WorkflowModel model) {
    return model.asJavaObject();
  }
}
