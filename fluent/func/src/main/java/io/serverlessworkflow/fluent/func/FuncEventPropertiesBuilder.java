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
import io.serverlessworkflow.api.types.func.EventDataFunction;
import io.serverlessworkflow.fluent.spec.AbstractEventPropertiesBuilder;
import java.util.function.Function;

public class FuncEventPropertiesBuilder
    extends AbstractEventPropertiesBuilder<FuncEventPropertiesBuilder> {

  @Override
  protected FuncEventPropertiesBuilder self() {
    return this;
  }

  public <T> FuncEventPropertiesBuilder data(Function<T, CloudEventData> function) {
    this.eventProperties.setData(new EventDataFunction().withFunction(function));
    return this;
  }

  public <T> FuncEventPropertiesBuilder data(Function<T, CloudEventData> function, Class<T> clazz) {
    this.eventProperties.setData(new EventDataFunction().withFunction(function, clazz));
    return this;
  }
}
