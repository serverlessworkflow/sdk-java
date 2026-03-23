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
package io.serverlessworkflow.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WorkflowListenerTest {

  private WorkflowModelFactory modelFactory;

  @BeforeEach
  void setup() {
    modelFactory = Mockito.mock(WorkflowModelFactory.class);
  }

  @Test
  void testSorted() {
    WorkflowExecutionListener mediumPrio = new MediumPriorityListener("javi");
    WorkflowExecutionListener lowestPrio = new LowestPriorityListener();
    WorkflowExecutionListener topPrio = new TopPriorityListener();
    WorkflowExecutionListener anotherMediumPrio = new MediumPriorityListener("javier");

    WorkflowApplication app =
        WorkflowApplication.builder()
            .withModelFactory(modelFactory)
            .withListener(mediumPrio)
            .withListener(lowestPrio)
            .withListener(topPrio)
            .withListener(anotherMediumPrio)
            .build();

    assertThat(app.listeners()).hasSize(5);
    assertThat(app.listeners())
        .startsWith(topPrio, mediumPrio, anotherMediumPrio, app.schedulerListener(), lowestPrio);
  }

  @Test
  void testNotDuplicated() {
    WorkflowExecutionListener mediumPrio = new MediumPriorityListener("javi");
    WorkflowExecutionListener lowestPrio = new LowestPriorityListener();
    WorkflowExecutionListener topPrio = new TopPriorityListener();
    WorkflowExecutionListener anotherMediumPrio = new MediumPriorityListener("javi");

    WorkflowApplication app =
        WorkflowApplication.builder()
            .withModelFactory(modelFactory)
            .withListener(mediumPrio)
            .withListener(lowestPrio)
            .withListener(topPrio)
            .withListener(anotherMediumPrio)
            .build();

    assertThat(app.listeners()).hasSize(4);
    assertThat(app.listeners())
        .startsWith(topPrio, mediumPrio, app.schedulerListener(), lowestPrio);
  }
}
