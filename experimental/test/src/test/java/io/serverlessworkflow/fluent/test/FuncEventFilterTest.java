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
package io.serverlessworkflow.fluent.test;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emitJson;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Event Filter DSL specification. Verifies that the fluent builder correctly wires
 * the payload parsing and contextual lambdas into the final Workflow definitions.
 */
class FuncEventFilterTest {

  @Test
  void testListenToOneCollection() {
    runIt(
        FuncWorkflowBuilder.workflow("listenToOneReviewCol")
            .tasks(
                listen("waitReview", toOne("org.acme.test.review"))
                    .outputAs((Collection<?> node) -> node.iterator().next()))
            .build());
  }

  @Test
  void testListenToOneNode() {
    runIt(
        FuncWorkflowBuilder.workflow("listenToOneReviewNode")
            .tasks(
                listen("waitReview", toOne("org.acme.test.review"))
                    .outputAs((ArrayNode node) -> node.get(0)))
            .build());
  }

  private Workflow reviewEmitter() {
    return FuncWorkflowBuilder.workflow("emitReview")
        .tasks(emitJson("draftReady", "org.acme.test.review", Review.class))
        .build();
  }

  private void runIt(Workflow listen) {
    Review review = new Review("Torrente", "espectacular", 5);
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      CompletableFuture<WorkflowModel> waiting =
          app.workflowDefinition(listen).instance(Map.of()).start();
      app.workflowDefinition(reviewEmitter()).instance(review).start().join();
      assertThat(waiting.join().as(Review.class).orElseThrow()).isEqualTo(review);
    }
  }
}
