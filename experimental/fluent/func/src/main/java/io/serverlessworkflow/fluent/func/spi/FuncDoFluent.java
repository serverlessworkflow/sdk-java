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

import io.serverlessworkflow.fluent.func.FuncCallHttpTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncCallOpenAPITaskBuilder;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncForTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncForkTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncListenTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncRaiseTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSetTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTryTaskBuilder;
import io.serverlessworkflow.fluent.spec.WorkflowTaskBuilder;
import io.serverlessworkflow.fluent.spec.spi.CallHttpFluent;
import io.serverlessworkflow.fluent.spec.spi.CallOpenAPIFluent;
import io.serverlessworkflow.fluent.spec.spi.EmitFluent;
import io.serverlessworkflow.fluent.spec.spi.ForEachFluent;
import io.serverlessworkflow.fluent.spec.spi.ForkFluent;
import io.serverlessworkflow.fluent.spec.spi.ListenFluent;
import io.serverlessworkflow.fluent.spec.spi.RaiseFluent;
import io.serverlessworkflow.fluent.spec.spi.SetFluent;
import io.serverlessworkflow.fluent.spec.spi.SwitchFluent;
import io.serverlessworkflow.fluent.spec.spi.TryCatchFluent;
import io.serverlessworkflow.fluent.spec.spi.WorkflowFluent;
import java.util.function.Consumer;

public interface FuncDoFluent<SELF extends FuncDoFluent<SELF>>
    extends SetFluent<FuncSetTaskBuilder, SELF>,
        EmitFluent<FuncEmitTaskBuilder, SELF>,
        ForEachFluent<FuncForTaskBuilder, SELF>,
        SwitchFluent<FuncSwitchTaskBuilder, SELF>,
        ForkFluent<FuncForkTaskBuilder, SELF>,
        ListenFluent<FuncListenTaskBuilder, SELF>,
        RaiseFluent<FuncRaiseTaskBuilder, SELF>,
        TryCatchFluent<FuncTryTaskBuilder, SELF>,
        CallFnFluent<FuncCallTaskBuilder, SELF>,
        CallHttpFluent<FuncCallHttpTaskBuilder, SELF>,
        CallOpenAPIFluent<FuncCallOpenAPITaskBuilder, SELF>,
        WorkflowFluent<WorkflowTaskBuilder, SELF> {

  default SELF subflow(String name, Consumer<WorkflowTaskBuilder> itemsConfigurer) {
    return this.workflow(name, itemsConfigurer);
  }

  default SELF subflow(Consumer<WorkflowTaskBuilder> itemsConfigurer) {
    return this.workflow(itemsConfigurer);
  }
}
