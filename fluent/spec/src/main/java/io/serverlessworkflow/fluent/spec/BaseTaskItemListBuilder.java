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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A builder for an ordered {@link TaskItem} list.
 *
 * <p>This builder only knows how to append new TaskItems of various flavors, but does NOT expose
 * {@link TaskBase}‑level methods like export(), input(), etc. Those belong on {@link
 * TaskBaseBuilder} subclasses.
 *
 * @param <SELF> the concrete builder type
 */
public abstract class BaseTaskItemListBuilder<SELF extends BaseTaskItemListBuilder<SELF>> {

  public final String TYPE_SET = "set";
  public final String TYPE_FOR = "for";
  public final String TYPE_SWITCH = "switch";
  public final String TYPE_RAISE = "raise";
  public final String TYPE_FORK = "fork";
  public final String TYPE_LISTEN = "listen";
  public final String TYPE_EMIT = "emit";
  public final String TYPE_TRY = "try";
  public final String TYPE_HTTP = "http";
  public final String TYPE_OPENAPI = "openapi";

  private final List<TaskItem> list;

  public BaseTaskItemListBuilder() {
    this.list = new ArrayList<>();
  }

  public BaseTaskItemListBuilder(final List<TaskItem> list) {
    this.list = list;
  }

  protected abstract SELF self();

  protected abstract SELF newItemListBuilder();

  protected final List<TaskItem> mutableList() {
    return this.list;
  }

  protected final SELF addTaskItem(TaskItem taskItem) {
    Objects.requireNonNull(taskItem, "taskItem must not be null");
    list.add(taskItem);
    return self();
  }

  protected final String defaultNameAndRequireConfig(
      String name, Consumer<?> cfg, String taskType) {
    Objects.requireNonNull(cfg, "Configurer must not be null");

    if (name == null || name.isBlank()) {
      return taskType + "-" + this.list.size();
    }

    return name;
  }

  /**
   * @return an immutable snapshot of all {@link TaskItem}s added so far
   */
  public List<TaskItem> build() {
    return Collections.unmodifiableList(list);
  }
}
