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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import io.serverlessworkflow.impl.lifecycle.ce.AbstractLifeCyclePublisher;
import io.serverlessworkflow.impl.lifecycle.ce.TaskCancelledCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskCompletedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskFailedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskResumedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskStartedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskSuspendedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowCancelledCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowCompletedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowFailedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowResumedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowStartedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowSuspendedCEData;
import java.io.UncheckedIOException;

public class JacksonLifeCyclePublisher extends AbstractLifeCyclePublisher {

  @Override
  protected byte[] convert(WorkflowStartedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(WorkflowCompletedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(TaskStartedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(TaskCompletedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(TaskFailedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(WorkflowFailedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(WorkflowSuspendedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(WorkflowResumedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(WorkflowCancelledCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(TaskSuspendedCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(TaskCancelledCEData data) {
    return genericConvert(data);
  }

  @Override
  protected byte[] convert(TaskResumedCEData data) {
    return genericConvert(data);
  }

  protected <T> byte[] genericConvert(T data) {
    try {
      return JsonUtils.mapper().writeValueAsBytes(data);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
