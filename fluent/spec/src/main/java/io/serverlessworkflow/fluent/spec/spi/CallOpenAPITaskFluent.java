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
package io.serverlessworkflow.fluent.spec.spi;

import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointConfiguration;
import io.serverlessworkflow.api.types.EndpointUri;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.OpenAPIArguments;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.fluent.spec.ReferenceableAuthenticationPolicyBuilder;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import io.serverlessworkflow.fluent.spec.configurers.AuthenticationConfigurer;
import java.net.URI;
import java.util.Map;

public interface CallOpenAPITaskFluent<SELF extends TaskBaseBuilder<SELF>> {

  default CallOpenAPI build() {
    final CallOpenAPI task = ((CallOpenAPI) this.self().getTask());
    if (task.getWith().getOutput() == null) {
      task.getWith().setOutput(OpenAPIArguments.WithOpenAPIOutput.CONTENT);
    }
    return task;
  }

  SELF self();

  default SELF document(String uri) {
    ((CallOpenAPI) this.self().getTask())
        .getWith()
        .withDocument(
            new ExternalResource().withEndpoint(new Endpoint().withRuntimeExpression(uri)));
    return self();
  }

  default SELF document(URI uri) {
    ((CallOpenAPI) this.self().getTask())
        .getWith()
        .withDocument(
            new ExternalResource()
                .withEndpoint(
                    new Endpoint().withUriTemplate(new UriTemplate().withLiteralUri(uri))));
    return self();
  }

  default SELF document(String uri, AuthenticationConfigurer authenticationConfigurer) {
    final ReferenceableAuthenticationPolicyBuilder policy =
        new ReferenceableAuthenticationPolicyBuilder();
    authenticationConfigurer.accept(policy);
    ((CallOpenAPI) this.self().getTask()).getWith().setAuthentication(policy.build());
    ((CallOpenAPI) this.self().getTask())
        .getWith()
        .setDocument(
            new ExternalResource()
                .withEndpoint(
                    new Endpoint()
                        .withRuntimeExpression(uri)
                        .withEndpointConfiguration(
                            new EndpointConfiguration()
                                .withUri(new EndpointUri().withExpressionEndpointURI(uri))
                                .withAuthentication(policy.build()))));
    return self();
  }

  default SELF document(URI uri, AuthenticationConfigurer authenticationConfigurer) {
    final ReferenceableAuthenticationPolicyBuilder policy =
        new ReferenceableAuthenticationPolicyBuilder();
    authenticationConfigurer.accept(policy);

    ((CallOpenAPI) this.self().getTask()).getWith().setAuthentication(policy.build());
    ((CallOpenAPI) this.self().getTask())
        .getWith()
        .setDocument(
            new ExternalResource()
                .withEndpoint(
                    new Endpoint()
                        .withUriTemplate(new UriTemplate().withLiteralUri(uri))
                        .withEndpointConfiguration(
                            new EndpointConfiguration()
                                .withUri(
                                    new EndpointUri()
                                        .withLiteralEndpointURI(
                                            new UriTemplate().withLiteralUri(uri)))
                                .withAuthentication(policy.build()))));
    return self();
  }

  default SELF operation(String operation) {
    ((CallOpenAPI) this.self().getTask()).getWith().setOperationId(operation);
    return self();
  }

  default SELF parameters(Map<String, Object> parameters) {
    ((CallOpenAPI) this.self().getTask())
        .getWith()
        .getParameters()
        .getAdditionalProperties()
        .putAll(parameters);
    return self();
  }

  default SELF parameter(String name, String value) {
    ((CallOpenAPI) this.self().getTask())
        .getWith()
        .getParameters()
        .getAdditionalProperties()
        .put(name, value);
    return self();
  }

  default SELF authentication(AuthenticationConfigurer authenticationConfigurer) {
    final ReferenceableAuthenticationPolicyBuilder policy =
        new ReferenceableAuthenticationPolicyBuilder();
    authenticationConfigurer.accept(policy);
    ((CallOpenAPI) this.self().getTask()).getWith().setAuthentication(policy.build());
    return self();
  }

  default SELF output(OpenAPIArguments.WithOpenAPIOutput output) {
    ((CallOpenAPI) this.self().getTask()).getWith().setOutput(output);
    return self();
  }

  default SELF redirect(boolean redirect) {
    ((CallOpenAPI) this.self().getTask()).getWith().setRedirect(redirect);
    return self();
  }
}
