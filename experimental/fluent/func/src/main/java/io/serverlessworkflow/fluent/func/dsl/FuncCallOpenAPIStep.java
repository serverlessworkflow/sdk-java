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
package io.serverlessworkflow.fluent.func.dsl;

import io.serverlessworkflow.api.types.OpenAPIArguments;
import io.serverlessworkflow.fluent.func.FuncCallOpenAPITaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.spec.configurers.AuthenticationConfigurer;
import io.serverlessworkflow.fluent.spec.spi.CallOpenAPITaskFluent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FuncCallOpenAPIStep extends Step<FuncCallOpenAPIStep, FuncCallOpenAPITaskBuilder> {

  private final List<Consumer<CallOpenAPITaskFluent<?>>> steps = new ArrayList<>();

  private String name;

  public FuncCallOpenAPIStep(String name) {
    this.name = name;
  }

  public FuncCallOpenAPIStep() {}

  public void setName(String name) {
    this.name = name;
  }

  public FuncCallOpenAPIStep document(String uri) {
    steps.add(b -> b.document(uri));
    return this;
  }

  public FuncCallOpenAPIStep document(
      String uri, AuthenticationConfigurer authenticationConfigurer) {
    steps.add(b -> b.document(uri, authenticationConfigurer));
    return this;
  }

  public FuncCallOpenAPIStep document(URI uri) {
    steps.add(b -> b.document(uri));
    return this;
  }

  public FuncCallOpenAPIStep document(URI uri, AuthenticationConfigurer authenticationConfigurer) {
    steps.add(b -> b.document(uri, authenticationConfigurer));
    return this;
  }

  public FuncCallOpenAPIStep operation(String operationId) {
    steps.add(b -> b.operation(operationId));
    return this;
  }

  public FuncCallOpenAPIStep parameters(Map<String, Object> params) {
    steps.add(b -> b.parameters(params));
    return this;
  }

  public FuncCallOpenAPIStep parameter(String name, String value) {
    steps.add(b -> b.parameter(name, value));
    return this;
  }

  public FuncCallOpenAPIStep redirect(boolean redirect) {
    steps.add(b -> b.redirect(redirect));
    return this;
  }

  public FuncCallOpenAPIStep authentication(AuthenticationConfigurer authenticationConfigurer) {
    steps.add(b -> b.authentication(authenticationConfigurer));
    return this;
  }

  public FuncCallOpenAPIStep output(OpenAPIArguments.WithOpenAPIOutput output) {
    steps.add(b -> b.output(output));
    return this;
  }

  @Override
  protected void configure(
      FuncTaskItemListBuilder list, Consumer<FuncCallOpenAPITaskBuilder> post) {
    list.openapi(
        name,
        builder -> {
          for (Consumer<CallOpenAPITaskFluent<?>> c : steps) {
            c.accept(builder);
          }
          post.accept(builder);
        });
  }
}
