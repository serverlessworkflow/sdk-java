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
