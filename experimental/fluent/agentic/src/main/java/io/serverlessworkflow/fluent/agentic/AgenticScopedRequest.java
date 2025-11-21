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
package io.serverlessworkflow.fluent.agentic;

import dev.langchain4j.agentic.agent.AgentRequest;
import dev.langchain4j.agentic.internal.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import java.util.List;
import java.util.Map;

public class AgenticScopedRequest implements AgenticScope {

  protected final String agentName;
  private final AgenticScope wrapped;

  AgenticScopedRequest(AgenticScope wrapped, String agentName) {
    this.wrapped = wrapped;
    this.agentName = agentName;
  }

  @Override
  public Object memoryId() {
    return wrapped.memoryId();
  }

  @Override
  public void writeState(String s, Object o) {
    wrapped.writeState(s, o);
  }

  @Override
  public void writeStates(Map<String, Object> map) {
    wrapped.writeStates(map);
  }

  @Override
  public boolean hasState(String s) {
    return wrapped.hasState(s);
  }

  @Override
  public Object readState(String s) {
    return wrapped.readState(s);
  }

  @Override
  public <T> T readState(String s, T t) {
    return wrapped.readState(s, t);
  }

  @Override
  public Map<String, Object> state() {
    return wrapped.state();
  }

  @Override
  public String contextAsConversation(String... strings) {
    return wrapped.contextAsConversation(strings);
  }

  @Override
  public String contextAsConversation(Object... objects) {
    return wrapped.contextAsConversation(objects);
  }

  @Override
  public List<AgentInvocation> agentInvocations(String s) {
    return wrapped.agentInvocations(s);
  }

  public AgentRequest asAgentRequest() {
    return new AgentRequest(this, agentName, state());
  }
}
