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

import io.serverlessworkflow.api.types.AuthenticationPolicy;
import io.serverlessworkflow.api.types.OAuth2AutenthicationData;
import io.serverlessworkflow.api.types.OAuth2AutenthicationDataClient;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPropertiesEndpoints;
import io.serverlessworkflow.api.types.OAuth2TokenDefinition;
import io.serverlessworkflow.api.types.OAuth2TokenRequest;
import java.util.List;
import java.util.function.Consumer;

public abstract class OIDCBuilder<T extends AuthenticationPolicy> {
  private final OAuth2AutenthicationData authenticationData;

  OIDCBuilder() {
    this.authenticationData = new OAuth2AutenthicationData();
    this.authenticationData.setRequest(new OAuth2TokenRequest());
  }

  public OIDCBuilder<T> authority(String authority) {
    this.authenticationData.setAuthority(UriTemplateBuilder.newUriTemplate(authority));
    return this;
  }

  public OIDCBuilder<T> grant(OAuth2AutenthicationData.OAuth2AutenthicationDataGrant grant) {
    this.authenticationData.setGrant(grant);
    return this;
  }

  public OIDCBuilder<T> issuers(String... issuers) {
    if (issuers != null) {
      this.authenticationData.setIssuers(List.of(issuers));
    }
    return this;
  }

  public OIDCBuilder<T> scopes(String... scopes) {
    if (scopes != null) {
      this.authenticationData.setScopes(List.of(scopes));
    }
    return this;
  }

  public OIDCBuilder<T> audiences(String... audiences) {
    if (audiences != null) {
      this.authenticationData.setAudiences(List.of(audiences));
    }
    return this;
  }

  public OIDCBuilder<T> username(String username) {
    this.authenticationData.setUsername(username);
    return this;
  }

  public OIDCBuilder<T> password(String password) {
    this.authenticationData.setPassword(password);
    return this;
  }

  public OIDCBuilder<T> requestEncoding(OAuth2TokenRequest.Oauth2TokenRequestEncoding encoding) {
    this.authenticationData.setRequest(new OAuth2TokenRequest().withEncoding(encoding));
    return this;
  }

  public OIDCBuilder<T> subject(Consumer<OAuth2TokenDefinitionBuilder> subjectConsumer) {
    final OAuth2TokenDefinitionBuilder builder = new OAuth2TokenDefinitionBuilder();
    subjectConsumer.accept(builder);
    this.authenticationData.setSubject(builder.build());
    return this;
  }

  public OIDCBuilder<T> actor(Consumer<OAuth2TokenDefinitionBuilder> actorConsumer) {
    final OAuth2TokenDefinitionBuilder builder = new OAuth2TokenDefinitionBuilder();
    actorConsumer.accept(builder);
    this.authenticationData.setActor(builder.build());
    return this;
  }

  public OIDCBuilder<T> client(Consumer<OAuth2AuthenticationDataClientBuilder> clientConsumer) {
    final OAuth2AuthenticationDataClientBuilder builder =
        new OAuth2AuthenticationDataClientBuilder();
    clientConsumer.accept(builder);
    this.authenticationData.setClient(builder.build());
    return this;
  }

  protected final OAuth2AutenthicationData getAuthenticationData() {
    return authenticationData;
  }

  public abstract T build();

  public static final class OAuth2TokenDefinitionBuilder {
    private final OAuth2TokenDefinition oauth2TokenDefinition;

    OAuth2TokenDefinitionBuilder() {
      this.oauth2TokenDefinition = new OAuth2TokenDefinition();
    }

    public OAuth2TokenDefinitionBuilder token(String token) {
      this.oauth2TokenDefinition.setToken(token);
      return this;
    }

    public OAuth2TokenDefinitionBuilder type(String type) {
      this.oauth2TokenDefinition.setType(type);
      return this;
    }

    public OAuth2TokenDefinition build() {
      return this.oauth2TokenDefinition;
    }
  }

  public static final class OAuth2AuthenticationDataClientBuilder {
    private final OAuth2AutenthicationDataClient client;

    OAuth2AuthenticationDataClientBuilder() {
      this.client = new OAuth2AutenthicationDataClient();
    }

    public OAuth2AuthenticationDataClientBuilder id(String id) {
      this.client.setId(id);
      return this;
    }

    public OAuth2AuthenticationDataClientBuilder secret(String secret) {
      this.client.setSecret(secret);
      return this;
    }

    public OAuth2AuthenticationDataClientBuilder assertion(String assertion) {
      this.client.setAssertion(assertion);
      return this;
    }

    public OAuth2AuthenticationDataClientBuilder authentication(
        OAuth2AutenthicationDataClient.ClientAuthentication authentication) {
      this.client.setAuthentication(authentication);
      return this;
    }

    public OAuth2AutenthicationDataClient build() {
      return this.client;
    }
  }

  public static final class OAuth2AuthenticationPropertiesEndpointsBuilder {
    private final OAuth2AuthenticationPropertiesEndpoints endpoints;

    OAuth2AuthenticationPropertiesEndpointsBuilder() {
      endpoints = new OAuth2AuthenticationPropertiesEndpoints();
    }

    public OAuth2AuthenticationPropertiesEndpointsBuilder token(String token) {
      this.endpoints.setToken(token);
      return this;
    }

    public OAuth2AuthenticationPropertiesEndpointsBuilder revocation(String revocation) {
      this.endpoints.setRevocation(revocation);
      return this;
    }

    public OAuth2AuthenticationPropertiesEndpointsBuilder introspection(String introspection) {
      this.endpoints.setIntrospection(introspection);
      return this;
    }

    public OAuth2AuthenticationPropertiesEndpoints build() {
      return this.endpoints;
    }
  }
}
