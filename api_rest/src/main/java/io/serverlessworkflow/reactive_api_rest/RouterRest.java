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

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterRest implements IRouterRest {

  /***
   * Get image SVG from string.
   * @param serverlessRequest The serverless request.
   * @return String from SVG image
   */
  @Bean
  @Override
  public RouterFunction<ServerResponse> servelessRouterFunction(ServerlessRequest serverlessRequest) {
    return route(POST(RouterPaths.GETTING_SVG_FROM_WORKFLOW),
            serverlessRequest::getDiagramSVGFromWorkFlow);
  }

}
