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

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ServerlessRequest {

  /**
   * Get the SVG diagram of a workflow from API Request
   *
   * @param sRequest workFlow (yml or json)
   * @return String SVG
   */
  public Mono<ServerResponse> getDiagramSVGFromWorkFlow(ServerRequest sRequest) {
    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            sRequest
                .bodyToMono(String.class)
                .flatMap(ServerlesRequestHelper::getSvg)
                .onErrorMap(e -> new IllegalArgumentException(e.getMessage())),
            ServerlessWorkFlowResponse.class);
  }
}
