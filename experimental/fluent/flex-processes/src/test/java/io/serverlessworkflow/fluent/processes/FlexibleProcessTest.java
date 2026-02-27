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
package io.serverlessworkflow.fluent.processes;

import static io.serverlessworkflow.api.types.func.CallJava.consumer;
import static io.serverlessworkflow.fluent.processes.dsl.FlexibleProcessDSL.activity;
import static io.serverlessworkflow.fluent.processes.dsl.FlexibleProcessDSL.process;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

public class FlexibleProcessTest {

  @Test
  void testJavaFunction() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      Workflow workflow =
          FuncWorkflowBuilder.workflow("testJavaCall")
              .tasks(
                  process(
                      workflowModel -> {
                        Integer counter = (Integer) workflowModel.asMap().get().get("counter");
                        return counter >= 3;
                      },
                      activity(
                          ab ->
                              ab.callTask(
                                      consumer(
                                          map -> {
                                            Integer counter = (Integer) map.get("counter");
                                            counter++;
                                            map.put("counter", counter);
                                          },
                                          Map.class))
                                  .entryCondition(wm -> true)
                                  .isRepeatable(true))))
              .build();

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("counter", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals(3, result.get("counter"));
    }
  }

  @Test
  public void testFluentWorkflow() throws InterruptedException, ExecutionException {

    Activity activity =
        activity(
            builder ->
                builder
                    .callTask(
                        consumer(
                            map -> {
                              Integer counter = (Integer) map.get("counter");
                              counter++;
                              map.put("counter", counter);
                            },
                            Map.class))
                    .entryCondition(workflowModel -> true)
                    .isRepeatable(true));

    FlexibleProcess flexibleProcess =
        new FlexibleProcess(
            workflowModel -> {
              Integer counter = (Integer) workflowModel.asMap().get().get("counter");
              return counter >= 3;
            },
            activity);

    Workflow workflow =
        FuncWorkflowBuilder.workflow("step-emit-export")
            .tasks(
                process("flexible_process", flexibleProcess),
                process("flexible_process", flexibleProcess))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("counter", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals(3, result.get("counter"));
    }
  }

  @Test
  void testMaxIterations() throws InterruptedException, ExecutionException {
    AtomicInteger attemptCounter1 = new AtomicInteger(0);
    AtomicInteger attemptCounter2 = new AtomicInteger(0);

    Activity incrementer1 =
        Activity.builder()
            .callTask(consumer(map -> attemptCounter1.incrementAndGet(), Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Activity incrementer2 =
        Activity.builder()
            .callTask(consumer(map -> attemptCounter2.incrementAndGet(), Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testMultipleActivities")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(workflowModel -> false, incrementer1, incrementer2)
                              .setMaxAttempts(5)
                              .asTask())));

      app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    }
    assertEquals(5, attemptCounter1.get());
    assertEquals(5, attemptCounter2.get());
  }

  @Test
  void testConditionalActivityExecution() throws InterruptedException, ExecutionException {
    Activity evenIncrementer =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer counter = (Integer) map.get("counter");
                      map.put("counter", counter + 2);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Integer counter = (Integer) workflowModel.asMap().get().get("counter");
                  return counter % 2 == 0;
                })
            .isRepeatable(true)
            .build();

    Activity oddIncrementer =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer counter = (Integer) map.get("counter");
                      map.put("counter", counter + 1);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Integer counter = (Integer) workflowModel.asMap().get().get("counter");
                  return counter % 2 != 0;
                })
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> exceeds10 =
        workflowModel -> {
          Integer counter = (Integer) workflowModel.asMap().get().get("counter");
          return counter >= 10;
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testConditional")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(exceeds10, evenIncrementer, oddIncrementer)
                              .asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("counter", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertTrue((Integer) result.get("counter") >= 10);
    }
  }

  @Test
  void testWorkflowWithMultipleFields() throws InterruptedException, ExecutionException {
    Activity calculator =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer a = (Integer) map.get("a");
                      Integer b = (Integer) map.get("b");
                      map.put("sum", a + b);
                      map.put("a", a + 1);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> sumExceeds20 =
        workflowModel -> {
          Map<String, Object> data = workflowModel.asMap().get();
          Integer sum = (Integer) data.getOrDefault("sum", 0);
          return sum > 20;
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testMultipleFields")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(sumExceeds20, calculator).asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("a", 5, "b", 10))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertTrue((Integer) result.get("sum") > 20);
      assertTrue((Integer) result.get("a") > 5);
      assertEquals(10, result.get("b"));
    }
  }

  @Test
  void testImmediateExitCondition() throws InterruptedException, ExecutionException {
    Activity shouldNotRun =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("executed", true);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> alwaysTrue = workflowModel -> true;

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testImmediateExit")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(alwaysTrue, shouldNotRun).asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("counter", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertFalse(result.containsKey("executed"));
      assertEquals(0, result.get("counter"));
    }
  }

  @Test
  void testChainedFlexibleProcesses() throws InterruptedException, ExecutionException {
    Activity multiplier =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer value = (Integer) map.get("value");
                      map.put("value", value * 2);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Activity subtractor =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer value = (Integer) map.get("value");
                      map.put("value", value - 5);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> exceeds50 =
        workflowModel -> {
          Integer value = (Integer) workflowModel.asMap().get().get("value");
          return value >= 50;
        };

    Predicate<WorkflowModel> below10 =
        workflowModel -> {
          Integer value = (Integer) workflowModel.asMap().get().get("value");
          return value < 10;
        };

    FlexibleProcess growthProcess = new FlexibleProcess(exceeds50, multiplier);

    FlexibleProcess shrinkProcess = new FlexibleProcess(below10, subtractor);

    Workflow workflow =
        FuncWorkflowBuilder.workflow("chained-processes")
            .tasks(process("growth", growthProcess), process("shrink", shrinkProcess))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("value", 5))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertTrue((Integer) result.get("value") < 10);
    }
  }

  @Test
  void testNoActivitiesMatch() throws InterruptedException, ExecutionException {
    Activity neverRuns =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("ran", true);
                    },
                    Map.class))
            .entryCondition(workflowModel -> false) // Never matches
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> maxIterations =
        workflowModel -> {
          Integer iterations = (Integer) workflowModel.asMap().get().getOrDefault("iterations", 0);
          return iterations >= 5;
        };

    Activity iterationCounter =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer iterations = (Integer) map.getOrDefault("iterations", 0);
                      map.put("iterations", iterations + 1);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testNoMatch").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(maxIterations, neverRuns, iterationCounter)
                              .asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();

      assertFalse(result.containsKey("ran"));
      assertEquals(5, result.get("iterations"));
    }
  }

  @Test
  void testComplexBusinessLogic() throws InterruptedException, ExecutionException {
    Activity validator =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer score = (Integer) map.get("score");
                      if (score < 0) {
                        map.put("score", 0);
                      }
                      map.put("validated", true);
                    },
                    Map.class))
            .entryCondition(workflowModel -> !workflowModel.asMap().get().containsKey("validated"))
            .isRepeatable(false)
            .build();

    Activity scoreBooster =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer score = (Integer) map.get("score");
                      Integer boost = (Integer) map.getOrDefault("boost", 5);
                      map.put("score", score + boost);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Boolean validated = (Boolean) workflowModel.asMap().get().get("validated");
                  return validated != null && validated;
                })
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> targetReached =
        workflowModel -> {
          Integer score = (Integer) workflowModel.asMap().get().get("score");
          Integer target = (Integer) workflowModel.asMap().get().get("target");
          return score >= target;
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testBusinessLogic")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(targetReached, validator, scoreBooster).asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("score", -10, "target", 30, "boost", 7))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals(true, result.get("validated"));
      assertTrue((Integer) result.get("score") >= 30);
    }
  }

  @Test
  void testLargeNumberOfActivities() throws InterruptedException, ExecutionException {
    Activity[] activities = new Activity[10];
    for (int i = 0; i < activities.length; i++) {
      final int index = i;
      activities[i] =
          Activity.builder()
              .callTask(
                  consumer(
                      map -> {
                        Integer value = (Integer) map.getOrDefault("value", 0);
                        map.put("value", value + index + 1);
                      },
                      Map.class))
              .entryCondition(workflowModel -> true)
              .isRepeatable(true)
              .build();
    }

    Predicate<WorkflowModel> valueAtLeast200 =
        workflowModel -> {
          Integer value = (Integer) workflowModel.asMap().get().getOrDefault("value", 0);
          return value >= 200;
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testLargeNumberOfActivities")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(valueAtLeast200, activities).asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("value", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      Integer value = (Integer) result.get("value");
      assertTrue(value >= 200, "Expected value >= 200, got " + value);
    }
  }

  @Test
  void testStageDrivenActivitiesFlow() throws InterruptedException, ExecutionException {
    Activity[] activities = new Activity[5];
    for (int i = 0; i < activities.length; i++) {
      final int index = i;
      activities[i] =
          Activity.builder()
              .callTask(
                  consumer(
                      map -> {
                        Integer stage = (Integer) map.getOrDefault("stage", 0);
                        map.put("stage", stage + 1);
                        String executed = (String) map.getOrDefault("executed", "");
                        map.put("executed", executed + index);
                      },
                      Map.class))
              .entryCondition(
                  workflowModel -> {
                    Integer stage = (Integer) workflowModel.asMap().get().getOrDefault("stage", 0);
                    return stage == index;
                  })
              .isRepeatable(true)
              .build();
    }

    Predicate<WorkflowModel> allStagesCompleted =
        workflowModel -> {
          Integer stage = (Integer) workflowModel.asMap().get().getOrDefault("stage", 0);
          return stage >= 5;
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testStageDrivenActivitiesFlow")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(allStagesCompleted, activities).asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of("stage", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      Integer finalStage = (Integer) result.get("stage");
      String executed = (String) result.get("executed");

      assertEquals(5, finalStage);
      assertEquals("01234", executed);
    }
  }

  @Test
  void testMultipleRepeatableActivitiesWithMaxAttempts()
      throws InterruptedException, ExecutionException {

    AtomicInteger[] attemptCounters = new AtomicInteger[3];
    for (int i = 0; i < attemptCounters.length; i++) {
      attemptCounters[i] = new AtomicInteger(0);
    }

    Activity[] activities = new Activity[attemptCounters.length];
    for (int i = 0; i < activities.length; i++) {
      final AtomicInteger counter = attemptCounters[i];
      activities[i] =
          Activity.builder()
              .callTask(consumer(map -> counter.incrementAndGet(), Map.class))
              .entryCondition(workflowModel -> true)
              .isRepeatable(true)
              .build();
    }

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testMultipleRepeatableActivitiesWithMaxAttempts")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(workflowModel -> false, activities)
                              .setMaxAttempts(7)
                              .asTask())));

      app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    }

    for (int i = 0; i < attemptCounters.length; i++) {
      assertEquals(
          7,
          attemptCounters[i].get(),
          "Activity " + i + " expected 7 attempts, got " + attemptCounters[i].get());
    }
  }

  @Test
  void testDependentActivitiesWithFlagsChain() throws InterruptedException, ExecutionException {
    Activity loadCustomer =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("customerLoaded", true);
                      map.putIfAbsent("balance", 0);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  return data.get("customerLoaded") == null;
                })
            .isRepeatable(true)
            .build();

    Activity applyWelcomeBonus =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer balance = (Integer) map.getOrDefault("balance", 0);
                      map.put("balance", balance + 100);
                      map.put("bonusApplied", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  Boolean customerLoaded = (Boolean) data.get("customerLoaded");
                  Boolean bonusApplied = (Boolean) data.get("bonusApplied");
                  return Boolean.TRUE.equals(customerLoaded) && bonusApplied == null;
                })
            .isRepeatable(true)
            .build();

    Activity chargeFee =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer balance = (Integer) map.getOrDefault("balance", 0);
                      map.put("balance", balance - 10);
                      map.put("feeCharged", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  Boolean bonusApplied = (Boolean) data.get("bonusApplied");
                  Boolean feeCharged = (Boolean) data.get("feeCharged");
                  return Boolean.TRUE.equals(bonusApplied) && feeCharged == null;
                })
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> accountingFinished =
        workflowModel -> {
          Map<String, Object> data = workflowModel.asMap().get();
          Boolean feeCharged = (Boolean) data.get("feeCharged");
          return Boolean.TRUE.equals(feeCharged);
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testDependentActivitiesWithFlagsChain")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(
                                  accountingFinished, loadCustomer, applyWelcomeBonus, chargeFee)
                              .asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of()) // никаких полей изначально
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals(true, result.get("customerLoaded"));
      assertEquals(true, result.get("bonusApplied"));
      assertEquals(true, result.get("feeCharged"));
      // 0 + 100 - 10 = 90
      assertEquals(90, result.get("balance"));
    }
  }

  @Test
  void testProducerConsumerDependency() throws InterruptedException, ExecutionException {
    Activity producer =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer produced = (Integer) map.getOrDefault("produced", 0);
                      Integer queue = (Integer) map.getOrDefault("queue", 0);
                      map.put("produced", produced + 1);
                      map.put("queue", queue + 1);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Activity consumerActivity =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer queue = (Integer) map.getOrDefault("queue", 0);
                      Integer processed = (Integer) map.getOrDefault("processed", 0);
                      if (queue > 0) {
                        map.put("queue", queue - 1);
                        map.put("processed", processed + 1);
                      }
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Integer queue = (Integer) workflowModel.asMap().get().getOrDefault("queue", 0);
                  return queue > 0;
                })
            .isRepeatable(true)
            .build();

    Activity completionMarker =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("done", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Integer processed =
                      (Integer) workflowModel.asMap().get().getOrDefault("processed", 0);
                  Boolean done = (Boolean) workflowModel.asMap().get().get("done");
                  return processed >= 5 && done == null;
                })
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> producerConsumerFinished =
        workflowModel -> {
          Boolean done = (Boolean) workflowModel.asMap().get().get("done");
          return Boolean.TRUE.equals(done);
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testProducerConsumerDependency")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(
                                  producerConsumerFinished,
                                  producer,
                                  consumerActivity,
                                  completionMarker)
                              .asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();

      Integer produced = (Integer) result.getOrDefault("produced", 0);
      Integer processed = (Integer) result.getOrDefault("processed", 0);
      Integer queue = (Integer) result.getOrDefault("queue", 0);

      assertTrue(processed >= 5);
      assertTrue(produced >= processed);
      assertTrue(queue >= 0);
      assertEquals(true, result.get("done"));
    }
  }

  @Test
  void testCascadingDependenciesMultipleFields() throws InterruptedException, ExecutionException {
    Activity prepareInput =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.putIfAbsent("x", 2);
                      map.putIfAbsent("y", 3);
                      map.put("prepared", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  return data.get("prepared") == null;
                })
            .isRepeatable(true)
            .build();

    Activity computeIntermediate =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer x = (Integer) map.get("x");
                      Integer y = (Integer) map.get("y");
                      map.put("z", x * y);
                      map.put("intermediateComputed", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  Boolean prepared = (Boolean) data.get("prepared");
                  Boolean intermediateComputed = (Boolean) data.get("intermediateComputed");
                  return Boolean.TRUE.equals(prepared) && intermediateComputed == null;
                })
            .isRepeatable(true)
            .build();

    Activity finalizeResult =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer z = (Integer) map.get("z");
                      map.put("result", z + 10);
                      map.put("finalized", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  Boolean intermediateComputed = (Boolean) data.get("intermediateComputed");
                  Boolean finalized = (Boolean) data.get("finalized");
                  return Boolean.TRUE.equals(intermediateComputed) && finalized == null;
                })
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> cascadingFinished =
        workflowModel -> {
          Boolean finalized = (Boolean) workflowModel.asMap().get().get("finalized");
          return Boolean.TRUE.equals(finalized);
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testCascadingDependenciesMultipleFields")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(
                                  cascadingFinished,
                                  prepareInput,
                                  computeIntermediate,
                                  finalizeResult)
                              .asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow)
              .instance(Map.of()) // пустой вход
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals(true, result.get("prepared"));
      assertEquals(true, result.get("intermediateComputed"));
      assertEquals(true, result.get("finalized"));

      // x = 2, y = 3, z = 6, result = 16
      assertEquals(2, result.get("x"));
      assertEquals(3, result.get("y"));
      assertEquals(6, result.get("z"));
      assertEquals(16, result.get("result"));
    }
  }

  @Test
  void testActivityEnablesAndThenDisablesAnother() throws InterruptedException, ExecutionException {

    AtomicInteger togglerExecutions = new AtomicInteger(0);
    AtomicInteger workerExecutions = new AtomicInteger(0);

    Activity toggler =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      togglerExecutions.incrementAndGet();
                      Boolean enabled = (Boolean) map.get("workerEnabled");
                      map.put("workerEnabled", enabled == null ? Boolean.TRUE : !enabled);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Activity worker =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      workerExecutions.incrementAndGet();
                      Integer processed = (Integer) map.getOrDefault("processed", 0);
                      map.put("processed", processed + 1);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  Boolean enabled = (Boolean) data.get("workerEnabled");
                  return Boolean.TRUE.equals(enabled);
                })
            .isRepeatable(true)
            .build();

    Predicate<WorkflowModel> toggleScenarioFinished =
        workflowModel -> {
          Integer processed = (Integer) workflowModel.asMap().get().getOrDefault("processed", 0);
          return processed >= 3;
        };

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testActivityEnablesAndThenDisablesAnother")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "flexible_process",
                          new FlexibleProcess(toggleScenarioFinished, toggler, worker)
                              .setMaxAttempts(20)
                              .asTask())));

      Map<String, Object> result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();

      Integer processed = (Integer) result.getOrDefault("processed", 0);
      assertTrue(processed >= 3);
      assertTrue(togglerExecutions.get() >= workerExecutions.get());
    }
  }

  @Test
  void testChainedFlexibleProcessesSelectiveActivities()
      throws InterruptedException, ExecutionException {
    Activity classifier =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer amount = (Integer) map.get("amount");
                      String risk = amount != null && amount >= 100 ? "HIGH" : "LOW";
                      map.put("risk", risk);
                      map.put("classified", true);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(false)
            .build();

    Predicate<WorkflowModel> classificationDone =
        workflowModel -> {
          Map<String, Object> data = workflowModel.asMap().get();
          return Boolean.TRUE.equals(data.get("classified"));
        };

    FlexibleProcess classificationProcess = new FlexibleProcess(classificationDone, classifier);

    Activity highRiskHandler =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("highHandled", true);
                      map.put("handled", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  return "HIGH".equals(data.get("risk")) && data.get("handled") == null;
                })
            .isRepeatable(false)
            .build();

    Activity lowRiskHandler =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("lowHandled", true);
                      map.put("handled", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  return "LOW".equals(data.get("risk")) && data.get("handled") == null;
                })
            .isRepeatable(false)
            .build();

    Predicate<WorkflowModel> handlingFinished =
        workflowModel -> {
          Map<String, Object> data = workflowModel.asMap().get();
          return Boolean.TRUE.equals(data.get("handled"));
        };

    FlexibleProcess handlingProcess =
        new FlexibleProcess(handlingFinished, highRiskHandler, lowRiskHandler);

    Workflow workflow =
        FuncWorkflowBuilder.workflow("chained-classifier-handler")
            .tasks(process("classify", classificationProcess), process("handle", handlingProcess))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      Map<String, Object> highResult =
          app.workflowDefinition(workflow)
              .instance(Map.of("amount", 200))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals("HIGH", highResult.get("risk"));
      assertEquals(true, highResult.get("classified"));
      assertEquals(true, highResult.get("handled"));
      assertEquals(true, highResult.get("highHandled"));
      assertFalse(highResult.containsKey("lowHandled"));

      Map<String, Object> lowResult =
          app.workflowDefinition(workflow)
              .instance(Map.of("amount", 50))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals("LOW", lowResult.get("risk"));
      assertEquals(true, lowResult.get("classified"));
      assertEquals(true, lowResult.get("handled"));
      assertEquals(true, lowResult.get("lowHandled"));
      assertFalse(lowResult.containsKey("highHandled"));
    }
  }

  @Test
  void testNonRepeatableActivityResetsBetweenInstancesOfSameDefinition()
      throws InterruptedException, ExecutionException {

    Activity oneShot =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer counter = (Integer) map.getOrDefault("counter", 0);
                      map.put("counter", counter + 1);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(false)
            .build();

    Predicate<WorkflowModel> counterReachedOne =
        workflowModel -> {
          Integer counter = (Integer) workflowModel.asMap().get().getOrDefault("counter", 0);
          return counter >= 1;
        };

    Workflow workflow =
        new Workflow()
            .withDocument(
                new Document()
                    .withNamespace("test")
                    .withName("testNonRepeatableResetsBetweenInstances")
                    .withVersion("1.0"))
            .withDo(
                List.of(
                    new TaskItem(
                        "flexible_process",
                        new FlexibleProcess(counterReachedOne, oneShot).asTask())));

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      var definition = app.workflowDefinition(workflow);
      Map<String, Object> result1 =
          definition.instance(Map.of("counter", 0)).start().get().asMap().orElseThrow();

      assertEquals(
          1, result1.get("counter"), "Non-repeatable activity must run once in first instance");

      Map<String, Object> result2 =
          definition.instance(Map.of("counter", 0)).start().get().asMap().orElseThrow();

      assertEquals(
          1,
          result2.get("counter"),
          "Non-repeatable activity must also run once in second instance");
    }
  }

  @Test
  void testNonRepeatableActivityResetsBetweenWorkflowDefinitions()
      throws InterruptedException, ExecutionException {

    Activity oneShot =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer counter = (Integer) map.getOrDefault("counter", 0);
                      map.put("counter", counter + 1);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(false)
            .build();

    Predicate<WorkflowModel> counterReachedOne =
        workflowModel -> {
          Integer counter = (Integer) workflowModel.asMap().get().getOrDefault("counter", 0);
          return counter >= 1;
        };

    FlexibleProcess process = new FlexibleProcess(counterReachedOne, oneShot);

    Workflow workflow =
        FuncWorkflowBuilder.workflow("test-non-repeatable-resets-between-definitions")
            .tasks(process("flexible_process", process))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      Map<String, Object> result1 =
          app.workflowDefinition(workflow)
              .instance(Map.of("counter", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals(1, result1.get("counter"));

      Map<String, Object> result2 =
          app.workflowDefinition(workflow)
              .instance(Map.of("counter", 0))
              .start()
              .get()
              .asMap()
              .orElseThrow();

      assertEquals(1, result2.get("counter"));
    }
  }

  @Test
  void testMaxAttemptsResetsBetweenInstances() throws InterruptedException, ExecutionException {
    Activity iterationCounter =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer iterations = (Integer) map.getOrDefault("iterations", 0);
                      map.put("iterations", iterations + 1);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(true)
            .build();

    Workflow workflow =
        FuncWorkflowBuilder.workflow("testMaxAttemptsResetsBetweenInstances")
            .tasks(
                process(
                    "flexible_process",
                    new FlexibleProcess(workflowModel -> false, iterationCounter)
                        .setMaxAttempts(3)))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      var definition = app.workflowDefinition(workflow);

      Map<String, Object> result1 =
          definition.instance(Map.of()).start().get().asMap().orElseThrow();

      assertEquals(3, result1.get("iterations"), "First instance should run exactly 3 attempts");

      Map<String, Object> result2 =
          definition.instance(Map.of()).start().get().asMap().orElseThrow();

      assertEquals(
          3, result2.get("iterations"), "Second instance should also run exactly 3 attempts");
    }
  }

  @Test
  void testChainedClassifierDoesNotLeakActivityStateBetweenInstances()
      throws InterruptedException, ExecutionException {

    Activity classifier =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      Integer amount = (Integer) map.get("amount");
                      String risk = amount != null && amount >= 100 ? "HIGH" : "LOW";
                      map.put("risk", risk);
                      map.put("classified", true);
                    },
                    Map.class))
            .entryCondition(workflowModel -> true)
            .isRepeatable(false)
            .build();

    Predicate<WorkflowModel> classificationDone =
        workflowModel -> {
          Map<String, Object> data = workflowModel.asMap().get();
          return Boolean.TRUE.equals(data.get("classified"));
        };

    FlexibleProcess classificationProcess = new FlexibleProcess(classificationDone, classifier);

    Activity highRiskHandler =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("highHandled", true);
                      map.put("handled", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  return "HIGH".equals(data.get("risk")) && data.get("handled") == null;
                })
            .isRepeatable(false)
            .build();

    Activity lowRiskHandler =
        Activity.builder()
            .callTask(
                consumer(
                    map -> {
                      map.put("lowHandled", true);
                      map.put("handled", true);
                    },
                    Map.class))
            .entryCondition(
                workflowModel -> {
                  Map<String, Object> data = workflowModel.asMap().get();
                  return "LOW".equals(data.get("risk")) && data.get("handled") == null;
                })
            .isRepeatable(false)
            .build();

    Predicate<WorkflowModel> handlingFinished =
        workflowModel -> {
          Map<String, Object> data = workflowModel.asMap().get();
          return Boolean.TRUE.equals(data.get("handled"));
        };

    FlexibleProcess handlingProcess =
        new FlexibleProcess(handlingFinished, highRiskHandler, lowRiskHandler);

    Workflow workflow =
        FuncWorkflowBuilder.workflow("chained-classifier-handler-nonleaking")
            .tasks(process("classify", classificationProcess), process("handle", handlingProcess))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      var definition = app.workflowDefinition(workflow);

      Map<String, Object> highResult =
          definition.instance(Map.of("amount", 200)).start().get().asMap().orElseThrow();

      assertEquals("HIGH", highResult.get("risk"));
      assertEquals(true, highResult.get("classified"));
      assertEquals(true, highResult.get("handled"));
      assertEquals(true, highResult.get("highHandled"));
      assertFalse(highResult.containsKey("lowHandled"));

      Map<String, Object> lowResult =
          definition.instance(Map.of("amount", 50)).start().get().asMap().orElseThrow();

      assertEquals("LOW", lowResult.get("risk"));
      assertEquals(true, lowResult.get("classified"));
      assertEquals(true, lowResult.get("handled"));
      assertEquals(true, lowResult.get("lowHandled"));
      assertFalse(lowResult.containsKey("highHandled"));
    }
  }
}
