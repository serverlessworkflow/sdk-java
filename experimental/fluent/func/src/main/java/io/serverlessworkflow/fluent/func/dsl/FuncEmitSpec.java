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
package io.serverlessworkflow.fluent.func.dsl;

import io.cloudevents.CloudEventData;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.data.PojoCloudEventData;
import io.serverlessworkflow.api.types.func.EventDataFunction;
import io.serverlessworkflow.fluent.func.FuncEmitEventPropertiesBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncEmitConfigurer;
import io.serverlessworkflow.fluent.spec.dsl.EventEmitPropertiesSpec;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public final class FuncEmitSpec
    extends EventEmitPropertiesSpec<FuncEmitSpec, FuncEmitEventPropertiesBuilder>
    implements FuncEmitConfigurer {

  @Override
  protected FuncEmitSpec self() {
    return this;
  }

  /** Sets the event data and the contentType to `application/json` */
  public <T> FuncEmitSpec jsonData(SerializableFunction<T, CloudEventData> function) {
    Class<T> clazz = ReflectionUtils.inferInputType(function);
    addPropertyStep(e -> e.data(new EventDataFunction().withFunction(function, clazz)));
    return JSON();
  }

  /** Sets the event data and the contentType to `application/octet-stream` */
  public <T> FuncEmitSpec bytesData(Function<T, byte[]> serializer, Class<T> clazz) {
    addPropertyStep(
        e -> e.data(payload -> BytesCloudEventData.wrap(serializer.apply(payload)), clazz));
    return OCTET_STREAM();
  }

  public FuncEmitSpec bytesDataUtf8() {
    return bytesData((String s) -> s.getBytes(StandardCharsets.UTF_8), String.class);
  }

  /** Sets the event data and the contentType to `application/json` */
  public <T> FuncEmitSpec jsonData(Function<T, CloudEventData> function, Class<T> clazz) {
    addPropertyStep(e -> e.data(new EventDataFunction().withFunction(function, clazz)));
    return JSON();
  }

  /** JSON with default mapper (PojoCloudEventData + application/json). */
  public <T> FuncEmitSpec jsonData(Class<T> clazz) {
    addPropertyStep(
        e ->
            e.data(
                payload ->
                    PojoCloudEventData.wrap(
                        payload, p -> JsonUtils.mapper().writeValueAsString(p).getBytes()),
                clazz));
    return JSON();
  }

  @Override
  public void accept(FuncEmitTaskBuilder funcEmitTaskBuilder) {
    funcEmitTaskBuilder.event(e -> getPropertySteps().forEach(step -> step.accept(e)));
  }
}
