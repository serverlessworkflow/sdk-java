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
package io.serverlessworkflow.fluent.standard;

import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicy;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.OAuth2ConnectAuthenticationProperties;
import io.serverlessworkflow.api.types.Oauth2;
import java.util.function.Consumer;

public final class OAuth2AuthenticationPolicyBuilder
    extends OIDCBuilder<OAuth2AuthenticationPolicy> {

  private final OAuth2ConnectAuthenticationProperties properties;

  OAuth2AuthenticationPolicyBuilder() {
    super();
    this.properties = new OAuth2ConnectAuthenticationProperties();
  }

  public OAuth2AuthenticationPolicyBuilder endpoints(
      Consumer<OAuth2AuthenticationPropertiesEndpointsBuilder> endpointsConsumer) {
    final OAuth2AuthenticationPropertiesEndpointsBuilder builder =
        new OAuth2AuthenticationPropertiesEndpointsBuilder();
    endpointsConsumer.accept(builder);
    this.properties.setEndpoints(builder.build());
    return this;
  }

  public OAuth2AuthenticationPolicy build() {
    final OAuth2AuthenticationPolicyConfiguration configuration =
        new OAuth2AuthenticationPolicyConfiguration();
    configuration.setOAuth2AutenthicationData(this.getAuthenticationData());
    configuration.setOAuth2ConnectAuthenticationProperties(this.properties);

    final Oauth2 oauth2 = new Oauth2();
    oauth2.setOAuth2ConnectAuthenticationProperties(configuration);

    final OAuth2AuthenticationPolicy policy = new OAuth2AuthenticationPolicy();
    policy.setOauth2(oauth2);

    return policy;
  }
}
