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

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.Workflow;

public record WorkflowId(String namespace, String name, String version) {

  public static WorkflowId of(Workflow workflow) {
    Document document = workflow.getDocument();
    return new WorkflowId(document.getNamespace(), document.getName(), document.getVersion());
  }

  public String identifier() {
    return identifier(namespace, name, version);
  }

  public static String asString(Workflow workflow) {
    Document document = workflow.getDocument();
    return identifier(document.getNamespace(), document.getName(), document.getVersion());
  }

  private static String identifier(String namespace, String name, String version) {
    return namespace + "-" + name + "-" + version;
  }
}
