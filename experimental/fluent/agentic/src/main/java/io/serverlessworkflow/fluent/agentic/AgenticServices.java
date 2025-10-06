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

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.V;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AgenticServices<T> {

  private final Class<T> agent;

  private AgentWorkflowBuilder builder;

  private AgenticServices(Class<T> agent) {
    this.agent = agent;
  }

  public static <T> AgenticServices<T> of(Class<T> agent) {
    return new AgenticServices<>(agent);
  }

  public AgenticServices<T> flow(AgentWorkflowBuilder builder) {
    this.builder = builder;
    return this;
  }

  public T build() {
    Objects.requireNonNull(
        builder, "AgenticServices.flow(AgentWorkflowBuilder) must be called before build()");
    Workflow workflow = builder.build();
    return AgenticServiceBuilder.create(agent, new AgentInvocationHandler(workflow));
  }

  private static class AgenticServiceBuilder {

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> runner, InvocationHandler h) {
      if (!runner.isInterface()) {
        throw new IllegalArgumentException(runner + " must be an interface to create a Proxy");
      }

      ClassLoader cl = runner.getClassLoader();
      Class<?>[] ifaces = new Class<?>[] {runner};
      return (T) Proxy.newProxyInstance(cl, ifaces, h);
    }
  }

  private class AgentInvocationHandler implements InvocationHandler {

    private final Workflow workflow;

    public AgentInvocationHandler(Workflow workflow) {
      this.workflow = workflow;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      if (method.getDeclaringClass() == Object.class) {
        return switch (method.getName()) {
          case "toString" -> "AgentProxy(" + workflow.getDocument().getName() + ")";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> throw new IllegalStateException("Unexpected Object method: " + method);
        };
      }

      Agent agent = method.getAnnotation(Agent.class);
      if (agent == null) {
        throw new IllegalStateException(
            "Method " + method.getName() + " is not annotated with @Agent");
      }

      Annotation[][] annotations = method.getParameterAnnotations();
      Map<String, Object> input = new HashMap<>();
      for (int i = 0; i < annotations.length; i++) {
        boolean found = false;
        for (Annotation a : annotations[i]) {
          if (a instanceof V) {
            String key = ((V) a).value();
            Object value = args[i];
            input.put(key, value);
            found = true;
            break;
          }
        }
        if (!found) {
          throw new IllegalStateException(
              "Parameter "
                  + (i + 1)
                  + " of method "
                  + method.getName()
                  + " is not annotated with @V");
        }
      }

      try (WorkflowApplication app = WorkflowApplication.builder().build()) {
        WorkflowModel result = app.workflowDefinition(workflow).instance(input).start().get();
        if (result.asJavaObject() instanceof AgenticScope scope) {
          Object out = scope.state().get("input");
          if (out != null) {
            return out;
          }
        }
        return result.asJavaObject();
      } catch (Exception e) {
        throw new RuntimeException("Workflow execution failed", e);
      }
    }
  }
}
