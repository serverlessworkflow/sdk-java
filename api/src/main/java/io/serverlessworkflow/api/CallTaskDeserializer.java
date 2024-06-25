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
package io.serverlessworkflow.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.serverlessworkflow.api.types.CallAsyncAPI;
import io.serverlessworkflow.api.types.CallGRPC;
import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.CallTask;
import java.io.IOException;
import java.util.List;

class CallTaskDeserializer extends JsonDeserializer<CallTask> {

  @Override
  public CallTask deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return DeserializeHelper.deserialize(
        p,
        CallTask.class,
        List.of(CallHTTP.class, CallAsyncAPI.class, CallOpenAPI.class, CallGRPC.class));
  }
}
