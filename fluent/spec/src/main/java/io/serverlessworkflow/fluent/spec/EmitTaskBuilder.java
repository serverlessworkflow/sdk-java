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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.EmitEventDefinition;
import io.serverlessworkflow.api.types.EmitTask;
import io.serverlessworkflow.api.types.EmitTaskConfiguration;
import java.util.function.Consumer;

public class EmitTaskBuilder extends TaskBaseBuilder<EmitTaskBuilder> {

  private final EmitTask emitTask;

  protected EmitTaskBuilder() {
    this.emitTask = new EmitTask();
    super.setTask(emitTask);
  }

  public EmitTaskBuilder event(Consumer<EventPropertiesBuilder> consumer) {
    final EventPropertiesBuilder eventPropertiesBuilder = new EventPropertiesBuilder();
    consumer.accept(eventPropertiesBuilder);
    this.emitTask.setEmit(
        new EmitTaskConfiguration()
            .withEvent(new EmitEventDefinition().withWith(eventPropertiesBuilder.build())));
    return this;
  }

  public EmitTask build() {
    return emitTask;
  }

  @Override
  protected EmitTaskBuilder self() {
    return this;
  }
}
