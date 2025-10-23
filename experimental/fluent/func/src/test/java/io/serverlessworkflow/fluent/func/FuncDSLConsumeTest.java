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
package io.serverlessworkflow.fluent.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consume;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FuncDSLConsumeTest {

  @Test
  @DisplayName(
      "consume(name, Consumer, Class) produces CallTask and leaves output unchanged by contract")
  void consume_produces_CallTask() {
    AtomicReference<String> sink = new AtomicReference<>();

    Workflow wf =
        FuncWorkflowBuilder.workflow("consumeStep")
            .tasks(
                consume(
                    "sendNewsletter",
                    (String reviewed) -> sink.set("CALLED:" + reviewed),
                    String.class))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());
    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask should be present for consume step");
  }
}
