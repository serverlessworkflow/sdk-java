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
   * @param serverlessRequest
   * @return
   */
  @Bean
  @Override
  public RouterFunction<ServerResponse> servelessRouterFunction(ServerlessRequest serverlessRequest) {
    return route(POST(RouterPaths.GETTING_SVG_FROM_WORKFLOW),
            serverlessRequest::getDiagramSVGFromWorkFlow);
  }

}
