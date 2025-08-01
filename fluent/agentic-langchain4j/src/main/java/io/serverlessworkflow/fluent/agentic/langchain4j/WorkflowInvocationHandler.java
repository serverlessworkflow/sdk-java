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
import dev.langchain4j.agentic.cognisphere.CognisphereKey;
import dev.langchain4j.agentic.cognisphere.CognisphereRegistry;
import dev.langchain4j.agentic.cognisphere.ResultWithCognisphere;
import dev.langchain4j.agentic.internal.AgentInstance;
import dev.langchain4j.agentic.internal.AgentSpecification;
import dev.langchain4j.agentic.internal.CognisphereOwner;
import dev.langchain4j.service.MemoryId;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class WorkflowInvocationHandler implements CognisphereOwner {

  private final Workflow workflow;
  private final WorkflowApplication.Builder workflowApplicationBuilder;
  private Cognisphere cognisphere;

  WorkflowInvocationHandler(
      Workflow workflow, WorkflowApplication.Builder workflowApplicationBuilder) {
    this.workflow = workflow;
    this.workflowApplicationBuilder = workflowApplicationBuilder;
  }

  @SuppressWarnings("unchecked")
  private static void writeCognisphereState(Cognisphere cognisphere, Method method, Object[] args) {
    if (method.getDeclaringClass() == UntypedAgent.class) {
      cognisphere.writeStates((Map<String, Object>) args[0]);
    } else {
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        int index = i;
        AgentSpecification.optionalParameterName(parameters[i])
            .ifPresent(argName -> cognisphere.writeState(argName, args[index]));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void writeWorkflowInputState(final Map<String, Object> input, Method method, Object[] args) {
    if (method.getDeclaringClass() == UntypedAgent.class) {
      input.putAll(((Map<String, Object>) args[0]));
    } else {
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        int index = i;
          AgentSpecification.optionalParameterName(parameters[i])
                .ifPresent(argName -> input.put(argName, args[index]));
      }
    }
  }

  private String agentId() {
    return workflow.getDocument().getName();
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // outputName
    if (method.getDeclaringClass() == AgentInstance.class) {
      return switch (method.getName()) {
        case "outputName" ->
            this.workflow
                .getDocument()
                .getMetadata()
                .getAdditionalProperties()
                .get(WorkflowDefinitionBuilder.META_KEY_OUTPUTNAME);
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
        case "withCognisphere" -> this.withCognisphere((Cognisphere) args[0]);
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on CognisphereOwner class : " + method.getName());
      };
    }
    // getCognisphere
    // evictCognisphere
    if (method.getDeclaringClass() == CognisphereAccess.class) {
      return switch (method.getName()) {
        case "getCognisphere" ->
            CognisphereRegistry.get(new CognisphereKey(this.agentId(), args[0]));
        case "evictCognisphere" ->
            CognisphereRegistry.evict(new CognisphereKey(this.agentId(), args[0]));
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on CognisphereAccess class : " + method.getName());
      };
    }

    // invoke
    return executeWorkflow(method, args);
  }

  private Object executeWorkflow(Method method, Object[] args) {
    // TODO: actually, we must own the Cognisphere object creation upon calling the workflow

    //writeCognisphereState(cognisphere, method, args);

    Object input;
    if (args == null || args.length == 0) {
      input = new HashMap<>();
    } else if (args.length == 1) {
      input = args[0];
    } else {
      Map<String, Object> inputMap = new HashMap<>();
      writeWorkflowInputState(inputMap, method, args);
      input = inputMap;
    }

    try (WorkflowApplication app = workflowApplicationBuilder.build()) {
      CompletableFuture<WorkflowModel> workflowInstance = app.workflowDefinition(workflow).instance(input).start();

      if (method.getReturnType().equals(ResultWithCognisphere.class)) {
        return workflowInstance.get().as(ResultWithCognisphere.class);
      } else {
        return workflowInstance.get().asJavaObject();
      }
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(
          "Failed to execute workflow: " + agentId() + " - Cognisphere: " + cognisphere, e);
    }
  }

  @Override
  public CognisphereOwner withCognisphere(Cognisphere cognisphere) {
    this.cognisphere = cognisphere;
    return this;
  }

  private Cognisphere currentCognisphere(Method method, Object[] args) {
    if (cognisphere != null) {
      return cognisphere;
    }

    Object memoryId = memoryId(method, args);
    return memoryId != null
        ? CognisphereRegistry.getOrCreate(new CognisphereKey(this.agentId(), memoryId))
        : CognisphereRegistry.createEphemeralCognisphere();
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
}
