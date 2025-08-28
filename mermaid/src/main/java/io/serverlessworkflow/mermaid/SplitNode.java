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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Split nodes can have more than one exit point */
public class SplitNode extends Node {

  private final Set<Node> nexts = new HashSet<>();

  public SplitNode(String id, String label) {
    super(id, label, NodeType.SPLIT);
    this.renderer = new SplitNodeRenderer(this);
  }

  public void addNext(Node next) {
    nexts.add(next);
  }

  /**
   * Same as #addNext
   *
   * @param next
   */
  @Override
  public void setNext(Node next) {
    this.addNext(next);
    super.setNext(next);
  }

  /**
   * Gets the first next in the set
   *
   * @return
   */
  @Override
  public Node getNext() {
    if (this.nexts.isEmpty()) {
      return null;
    }
    return this.nexts.iterator().next();
  }

  public Set<Node> getNexts() {
    return Collections.unmodifiableSet(nexts);
  }
}
