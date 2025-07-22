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

import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.func.ExportAsFunction;
import io.serverlessworkflow.api.types.func.InputFromFunction;
import io.serverlessworkflow.api.types.func.OutputAsFunction;
import io.serverlessworkflow.fluent.spec.TransformationHandlers;
import java.util.function.Function;

public interface FuncTransformations<SELF extends FuncTransformations<SELF>>
    extends TransformationHandlers {

  default <T, V> SELF exportAsFn(Function<T, V> function) {
    setExport(new Export().withAs(new ExportAsFunction().withFunction(function)));
    return (SELF) this;
  }

  default <T, V> SELF inputFrom(Function<T, V> function) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function)));
    return (SELF) this;
  }

  default <T, V> SELF outputAs(Function<T, V> function) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function)));
    return (SELF) this;
  }
}
