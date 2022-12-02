package io.serverlessworkflow.reactive_api_rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RouterRest.class,
        ServerlessRequest.class,
        ServerlesRequestHelper.class
})
@WebFluxTest
class ServerlesRequestHelperTest {

    private ServerlesRequestHelper serverlesRequestHelper;

    public static final String input = "id: greeting\n" +
            "version: '1.0'\n" +
            "specVersion: '0.8'\n" +
            "name: Greeting Workflow\n" +
            "description: Greet Someone\n" +
            "start: Greet\n" +
            "functions:\n" +
            "  - name: greetingFunction\n" +
            "    operation: file://myapis/greetingapis.json#greeting\n" +
            "states:\n" +
            "  - name: Greet\n" +
            "    type: operation\n" +
            "    actions:\n" +
            "      - functionRef:\n" +
            "          refName: greetingFunction\n" +
            "          arguments:\n" +
            "            name: \"${ .person.name }\"\n" +
            "        actionDataFilter:\n" +
            "          results: \"${ .greeting }\"\n" +
            "    end: true";

    @BeforeEach
    void setUp() {
        serverlesRequestHelper = new ServerlesRequestHelper();
    }

    @Test
    void getSvg() {
        Mono<ServerlessWorkFlowResponse> monoSvg = serverlesRequestHelper.getSvg(input);
        StepVerifier.create(monoSvg)
                .expectNextMatches(serverlessWorkFlowResponse -> serverlessWorkFlowResponse.
                        getResponse()
                        .contains("svg"))
                .verifyComplete();
    }
}