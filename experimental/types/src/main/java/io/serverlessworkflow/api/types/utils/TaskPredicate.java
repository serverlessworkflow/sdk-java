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
package io.serverlessworkflow.api.types.utils;

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskMetadata;
import io.serverlessworkflow.api.types.func.ContextPredicate;
import io.serverlessworkflow.api.types.func.FilterPredicate;
import io.serverlessworkflow.api.types.func.TypedContextPredicate;
import io.serverlessworkflow.api.types.func.TypedFilterPredicate;
import io.serverlessworkflow.api.types.func.TypedPredicate;
import java.util.function.Predicate;

public class TaskPredicate {

  private static final String PREDICATE_KEY_PREFIX = "predicate-";

  private TaskPredicate() {}

  public static <T, V extends TaskBase> TaskMetadata withPredicate(
      V task, String name, Predicate<T> predicate) {
    return TypesUtils.initMetadata(task).withAdditionalProperty(concat(name), predicate);
  }

  public static <T, V extends TaskBase> V withPredicate(
      V task, String name, Predicate<T> predicate, Class<T> predicateClass) {
    TypesUtils.initMetadata(task)
        .withAdditionalProperty(
            concat(name),
            predicateClass == null ? predicate : new TypedPredicate<>(predicate, predicateClass));
    return task;
  }

  public static <T, V extends TaskBase> V withPredicate(
      V task, String name, ContextPredicate<T> predicate) {
    TypesUtils.initMetadata(task).withAdditionalProperty(concat(name), predicate);
    return task;
  }

  public static <T, V extends TaskBase> V withPredicate(
      V task, String name, ContextPredicate<T> predicate, Class<T> predicateClass) {
    TypesUtils.initMetadata(task)
        .withAdditionalProperty(
            concat(name),
            predicateClass == null
                ? predicate
                : new TypedContextPredicate<>(predicate, predicateClass));
    return task;
  }

  public static <T, V extends TaskBase> V withPredicate(
      V task, String name, FilterPredicate<T> predicate) {
    TypesUtils.initMetadata(task).withAdditionalProperty(concat(name), predicate);
    return task;
  }

  public static <T, V extends TaskBase> V withPredicate(
      V task, String name, FilterPredicate<T> predicate, Class<T> predicateClass) {
    TypesUtils.initMetadata(task)
        .withAdditionalProperty(
            concat(name),
            predicateClass == null
                ? predicate
                : new TypedFilterPredicate<>(predicate, predicateClass));
    return task;
  }

  public static Object predicate(TaskBase task, String name) {
    return task.getMetadata() != null
        ? task.getMetadata().getAdditionalProperties().get(concat(name))
        : null;
  }

  private static String concat(String name) {
    return PREDICATE_KEY_PREFIX + name;
  }
}
