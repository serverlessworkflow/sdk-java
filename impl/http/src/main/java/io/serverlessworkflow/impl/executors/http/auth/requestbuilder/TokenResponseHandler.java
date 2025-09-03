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
package io.serverlessworkflow.impl.executors.http.auth.requestbuilder;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.function.BiFunction;

class TokenResponseHandler
    implements BiFunction<InvocationHolder, TaskContext, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(InvocationHolder invocation, TaskContext context) {
    try (Response response = invocation.call()) {
      if (response.getStatus() < 200 || response.getStatus() >= 300) {
        throw new WorkflowException(
            WorkflowError.communication(
                    response.getStatus(),
                    context,
                    "Failed to obtain token: HTTP "
                        + response.getStatus()
                        + " — "
                        + response.getEntity())
                .build());
      }
      return response.readEntity(new GenericType<>() {});
    } catch (ResponseProcessingException e) {
      throw new WorkflowException(
          WorkflowError.communication(
                  e.getResponse().getStatus(),
                  context,
                  "Failed to process response: " + e.getMessage())
              .build(),
          e);
    } catch (ProcessingException e) {
      throw new WorkflowException(
          WorkflowError.communication(
                  -1, context, "Failed to connect or process request: " + e.getMessage())
              .build(),
          e);
    } finally {
      invocation.close();
    }
  }
}
