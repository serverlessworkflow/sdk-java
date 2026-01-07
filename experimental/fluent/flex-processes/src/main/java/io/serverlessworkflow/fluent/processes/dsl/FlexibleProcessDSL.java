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
package io.serverlessworkflow.fluent.processes.dsl;

import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.fluent.func.configurers.FuncTaskConfigurer;
import io.serverlessworkflow.fluent.processes.Activity;
import io.serverlessworkflow.fluent.processes.FlexibleProcess;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * DSL helper class providing fluent API methods for creating and configuring FlexibleProcess and
 * Activity instances in workflows.
 *
 * <p>This class offers convenient static methods to build workflow components with minimal
 * boilerplate code, supporting both explicit and lambda-based configuration.
 */
public class FlexibleProcessDSL {

  /**
   * Creates a FuncTaskConfigurer for a FlexibleProcess defined inline by an exit condition and a
   * sequence of activities, using an auto-generated unique task name.
   *
   * <p>This is a convenience method that allows defining a process without explicitly creating a
   * {@link FlexibleProcess} instance. A random UUID is used as the task name.
   *
   * <p>Use this method when:
   *
   * <ul>
   *   <li>the process task does not need a meaningful name
   *   <li>the process is defined inline as part of a workflow definition
   * </ul>
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Workflow workflow = FuncWorkflowBuilder.workflow("sample-workflow")
   *     .tasks(
   *         process(
   *             model -> (boolean) model.asMap().get().get("done"),
   *             activity1,
   *             activity2
   *         )
   *     )
   *     .build();
   * }</pre>
   *
   * @param exitCondition predicate that determines when the process execution should terminate
   * @param activities activities to be executed as part of the process
   * @return a FuncTaskConfigurer that adds the process as a task with a generated name
   * @throws NullPointerException if exitCondition or activities is null
   */
  public static FuncTaskConfigurer process(
      Predicate<WorkflowModel> exitCondition, Activity... activities) {
    return process(UUID.randomUUID().toString(), exitCondition, activities);
  }

  /**
   * Creates a FuncTaskConfigurer for a FlexibleProcess defined inline by an exit condition and a
   * sequence of activities, using the specified task name.
   *
   * <p>This method is useful when you want to define a process declaratively while still assigning
   * a stable, meaningful name to the resulting workflow task (for debugging, monitoring, or
   * referencing in workflow logic).
   *
   * <p>This is functionally equivalent to creating a {@link FlexibleProcess} explicitly and passing
   * it to {@link #process(String, FlexibleProcess)}, but with reduced boilerplate.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Workflow workflow = FuncWorkflowBuilder.workflow("order-processing")
   *     .tasks(
   *         process(
   *             "validation-process",
   *             model -> (boolean) model.asMap().get().get("validated"),
   *             validateActivity,
   *             enrichActivity
   *         )
   *     )
   *     .build();
   * }</pre>
   *
   * @param name the name to assign to the process task (must not be null)
   * @param exitCondition predicate that determines when the process execution should terminate
   * @param activities activities to be executed as part of the process
   * @return a FuncTaskConfigurer that adds the named process as a task
   * @throws NullPointerException if name, exitCondition, or activities is null
   */
  public static FuncTaskConfigurer process(
      String name, Predicate<WorkflowModel> exitCondition, Activity... activities) {
    return process(name, new FlexibleProcess(exitCondition, activities));
  }

  /**
   * Creates a FuncTaskConfigurer for a FlexibleProcess with an auto-generated unique name.
   *
   * <p>This is a convenience method that generates a random UUID-based name for the process task.
   * Use this when the task name is not important for your workflow logic.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * FlexibleProcess myProcess = FlexibleProcess.builder()
   *     .exitCondition(model -> ...)
   *     .activities(activity1, activity2)
   *     .build();
   *
   * Workflow workflow = FuncWorkflowBuilder.workflow("my-workflow")
   *     .tasks(process(myProcess))
   *     .build();
   * }</pre>
   *
   * @param process the FlexibleProcess to be executed as a workflow task
   * @return a FuncTaskConfigurer that adds the process as a task with a generated name
   * @throws NullPointerException if process is null
   * @see #process(String, FlexibleProcess)
   */
  public static FuncTaskConfigurer process(FlexibleProcess process) {
    return process(UUID.randomUUID().toString(), process);
  }

  /**
   * Creates a FuncTaskConfigurer for a FlexibleProcess with a specified name.
   *
   * <p>This method allows you to assign a meaningful name to the process task, which can be useful
   * for debugging, monitoring, or when you need to reference this specific task in your workflow
   * logic.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * FlexibleProcess validationProcess = FlexibleProcess.builder()
   *     .exitCondition(model -> (boolean) model.asMap().get().get("validated"))
   *     .activities(validateActivity)
   *     .build();
   *
   * Workflow workflow = FuncWorkflowBuilder.workflow("order-processing")
   *     .tasks(
   *         process("validate-order", validationProcess),
   *         process("fulfill-order", fulfillmentProcess)
   *     )
   *     .build();
   * }</pre>
   *
   * @param name the name to assign to this process task (must not be null)
   * @param process the FlexibleProcess to be executed as a workflow task
   * @return a FuncTaskConfigurer that adds the named process as a task
   * @throws NullPointerException if either name or process is null
   */
  public static FuncTaskConfigurer process(String name, FlexibleProcess process) {
    Objects.requireNonNull(process, "FlexibleProcess cannot be null");
    return list -> list.addTaskItem(new TaskItem(name, process.asTask()));
  }

  /**
   * Creates an Activity using a builder lambda for inline configuration.
   *
   * <p>This method provides a fluent way to define activities without explicitly using the
   * Activity.builder() pattern. The builder instance is created and configured through the provided
   * consumer lambda.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Activity incrementer = activity(builder -> builder
   *     .name("increment-counter")
   *     .callTask(consumer(map -> {
   *         int value = (int) map.get("counter");
   *         map.put("counter", value + 1);
   *     }, Map.class))
   *     .entryCondition(model -> true)
   *     .isRepeatable(true)
   * );
   *
   * // Use in a FlexibleProcess
   * FlexibleProcess process = FlexibleProcess.builder()
   *     .exitCondition(model -> (int) model.asMap().get().get("counter") >= 10)
   *     .activities(incrementer)
   *     .build();
   * }</pre>
   *
   * <p>This is particularly useful when defining activities inline within a FlexibleProcess:
   *
   * <pre>{@code
   * FlexibleProcess process = FlexibleProcess.builder()
   *     .exitCondition(done)
   *     .activities(
   *         activity(b -> b.callTask(task1).entryCondition(cond1).isRepeatable(true)),
   *         activity(b -> b.callTask(task2).entryCondition(cond2).isRepeatable(false))
   *     )
   *     .build();
   * }</pre>
   *
   * @param builder a consumer that configures the Activity.ActivityBuilder
   * @return a fully configured Activity instance
   * @throws NullPointerException if builder is null
   * @throws NullPointerException if required fields (task, entryCondition) are not set in the
   *     builder
   */
  public static Activity activity(Consumer<Activity.ActivityBuilder> builder) {
    Activity.ActivityBuilder builderInstance = Activity.builder();
    builder.accept(builderInstance);
    return builderInstance.build();
  }
}
