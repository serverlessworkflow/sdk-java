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
package io.serverless.workflow.impl.executors.func;

import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JavaFunctions {

  static Person personPojo(String name) {
    return new Person(name + " Javierito", 23);
  }

  static String getName(Person person) {
    return person.name() + " Javierito";
  }

  static CompletableFuture<String> getNameFuture(Person person) {
    return CompletableFuture.completedFuture(getName(person));
  }

  static CompletableFuture<StringBuilder> getNameStringBuilder(Person person) {
    StringBuilder sb = new StringBuilder(person.name());
    sb.append(" Javierito");
    return CompletableFuture.completedFuture(sb);
  }

  static String getFilterName(
      Person person, WorkflowContextData workflowContext, TaskContextData taskContext) {
    return person.name() + "_" + workflowContext.instanceData().id() + "_" + taskContext.taskName();
  }

  static String getContextName(Person person, WorkflowContextData workflowContext) {
    return person.name() + "_" + workflowContext.instanceData().id();
  }

  static Map<String, Object> addJavierito(Map<String, Object> map) {
    return Map.of("name", map.get("name") + " Javierito");
  }

  static String addJavieritoString(String value) {
    return value + " Javierito";
  }
}
