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
package io.serverlessworkflow.diagramrest;

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
        DiagramRequest.class,
        DiagramRequestHelper.class
})
@WebFluxTest
class ServerlesRequestHelperTest {

    private DiagramRequestHelper serverlesRequestHelper;

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
        serverlesRequestHelper = new DiagramRequestHelper();
    }

    @Test
    void getSvg() {
        Mono<String> monoSvg = serverlesRequestHelper.getSvg(input);
        monoSvg.subscribe(result -> { assertNotNull(result); assertNotNull(result);});
        StepVerifier.create(monoSvg)
                .expectNextMatches(serverlessWorkFlowResponse -> serverlessWorkFlowResponse
                        .contains("svg"))
                .verifyComplete();
    }
}