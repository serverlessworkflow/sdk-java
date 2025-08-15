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
package io.serverlessworkflow.impl.expressions.agentic.langchain4j;

import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticScopeRegistry;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class AgenticScopeRegistryAssessor implements AgenticScopeOwner {

  private final AtomicReference<AgenticScopeRegistry> agenticScopeRegistry =
      new AtomicReference<>();
  private final String agentId;
  private AgenticScope agenticScope;
  private Object memoryId;

  public AgenticScopeRegistryAssessor(String agentId) {
    Objects.requireNonNull(agentId, "Agent id cannot be null");
    this.agentId = agentId;
  }

  // TODO: have access to the workflow definition and assign its name instead
  public AgenticScopeRegistryAssessor() {
    this.agentId = UUID.randomUUID().toString();
  }

  public void setMemoryId(Object memoryId) {
    this.memoryId = memoryId;
  }

  public AgenticScope getAgenticScope() {
    if (agenticScope != null) {
      return agenticScope;
    }

    if (memoryId != null) {
      this.agenticScope = registry().getOrCreate(memoryId);
    } else {
      this.agenticScope = registry().createEphemeralAgenticScope();
    }
    return this.agenticScope;
  }

  public void setAgenticScope(AgenticScope agenticScope) {
    this.agenticScope = Objects.requireNonNull(agenticScope, "AgenticScope cannot be null");
  }

  public void writeState(String key, Object value) {
    this.getAgenticScope().writeState(key, value);
  }

  public void writeStates(Map<String, Object> states) {
    this.getAgenticScope().writeStates(states);
  }

  @Override
  public AgenticScopeOwner withAgenticScope(DefaultAgenticScope agenticScope) {
    this.setAgenticScope(agenticScope);
    return this;
  }

  @Override
  public AgenticScopeRegistry registry() {
    agenticScopeRegistry.compareAndSet(null, new AgenticScopeRegistry(agentId));
    return agenticScopeRegistry.get();
  }
}
