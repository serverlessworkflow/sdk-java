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
import io.serverlessworkflow.impl.WorkflowApplication;
import java.time.Duration;

public class ConstantRetryIntervalFunction extends AbstractRetryIntervalFunction {

  public ConstantRetryIntervalFunction(
      WorkflowApplication application, TimeoutAfter delay, RetryPolicyJitter jitter) {
    super(application, delay, jitter);
  }

  @Override
  protected Duration calcDelay(Duration delay, short numAttempts) {
    return delay;
  }
}
