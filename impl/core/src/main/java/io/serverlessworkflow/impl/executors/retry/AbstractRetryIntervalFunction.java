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
package io.serverlessworkflow.impl.executors.retry;

import io.serverlessworkflow.api.types.RetryPolicyJitter;
import io.serverlessworkflow.api.types.TimeoutAfter;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.time.Duration;
import java.util.Optional;

public abstract class AbstractRetryIntervalFunction implements RetryIntervalFunction {

  private final Optional<WorkflowValueResolver<Duration>> minJitteringResolver;
  private final Optional<WorkflowValueResolver<Duration>> maxJitteringResolver;
  private final WorkflowValueResolver<Duration> delayResolver;

  public AbstractRetryIntervalFunction(
      WorkflowApplication appl, TimeoutAfter delay, RetryPolicyJitter jitter) {
    if (jitter != null) {
      minJitteringResolver = Optional.of(WorkflowUtils.fromTimeoutAfter(appl, jitter.getFrom()));
      maxJitteringResolver = Optional.of(WorkflowUtils.fromTimeoutAfter(appl, jitter.getTo()));
    } else {
      minJitteringResolver = Optional.empty();
      maxJitteringResolver = Optional.empty();
    }
    delayResolver = WorkflowUtils.fromTimeoutAfter(appl, delay);
  }

  @Override
  public Duration apply(
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel model,
      short numAttempts) {
    Duration delay = delayResolver.apply(workflowContext, taskContext, model);
    Duration minJittering =
        minJitteringResolver
            .map(min -> min.apply(workflowContext, taskContext, model))
            .orElse(Duration.ZERO);
    Duration maxJittering =
        maxJitteringResolver
            .map(max -> max.apply(workflowContext, taskContext, model))
            .orElse(Duration.ZERO);
    return calcDelay(delay, numAttempts)
        .plus(
            Duration.ofMillis(
                (long) (minJittering.toMillis() + Math.random() * maxJittering.toMillis())));
  }

  protected abstract Duration calcDelay(Duration delay, short numAttempts);
}
