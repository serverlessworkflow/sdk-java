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
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaggedInMemoryEvents extends InMemoryEvents {

  private static final Logger logger = LoggerFactory.getLogger(InMemoryEvents.class);

  @Override
  public CompletableFuture<Void> publish(CloudEvent ce) {
    return CompletableFuture.runAsync(
        () -> {
          Consumer<CloudEvent> allConsumer = allConsumerRef.get();
          if (allConsumer != null) {
            allConsumer.accept(ce);
          }
          Consumer<CloudEvent> consumer = topicMap.get(ce.getType());
          if (consumer != null) {
            consumer.accept(ce);
          }
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          logger.info("Accepted event {} for topic {}", ce.getId(), ce.getType());
        },
        serviceFactory.get());
  }
}
