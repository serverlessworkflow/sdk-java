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
package io.serverlessworkflow.impl;

import de.huxhorn.sulky.ulid.ULID;
import java.security.SecureRandom;

/**
 * A {@link WorkflowInstanceIdFactory} implementation that generates Monotonic ULIDs as workflow
 * instance IDs.
 */
public class MonotonicUlidWorkflowInstanceIdFactory implements WorkflowInstanceIdFactory {

  private final SecureRandom random = new SecureRandom();
  private final ULID ulid = new ULID(random);
  private ULID.Value previousUlid;

  @Override
  public synchronized String get() {
    if (previousUlid == null) {
      previousUlid = ulid.nextValue();
    } else {
      previousUlid = ulid.nextMonotonicValue(previousUlid);
    }
    return previousUlid.toString();
  }
}
