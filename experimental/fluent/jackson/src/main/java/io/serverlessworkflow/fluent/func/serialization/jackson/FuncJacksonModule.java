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
import io.serverlessworkflow.api.types.ExportAs;
import io.serverlessworkflow.api.types.FunctionArguments;
import io.serverlessworkflow.api.types.InputFrom;
import io.serverlessworkflow.api.types.OutputAs;
import io.serverlessworkflow.api.types.TaskMetadata;
import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import io.serverlessworkflow.api.types.func.LoopFunction;
import io.serverlessworkflow.api.types.func.LoopFunctionIndex;
import io.serverlessworkflow.api.types.func.LoopPredicate;
import io.serverlessworkflow.api.types.func.LoopPredicateIndex;
import io.serverlessworkflow.api.types.func.LoopPredicateIndexContext;
import io.serverlessworkflow.api.types.func.LoopPredicateIndexFilter;
import io.serverlessworkflow.api.types.func.SerializableConsumer;
import io.serverlessworkflow.api.types.func.SerializableFunction;
import io.serverlessworkflow.api.types.func.SerializablePredicate;
import java.lang.invoke.SerializedLambda;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

    super.addSerializer(TaskMetadata.class, new TaskMetadataSerializer());
    super.addDeserializer(TaskMetadata.class, new TaskMetadataDeserializer());
    super.addSerializer(FunctionArguments.class, new FunctionArgumentsSerializer());
    super.addDeserializer(FunctionArguments.class, new FunctionArgumentsDeserializer());
    super.addDeserializer(Function.class, new FunctionDeserializer(Function.class));
    super.addDeserializer(Predicate.class, new FunctionDeserializer(Predicate.class));
    super.addDeserializer(Consumer.class, new FunctionDeserializer(Consumer.class));
    super.addDeserializer(ContextFunction.class, new FunctionDeserializer(ContextFunction.class));
    super.addDeserializer(FilterFunction.class, new FunctionDeserializer(FilterFunction.class));
    super.addDeserializer(LoopFunction.class, new FunctionDeserializer(LoopFunction.class));
    super.addDeserializer(
        LoopFunctionIndex.class, new FunctionDeserializer(LoopFunctionIndex.class));
    super.addDeserializer(LoopPredicate.class, new FunctionDeserializer(LoopPredicate.class));
    super.addDeserializer(
        LoopPredicateIndex.class, new FunctionDeserializer(LoopPredicateIndex.class));
    super.addDeserializer(
        LoopPredicateIndexContext.class, new FunctionDeserializer(LoopPredicateIndexContext.class));
    super.addDeserializer(
        LoopPredicateIndexFilter.class, new FunctionDeserializer(LoopPredicateIndexFilter.class));

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
    super.addDeserializer(SerializedLambda.class, new SerializedLambdaDeserializer());

    super.setMixInAnnotation(OutputAs.class, FuncOutputAsMixIn.class);
    super.setMixInAnnotation(ExportAs.class, FuncExportAsMixIn.class);
    super.setMixInAnnotation(InputFrom.class, FuncInputFromMixIn.class);

    super.setupModule(context);
  }
}
