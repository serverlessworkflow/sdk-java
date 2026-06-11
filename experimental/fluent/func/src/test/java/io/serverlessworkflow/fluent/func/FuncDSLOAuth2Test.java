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
package io.serverlessworkflow.fluent.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.call;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.http;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.oauth2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.OAuth2AuthenticationDataClient;
import io.serverlessworkflow.api.types.OAuth2ConnectAuthenticationProperties;
import io.serverlessworkflow.api.types.Workflow;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class FuncDSLOAuth2Test {

  private static final String EXPR_ENDPOINT = "${ .endpoint }";

  private static OAuth2ConnectAuthenticationProperties oauth2PropertiesOf(Workflow wf) {
    var auth =
        wf.getDo()
            .get(0)
            .getTask()
            .getCallTask()
            .getCallHTTP()
            .getWith()
            .getEndpoint()
            .getEndpointConfiguration()
            .getAuthentication()
            .getAuthenticationPolicy();
    assertNotNull(auth.getOAuth2AuthenticationPolicy());
    return auth.getOAuth2AuthenticationPolicy()
        .getOauth2()
        .getOAuth2ConnectAuthenticationProperties();
  }

  @Test
  void convenience_overload_sets_token_endpoint() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("oauth2-token")
            .tasks(
                call(
                    http()
                        .POST()
                        .endpoint(
                            EXPR_ENDPOINT,
                            oauth2(
                                "https://auth.example.com/",
                                OAuth2AuthenticationData.OAuth2AuthenticationDataGrant
                                    .CLIENT_CREDENTIALS,
                                "client-id",
                                "client-secret",
                                e -> e.token("/custom/token")))))
            .build();

    var props = oauth2PropertiesOf(wf);
    assertEquals(URI.create("https://auth.example.com/"), props.getAuthority().getLiteralUri());
    assertEquals(
        OAuth2AuthenticationData.OAuth2AuthenticationDataGrant.CLIENT_CREDENTIALS,
        props.getGrant());
    assertEquals("client-id", props.getClient().getId());
    assertEquals("client-secret", props.getClient().getSecret());
    assertEquals("/custom/token", props.getEndpoints().getToken());
  }

  @Test
  void builder_overload_supports_full_oauth2_section() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("oauth2-full")
            .tasks(
                call(
                    http()
                        .GET()
                        .endpoint(
                            EXPR_ENDPOINT,
                            oauth2(
                                o ->
                                    o.endpoints(
                                            e ->
                                                e.token("/oauth2/token")
                                                    .revocation("/oauth2/revoke")
                                                    .introspection("/oauth2/introspect"))
                                        .authority("https://auth.example.com/")
                                        .grant(
                                            OAuth2AuthenticationData.OAuth2AuthenticationDataGrant
                                                .CLIENT_CREDENTIALS)
                                        .scopes("read", "write")
                                        .audiences("api://default")
                                        .client(
                                            c ->
                                                c.id("client-id")
                                                    .secret("client-secret")
                                                    .authentication(
                                                        OAuth2AuthenticationDataClient
                                                            .ClientAuthentication
                                                            .CLIENT_SECRET_BASIC))))))
            .build();

    var props = oauth2PropertiesOf(wf);
    assertEquals(URI.create("https://auth.example.com/"), props.getAuthority().getLiteralUri());
    assertEquals(
        OAuth2AuthenticationData.OAuth2AuthenticationDataGrant.CLIENT_CREDENTIALS,
        props.getGrant());
    assertEquals(List.of("read", "write"), props.getScopes());
    assertEquals(List.of("api://default"), props.getAudiences());
    assertEquals("client-id", props.getClient().getId());
    assertEquals(
        OAuth2AuthenticationDataClient.ClientAuthentication.CLIENT_SECRET_BASIC,
        props.getClient().getAuthentication());
    assertEquals("/oauth2/token", props.getEndpoints().getToken());
    assertEquals("/oauth2/revoke", props.getEndpoints().getRevocation());
    assertEquals("/oauth2/introspect", props.getEndpoints().getIntrospection());
  }
}
