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
package io.serverlessworkflow.fluent.func;

import io.cloudevents.CloudEventData;
import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.EventDataFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import io.serverlessworkflow.fluent.func.dsl.SerializableFunction;
import io.serverlessworkflow.fluent.spec.AbstractEventPropertiesBuilder;
import java.util.function.Function;

public class FuncEmitEventPropertiesBuilder
    extends AbstractEventPropertiesBuilder<FuncEmitEventPropertiesBuilder> {

  @Override
  protected FuncEmitEventPropertiesBuilder self() {
    return this;
  }

  public <T> FuncEmitEventPropertiesBuilder data(SerializableFunction<T, CloudEventData> function) {
    this.eventProperties.setData(new EventDataFunction().withFunction(function));
    return this;
  }

  public <T> FuncEmitEventPropertiesBuilder data(
      Function<T, CloudEventData> function, Class<T> clazz) {
    this.eventProperties.setData(new EventDataFunction().withFunction(function, clazz));
    return this;
  }

  public <T> FuncEmitEventPropertiesBuilder data(
      ContextFunction<T, CloudEventData> function, Class<T> clazz) {
    this.eventProperties.setData(new EventDataFunction().withFunction(function, clazz));
    return this;
  }

  public <T> FuncEmitEventPropertiesBuilder data(
      FilterFunction<T, CloudEventData> function, Class<T> clazz) {
    this.eventProperties.setData(new EventDataFunction().withFunction(function, clazz));
    return this;
  }
}
