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
package io.serverlessworkflow.fluent.func.spi;

import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.func.ExportAsFunction;
import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.fluent.spec.spi.TaskTransformationHandlers;
import java.util.function.Function;

public interface FuncTaskTransformations<SELF extends FuncTaskTransformations<SELF>>
    extends TaskTransformationHandlers, FuncTransformations<SELF> {

  @SuppressWarnings("unchecked")
  default <T, V> SELF exportAs(Function<T, V> function) {
    setExport(new Export().withAs(new ExportAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF exportAs(Function<T, V> function, Class<T> argClass) {
    setExport(new Export().withAs(new ExportAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF exportAs(JavaFilterFunction<T, V> function) {
    setExport(new Export().withAs(new ExportAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF exportAs(JavaFilterFunction<T, V> function, Class<T> argClass) {
    setExport(new Export().withAs(new ExportAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF exportAs(JavaContextFunction<T, V> function) {
    setExport(new Export().withAs(new ExportAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF exportAs(JavaContextFunction<T, V> function, Class<T> argClass) {
    setExport(new Export().withAs(new ExportAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }
}
