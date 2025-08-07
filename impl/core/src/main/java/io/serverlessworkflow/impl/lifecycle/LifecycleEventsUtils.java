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
package io.serverlessworkflow.impl.lifecycle;

import io.serverlessworkflow.impl.WorkflowContext;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifecycleEventsUtils {

  private LifecycleEventsUtils() {}

  private static final Logger logger = LoggerFactory.getLogger(LifecycleEventsUtils.class);

  public static <T extends TaskEvent> void publishEvent(
      WorkflowContext workflowContext, Consumer<WorkflowExecutionListener> consumer) {
    workflowContext
        .definition()
        .application()
        .listeners()
        .forEach(
            v -> {
              try {
                consumer.accept(v);
              } catch (Exception ex) {
                logger.error("Error processing listener. Ignoring and going on", ex);
              }
            });
  }
}
