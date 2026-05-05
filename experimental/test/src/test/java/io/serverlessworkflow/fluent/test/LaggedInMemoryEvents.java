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
package io.serverlessworkflow.fluent.test;

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.impl.events.InMemoryEvents;
import java.util.concurrent.CompletableFuture;

public class LaggedInMemoryEvents extends InMemoryEvents {

  @Override
  public CompletableFuture<Void> publish(CloudEvent ce) {

    return super.publish(ce)
        .thenRun(
            () -> {
              try {
                Thread.sleep(50);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });
  }
}
