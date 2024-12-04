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
package io.serverlessworkflow.impl.executors;

import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.WaitTask;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitExecutor extends AbstractTaskExecutor<WaitTask> {

  private static Logger logger = LoggerFactory.getLogger(WaitExecutor.class);
  private final Duration millisToWait;

  protected WaitExecutor(WaitTask task, WorkflowDefinition definition) {
    super(task, definition);
    this.millisToWait =
        task.getWait().getDurationInline() != null
            ? toLong(task.getWait().getDurationInline())
            : Duration.parse(task.getWait().getDurationExpression());
  }

  private Duration toLong(DurationInline durationInline) {
    Duration duration = Duration.ofMillis(durationInline.getMilliseconds());
    duration.plus(Duration.ofSeconds(durationInline.getSeconds()));
    duration.plus(Duration.ofMinutes(durationInline.getMinutes()));
    duration.plus(Duration.ofHours(durationInline.getHours()));
    duration.plus(Duration.ofDays(durationInline.getDays()));
    return duration;
  }

  @Override
  protected void internalExecute(WorkflowContext workflow, TaskContext<WaitTask> taskContext) {
    try {
      Thread.sleep(millisToWait.toMillis());
    } catch (InterruptedException e) {
      logger.warn("Waiting thread was interrupted", e);
      Thread.currentThread().interrupt();
    }
  }
}
