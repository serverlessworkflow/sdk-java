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
package io.serverlessworkflow.mermaid;

public enum NodeType {
  RECT("rect"),
  STOP("stop"),
  SUBGRAPH("subgraph"),
  TRY_CATCH("subgraph"),
  TRY_BLOCK("subgraph"),
  NOTE("note"),
  SPLIT("sm-circ"),
  START("sm-circ"),
  EVENT("rounded"),
  EMIT("lean-r"),
  JUNCTION("f-circ");

  private final String type;

  NodeType(String type) {
    this.type = type;
  }

  public String mermaidShape() {
    return this.type;
  }
}
