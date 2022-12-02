package io.serverlessworkflow.reactive_api_rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ServerlessRequest {

  private final ServerlesRequestHelper helper;

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
                .flatMap(helper::getSvg)
                .onErrorMap(e -> new ControllerErrorException(e.getMessage())),
            ServerlessWorkFlowResponse.class);
  }
}
