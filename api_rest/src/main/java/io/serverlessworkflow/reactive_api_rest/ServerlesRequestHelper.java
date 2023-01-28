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

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.WorkflowDiagram;
import io.serverlessworkflow.diagram.WorkflowDiagramImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ServerlesRequestHelper {

  public Mono<ServerlessWorkFlowResponse> getSvg(String workFlow) {
    String diagramSVG = "";
    Workflow workflow = Workflow.fromSource(workFlow);

    ServerlessWorkFlowResponse response = new ServerlessWorkFlowResponse();

    WorkflowDiagram workflowDiagram =
        new WorkflowDiagramImpl()
            .showLegend(true)
            .setWorkflow(workflow)
            .setTemplate("custom-template");

    try {
      diagramSVG = workflowDiagram.getSvgDiagram();
    } catch (Exception e) {
      response.setResponse(e.getMessage());
      return Mono.just(response);
    }

    response.setResponse(diagramSVG);

    return Mono.just(response);
  }
}
