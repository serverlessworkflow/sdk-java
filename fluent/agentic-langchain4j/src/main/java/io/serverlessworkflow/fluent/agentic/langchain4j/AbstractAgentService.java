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
package io.serverlessworkflow.fluent.agentic.langchain4j;

import dev.langchain4j.agentic.agent.ErrorContext;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.internal.AgentSpecification;
import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.agentic.AgentWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractAgentService<T, S> implements WorkflowDefinitionBuilder {

  // Workflow OutputAs
  private static final Function<AgenticScope, Object> DEFAULT_OUTPUT_FUNCTION =
      agenticScope -> null;

  protected final WorkflowApplication.Builder workflowExecBuilder;
  protected final AgentWorkflowBuilder workflowBuilder;
  protected final Class<T> agentServiceClass;

  protected AbstractAgentService(Class<T> agentServiceClass) {
    this.workflowBuilder = AgentWorkflowBuilder.workflow().outputAs(DEFAULT_OUTPUT_FUNCTION);
    this.agentServiceClass = agentServiceClass;
    this.workflowExecBuilder = WorkflowApplication.builder();
  }

  @SuppressWarnings("unchecked")
  public T build() {
    return (T)
        Proxy.newProxyInstance(
            this.agentServiceClass.getClassLoader(),
            new Class<?>[] {agentServiceClass, AgentSpecification.class, AgenticScopeOwner.class},
            new WorkflowInvocationHandler(
                this.workflowBuilder.build(), this.workflowExecBuilder, this.agentServiceClass));
  }

  @SuppressWarnings("unchecked")
  public S beforeCall(Consumer<AgenticScope> beforeCall) {
    this.workflowBuilder.inputFrom(
        cog -> {
          beforeCall.accept(cog);
          return cog;
        },
        AgenticScope.class);
    return (S) this;
  }

  @SuppressWarnings("unchecked")
  public S outputName(String outputName) {
    Function<DefaultAgenticScope, Object> outputFunction = cog -> cog.readState(outputName);
    this.workflowBuilder.outputAs(outputFunction, DefaultAgenticScope.class);
    this.workflowBuilder.document(
        d -> d.metadata(m -> m.metadata(META_KEY_OUTPUTNAME, outputName)));
    return (S) this;
  }

  @SuppressWarnings("unchecked")
  public S output(Function<AgenticScope, Object> output) {
    this.workflowBuilder.outputAs(output, AgenticScope.class);
    return (S) this;
  }

  @SuppressWarnings("unchecked")
  public S errorHandler(Function<ErrorContext, ErrorRecoveryResult> errorHandler) {
    return (S) this;
  }

  @Override
  public Workflow getDefinition() {
    return this.workflowBuilder.build();
  }
}
