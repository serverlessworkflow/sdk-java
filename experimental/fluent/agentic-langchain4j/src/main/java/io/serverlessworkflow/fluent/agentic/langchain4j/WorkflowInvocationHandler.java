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

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.internal.AgentInvoker;
import dev.langchain4j.agentic.internal.AgentSpecification;
import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.agentic.scope.AgenticScopeRegistry;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.MemoryId;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.model.agentic.langchain4j.AgenticScopeRegistryAssessor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class WorkflowInvocationHandler implements InvocationHandler, AgenticScopeOwner {

  private final Workflow workflow;
  private final WorkflowApplication.Builder workflowApplicationBuilder;
  private final AgenticScopeRegistryAssessor agenticScopeRegistryAssessor;

  WorkflowInvocationHandler(
      Workflow workflow,
      WorkflowApplication.Builder workflowApplicationBuilder,
      Class<?> agentServiceClass) {
    this.workflow = workflow;
    this.workflowApplicationBuilder = workflowApplicationBuilder;
    this.agenticScopeRegistryAssessor =
        new AgenticScopeRegistryAssessor(agentServiceClass.getName());
  }

  @SuppressWarnings("unchecked")
  private static void writeAgenticScopeState(
      AgenticScope agenticScope, Method method, Object[] args) {
    if (method.getDeclaringClass() == UntypedAgent.class) {
      agenticScope.writeStates((Map<String, Object>) args[0]);
    } else {
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        int index = i;
        AgentInvoker.optionalParameterName(parameters[i])
            .ifPresent(argName -> agenticScope.writeState(argName, args[index]));
      }
    }
  }

  private String outputKey() {
    Object outputName =
        this.workflow
            .getDocument()
            .getMetadata()
            .getAdditionalProperties()
            .get(WorkflowDefinitionBuilder.META_KEY_OUTPUTNAME);
    if (outputName != null) {
      return outputName.toString();
    }
    return null;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    AgenticScopeRegistry registry = registry();
    // outputKey
    if (method.getDeclaringClass() == AgentSpecification.class) {
      return switch (method.getName()) {
        case "name" -> this.workflow.getDocument().getName();
        case "description" -> this.workflow.getDocument().getSummary();
        case "outputKey" -> outputKey();
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on AgentInstance class : " + method.getName());
      };
    }
    // withAgenticScope
    if (method.getDeclaringClass() == AgenticScopeOwner.class) {
      // Ingest the workflow input as a AgenticScope object
      // Later, retrieve it and start the workflow with it as input.
      return switch (method.getName()) {
        case "withAgenticScope" -> this.withAgenticScope((DefaultAgenticScope) args[0]);
        case "registry" -> registry;
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on AgenticScopeOwner class : " + method.getName());
      };
    }
    // getAgenticScope
    // evictAgenticScope
    if (method.getDeclaringClass() == AgenticScopeAccess.class) {
      return switch (method.getName()) {
        case "getAgenticScope" -> registry().get(args[0]);
        case "evictAgenticScope" -> registry().evict(args[0]);
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on CognisphereAccess class : " + method.getName());
      };
    }

    // invoke
    return executeWorkflow(currentAgenticScope(method, args), method, args);
  }

  private Object executeWorkflow(AgenticScope agenticScope, Method method, Object[] args) {
    writeAgenticScopeState(agenticScope, method, args);

    try (WorkflowApplication app = workflowApplicationBuilder.build()) {
      // TODO improve result handling
      AgenticScope output =
          app.workflowDefinition(workflow)
              .instance(agenticScope)
              .start()
              .get()
              .as(AgenticScope.class)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Workflow hasn't returned a AgenticScope object."));
      Object result = output.readState(outputKey());

      return method.getReturnType().equals(ResultWithAgenticScope.class)
          ? new ResultWithAgenticScope<>(output, result)
          : result;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(
          "Failed to execute workflow: "
              + workflow.getDocument().getName()
              + " - AgenticScope: "
              + agenticScope,
          e);
    }
  }

  private AgenticScope currentAgenticScope(Method method, Object[] args) {
    Object memoryId = memoryId(method, args);
    this.agenticScopeRegistryAssessor.setMemoryId(memoryId);
    return this.agenticScopeRegistryAssessor.getAgenticScope();
  }

  private Object memoryId(Method method, Object[] args) {
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getAnnotation(MemoryId.class) != null) {
        return args[i];
      }
    }
    return null;
  }

  @Override
  public AgenticScopeOwner withAgenticScope(DefaultAgenticScope agenticScope) {
    this.agenticScopeRegistryAssessor.withAgenticScope(agenticScope);
    return this;
  }

  @Override
  public AgenticScopeRegistry registry() {
    return this.agenticScopeRegistryAssessor.registry();
  }
}
