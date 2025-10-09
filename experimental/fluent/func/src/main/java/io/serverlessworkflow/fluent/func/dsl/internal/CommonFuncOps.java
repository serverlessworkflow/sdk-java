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
package io.serverlessworkflow.fluent.func.dsl.internal;

import io.cloudevents.CloudEventData;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncPredicateEventConfigurer;
import io.serverlessworkflow.fluent.func.configurers.SwitchCaseConfigurer;
import io.serverlessworkflow.fluent.func.dsl.ReflectionUtils;
import io.serverlessworkflow.fluent.func.dsl.SwitchCaseSpec;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface CommonFuncOps {

  default <T, V> Consumer<FuncCallTaskBuilder> fn(Function<T, V> function, Class<T> argClass) {
    return f -> f.function(function, argClass);
  }

  default <T, V> Consumer<FuncCallTaskBuilder> fn(Function<T, V> function) {
    Class<T> clazz = ReflectionUtils.inferInputType(function);
    return f -> f.function(function, clazz);
  }

  default Consumer<FuncSwitchTaskBuilder> cases(SwitchCaseConfigurer... cases) {
    return s -> {
      for (SwitchCaseConfigurer c : cases) {
        s.onPredicate(c);
      }
    };
  }

  default <T> SwitchCaseSpec<T> caseOf(Predicate<T> when, Class<T> whenClass) {
    return new SwitchCaseSpec<T>().when(when, whenClass);
  }

  default <T> SwitchCaseSpec<T> caseOf(Predicate<T> when) {
    return new SwitchCaseSpec<T>().when(when);
  }

  default SwitchCaseConfigurer caseDefault(String task) {
    return s -> s.then(task);
  }

  default SwitchCaseConfigurer caseDefault(FlowDirectiveEnum directive) {
    return s -> s.then(directive);
  }

  default <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function) {
    return event -> event.event(e -> e.type(type).data(function));
  }

  default <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function, Class<T> clazz) {
    return event -> event.event(e -> e.type(type).data(function, clazz));
  }

  default FuncPredicateEventConfigurer event(String type) {
    return e -> e.type(type);
  }
}
