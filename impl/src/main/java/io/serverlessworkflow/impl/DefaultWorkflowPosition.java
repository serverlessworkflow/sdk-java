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

public class DefaultWorkflowPosition implements WorkflowPosition {

  private StringBuilder sb = new StringBuilder("");

  @Override
  public WorkflowPosition addIndex(int index) {
    sb.append('/').append(index);
    return this;
  }

  @Override
  public WorkflowPosition addProperty(String prop) {
    sb.append('/').append(prop);
    return this;
  }

  @Override
  public String jsonPointer() {
    return sb.toString();
  }

  @Override
  public String toString() {
    return "DefaultWorkflowPosition [sb=" + sb + "]";
  }

  @Override
  public WorkflowPosition back() {
    int indexOf = sb.lastIndexOf("/");
    if (indexOf != -1) {
      sb.substring(0, indexOf);
    }
    return this;
  }
}
