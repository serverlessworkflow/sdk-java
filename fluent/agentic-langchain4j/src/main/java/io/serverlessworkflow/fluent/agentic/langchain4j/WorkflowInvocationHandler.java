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
import dev.langchain4j.agentic.cognisphere.CognisphereAccess;
import dev.langchain4j.agentic.cognisphere.CognisphereKey;
import dev.langchain4j.agentic.cognisphere.CognisphereRegistry;
import dev.langchain4j.agentic.internal.AgentInstance;
import dev.langchain4j.agentic.internal.CognisphereOwner;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.lang.reflect.Method;

public class WorkflowInvocationHandler implements CognisphereOwner {

  private final Workflow workflow;
  private final WorkflowApplication.Builder workflowApplicationBuilder;
  private Cognisphere cognisphere;

  WorkflowInvocationHandler(
      Workflow workflow, WorkflowApplication.Builder workflowApplicationBuilder) {
    this.workflow = workflow;
    this.workflowApplicationBuilder = workflowApplicationBuilder;
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
            CognisphereRegistry.get(
                new CognisphereKey(this.workflow.getDocument().getName(), args[0]));
        case "evictCognisphere" ->
            CognisphereRegistry.evict(
                new CognisphereKey(this.workflow.getDocument().getName(), args[0]));
        default ->
            throw new UnsupportedOperationException(
                "Unknown method on CognisphereAccess class : " + method.getName());
      };
    }

    // invoke
    return null;
  }

  @Override
  public CognisphereOwner withCognisphere(Cognisphere cognisphere) {
    this.cognisphere = cognisphere;
    return this;
  }
}
