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

import dev.langchain4j.agentic.cognisphere.Cognisphere;
import dev.langchain4j.agentic.internal.AgentInstance;
import dev.langchain4j.agentic.internal.CognisphereOwner;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.agentic.AgentWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractAgentService<T, S> implements WorkflowDefinitionBuilder {

  // Workflow OutputAs
  private static final Function<Cognisphere, Object> DEFAULT_OUTPUT_FUNCTION = cognisphere -> null;
  // Workflow InputFrom
  private static final Consumer<Cognisphere> DEFAULT_INPUT_FUNCTION = cognisphere -> {};

  protected final WorkflowApplication.Builder workflowExecBuilder;
  protected final AgentWorkflowBuilder workflowBuilder;
  protected final Class<T> agentServiceClass;

  protected AbstractAgentService(Class<T> agentServiceClass) {
    this("", agentServiceClass);
  }

  protected AbstractAgentService(String name, Class<T> agentServiceClass) {
    this.workflowBuilder =
        AgentWorkflowBuilder.workflow(name)
            .outputAs(DEFAULT_OUTPUT_FUNCTION)
            .input(i -> i.from(DEFAULT_INPUT_FUNCTION));
    this.agentServiceClass = agentServiceClass;
    this.workflowExecBuilder = WorkflowApplication.builder();
  }

  @SuppressWarnings("unchecked")
  public T build() {
    return (T)
        Proxy.newProxyInstance(
            this.agentServiceClass.getClassLoader(),
            new Class<?>[] {agentServiceClass, AgentInstance.class, CognisphereOwner.class},
            new WorkflowInvocationHandler(this.workflowBuilder.build(), this.workflowExecBuilder));
  }

  @SuppressWarnings("unchecked")
  public S beforeCall(Consumer<Cognisphere> beforeCall) {
    // TODO: Our runner must know that our input can be a cognisphere object and extract it from the
    // context, so that we can accept the consumer.
    // TODO: For now, we can add the consumer to inputFrom
    this.workflowBuilder.input(i -> i.from(beforeCall));
    return (S) this;
  }

  @SuppressWarnings("unchecked")
  public S outputName(String outputName) {
    Function<Cognisphere, Object> outputFunction = cog -> cog.readState(outputName);
    this.workflowBuilder.outputAs(outputFunction);
    this.workflowBuilder.document(
        d -> d.metadata(m -> m.metadata(META_KEY_OUTPUTNAME, outputName)));
    return (S) this;
  }

  @SuppressWarnings("unchecked")
  public S output(Function<Cognisphere, Object> output) {
    this.workflowBuilder.outputAs(output);
    return (S) this;
  }

  @Override
  public Workflow getDefinition() {
    return this.workflowBuilder.build();
  }
}
