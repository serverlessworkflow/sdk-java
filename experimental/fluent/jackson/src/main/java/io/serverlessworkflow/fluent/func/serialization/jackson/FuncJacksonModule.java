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

import com.fasterxml.jackson.databind.module.SimpleModule;

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.func.CallJava;

public class FuncJacksonModule extends SimpleModule {

  private static final long serialVersionUID = 1L;

  public void setupModule(com.fasterxml.jackson.databind.Module.SetupContext context) {
	  super.addSerializer(CallTask.class, new CallTaskFunctionSerializer());
//    super.addDeserializer(CallJava.CallJavaFunction.class, new CallJavaFunctionDeserializer());
 //   super.addSerializer(CallJava.CallJavaFunction.class, new CallJavaFunctionSerializer());
    super.setupModule(context);
  }
}
