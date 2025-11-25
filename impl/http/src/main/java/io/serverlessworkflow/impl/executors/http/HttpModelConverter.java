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
package io.serverlessworkflow.impl.executors.http;

import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowModel;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.slf4j.LoggerFactory;

public interface HttpModelConverter {

  default Entity<?> toEntity(WorkflowModel model) {
    return Entity.json(model.as(model.objectClass()).orElseThrow());
  }

  Class<?> responseType();

  default WorkflowError.Builder errorFromResponse(
      WorkflowError.Builder errorBuilder, Response response) {
    try {
      Object title = response.readEntity(responseType());
      if (title != null) {
        errorBuilder.title(title.toString());
      }
    } catch (Exception ex) {
      LoggerFactory.getLogger(HttpModelConverter.class)
          .warn("Problem extracting error from http response", ex);
    }

    return errorBuilder;
  }
}
