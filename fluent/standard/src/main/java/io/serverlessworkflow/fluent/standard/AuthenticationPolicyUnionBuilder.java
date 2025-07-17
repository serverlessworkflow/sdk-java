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

import io.serverlessworkflow.api.types.AuthenticationPolicyUnion;
import java.util.function.Consumer;

public class AuthenticationPolicyUnionBuilder {
  final AuthenticationPolicyUnion authenticationPolicy;

  AuthenticationPolicyUnionBuilder() {
    this.authenticationPolicy = new AuthenticationPolicyUnion();
  }

  public AuthenticationPolicyUnionBuilder basic(
      Consumer<BasicAuthenticationPolicyBuilder> basicConsumer) {
    final BasicAuthenticationPolicyBuilder basicAuthenticationPolicyBuilder =
        new BasicAuthenticationPolicyBuilder();
    basicConsumer.accept(basicAuthenticationPolicyBuilder);
    this.authenticationPolicy.setBasicAuthenticationPolicy(
        basicAuthenticationPolicyBuilder.build());
    return this;
  }

  public AuthenticationPolicyUnionBuilder bearer(
      Consumer<BearerAuthenticationPolicyBuilder> bearerConsumer) {
    final BearerAuthenticationPolicyBuilder bearerAuthenticationPolicyBuilder =
        new BearerAuthenticationPolicyBuilder();
    bearerConsumer.accept(bearerAuthenticationPolicyBuilder);
    this.authenticationPolicy.setBearerAuthenticationPolicy(
        bearerAuthenticationPolicyBuilder.build());
    return this;
  }

  public AuthenticationPolicyUnionBuilder digest(
      Consumer<DigestAuthenticationPolicyBuilder> digestConsumer) {
    final DigestAuthenticationPolicyBuilder digestAuthenticationPolicyBuilder =
        new DigestAuthenticationPolicyBuilder();
    digestConsumer.accept(digestAuthenticationPolicyBuilder);
    this.authenticationPolicy.setDigestAuthenticationPolicy(
        digestAuthenticationPolicyBuilder.build());
    return this;
  }

  public AuthenticationPolicyUnionBuilder oauth2(
      Consumer<OAuth2AuthenticationPolicyBuilder> oauth2Consumer) {
    final OAuth2AuthenticationPolicyBuilder oauth2AuthenticationPolicyBuilder =
        new OAuth2AuthenticationPolicyBuilder();
    oauth2Consumer.accept(oauth2AuthenticationPolicyBuilder);
    this.authenticationPolicy.setOAuth2AuthenticationPolicy(
        oauth2AuthenticationPolicyBuilder.build());
    return this;
  }

  public AuthenticationPolicyUnionBuilder openIDConnect(
      Consumer<OpenIdConnectAuthenticationPolicyBuilder> openIdConnectConsumer) {
    final OpenIdConnectAuthenticationPolicyBuilder builder =
        new OpenIdConnectAuthenticationPolicyBuilder();
    openIdConnectConsumer.accept(builder);
    this.authenticationPolicy.setOpenIdConnectAuthenticationPolicy(builder.build());
    return this;
  }

  public AuthenticationPolicyUnion build() {
    return authenticationPolicy;
  }
}
