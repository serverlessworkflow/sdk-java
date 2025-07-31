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
import dev.langchain4j.agentic.cognisphere.Cognisphere;
import dev.langchain4j.agentic.cognisphere.CognisphereAccess;
import dev.langchain4j.agentic.cognisphere.CognisphereRegistry;
import dev.langchain4j.agentic.cognisphere.DefaultCognisphere;
import dev.langchain4j.agentic.cognisphere.ResultWithCognisphere;
import dev.langchain4j.agentic.internal.AgentInvoker;
import dev.langchain4j.agentic.internal.AgentSpecification;
import dev.langchain4j.agentic.internal.CognisphereOwner;
import dev.langchain4j.service.MemoryId;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.expressions.agentic.langchain4j.CognisphereRegistryAssessor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class WorkflowInvocationHandler implements InvocationHandler, CognisphereOwner {

  private final Workflow workflow;
  private final WorkflowApplication.Builder workflowApplicationBuilder;
  private final CognisphereRegistryAssessor cognisphereRegistryAssessor;

  WorkflowInvocationHandler(
      Workflow workflow,
      WorkflowApplication.Builder workflowApplicationBuilder,
      Class<?> agentServiceClass) {
    this.workflow = workflow;
    this.workflowApplicationBuilder = workflowApplicationBuilder;
    this.cognisphereRegistryAssessor = new CognisphereRegistryAssessor(agentServiceClass.getName());
  }

  @SuppressWarnings("unchecked")
  private static void writeCognisphereState(Cognisphere cognisphere, Method method, Object[] args) {
    if (method.getDeclaringClass() == UntypedAgent.class) {
      cognisphere.writeStates((Map<String, Object>) args[0]);
    } else {
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        int index = i;
        AgentInvoker.optionalParameterName(parameters[i])
            .ifPresent(argName -> cognisphere.writeState(argName, args[index]));
      }
    }
  }

  private String outputName() {
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
    CognisphereRegistry registry = registry();
    // outputName
    if (method.getDeclaringClass() == AgentSpecification.class) {
      return switch (method.getName()) {
        case "outputName" -> outputName();
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on AgentInstance class : " + method.getName());
      };
    }
    // withCognisphere
    if (method.getDeclaringClass() == CognisphereOwner.class) {
      // Ingest the workflow input as a Cognisphere object
      // Later, retrieve it and start the workflow with it as input.
      return switch (method.getName()) {
        case "withCognisphere" -> this.withCognisphere((DefaultCognisphere) args[0]);
        case "registry" -> registry;
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on CognisphereOwner class : " + method.getName());
      };
    }
    // getCognisphere
    // evictCognisphere
    if (method.getDeclaringClass() == CognisphereAccess.class) {
      return switch (method.getName()) {
        case "getCognisphere" -> registry().get(args[0]);
        case "evictCognisphere" -> registry().evict(args[0]);
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on CognisphereAccess class : " + method.getName());
      };
    }

    // invoke
    return executeWorkflow(currentCognisphere(method, args), method, args);
  }

  private Object executeWorkflow(DefaultCognisphere cognisphere, Method method, Object[] args) {
    writeCognisphereState(cognisphere, method, args);

    try (WorkflowApplication app = workflowApplicationBuilder.build()) {
      // TODO improve result handling
      DefaultCognisphere output =
          app.workflowDefinition(workflow)
              .instance(cognisphere)
              .start()
              .get()
              .as(DefaultCognisphere.class)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Workflow hasn't returned a Cognisphere object."));
      Object result = output.readState(outputName());

      return method.getReturnType().equals(ResultWithCognisphere.class)
          ? new ResultWithCognisphere<>(output, result)
          : result;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(
          "Failed to execute workflow: "
              + workflow.getDocument().getName()
              + " - Cognisphere: "
              + cognisphere,
          e);
    }
  }

  private DefaultCognisphere currentCognisphere(Method method, Object[] args) {
    Object memoryId = memoryId(method, args);
    this.cognisphereRegistryAssessor.setMemoryId(memoryId);
    return this.cognisphereRegistryAssessor.getCognisphere();
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
  public CognisphereOwner withCognisphere(DefaultCognisphere cognisphere) {
    this.cognisphereRegistryAssessor.withCognisphere(cognisphere);
    return this;
  }

  @Override
  public CognisphereRegistry registry() {
    return this.cognisphereRegistryAssessor.registry();
  }
}
