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
package io.serverlessworkflow.reactive_api_rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

public interface IRouterRest {

  @RouterOperations(
      value = {
        @RouterOperation(
            path = RouterPaths.GETTING_SVG_FROM_WORKFLOW,
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.POST,
            beanClass = ServerlessRequest.class,
            beanMethod = "getDiagramSVGFromWorkFlow",
            operation =
                @Operation(
                    operationId = "Get-Diagram-SVG-From-WorkFlow",
                    responses = {
                      @ApiResponse(
                          responseCode = "200",
                          description = "Get diagram SVG from workFlow",
                          content =
                              @Content(
                                  schema =
                                      @Schema(implementation = ServerlessWorkFlowResponse.class)))
                    }
                    /*,
                    security = @SecurityRequirement(
                            name = "bearer-key",
                            scopes = {}
                    )*/
                    ))
      })
  RouterFunction<ServerResponse> servelessRouterFunction(ServerlessRequest serverlessRequest);


}
