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
package io.serverlessworkflow.fluent.func.serialization.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.serverlessworkflow.api.reflection.func.SerializableConsumer;
import io.serverlessworkflow.api.reflection.func.SerializableFunction;
import io.serverlessworkflow.api.reflection.func.SerializablePredicate;
import io.serverlessworkflow.api.types.FunctionArguments;
import io.serverlessworkflow.api.types.TaskMetadata;
import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import io.serverlessworkflow.api.types.func.LoopFunction;
import io.serverlessworkflow.api.types.func.LoopFunctionIndex;
import io.serverlessworkflow.api.types.func.LoopPredicate;
import io.serverlessworkflow.api.types.func.LoopPredicateIndex;
import io.serverlessworkflow.api.types.func.LoopPredicateIndexContext;
import io.serverlessworkflow.api.types.func.LoopPredicateIndexFilter;
import java.lang.invoke.SerializedLambda;
import java.util.List;

public class FuncJacksonModule extends SimpleModule {

  private static final long serialVersionUID = 1L;

  public void setupModule(com.fasterxml.jackson.databind.Module.SetupContext context) {
    SerializableFunctionSerializer serializer = new SerializableFunctionSerializer();
    super.addSerializer(SerializableFunction.class, serializer);
    super.addSerializer(SerializablePredicate.class, serializer);
    super.addSerializer(SerializableConsumer.class, serializer);
    super.addSerializer(ContextFunction.class, serializer);
    super.addSerializer(FilterFunction.class, serializer);
    super.addSerializer(LoopFunction.class, serializer);
    super.addSerializer(LoopFunctionIndex.class, serializer);
    super.addSerializer(LoopPredicate.class, serializer);
    super.addSerializer(LoopPredicateIndex.class, serializer);
    super.addSerializer(LoopPredicateIndexContext.class, serializer);
    super.addSerializer(LoopPredicateIndexFilter.class, serializer);
    super.addDeserializer(SerializedLambda.class, new SerializedLambdaDeserializer());
    super.setMixInAnnotation(TaskMetadata.class, TaskMetadataMixIn.class);
    super.setMixInAnnotation(FunctionArguments.class, FunctionArgumentsMixIn.class);
    super.setSerializerModifier(
        new BeanSerializerModifier() {
          @Override
          public List<BeanPropertyWriter> changeProperties(
              SerializationConfig config,
              BeanDescription beanDesc,
              List<BeanPropertyWriter> beanProperties) {
            if (beanDesc.getBeanClass().equals(SerializedLambda.class)) {
              beanProperties.add(new SerializedLambdaWriter(beanProperties.get(0)));
            }
            return beanProperties;
          }
        });
    super.setupModule(context);
  }
}
