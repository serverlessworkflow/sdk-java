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
package io.serverlessworkflow.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.serverlessworkflow.api.types.AuthenticationPolicyUnion;
import io.serverlessworkflow.api.types.BasicAuthenticationPolicy;
import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicy;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.OAuth2ConnectAuthenticationProperties;
import io.serverlessworkflow.api.types.OpenIdConnectAuthenticationPolicy;
import io.serverlessworkflow.api.types.OpenIdConnectAuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.SecretBasedAuthenticationPolicy;
import io.serverlessworkflow.impl.auth.OAuthUtils;
import io.serverlessworkflow.impl.auth.OAuthUtils.OAuthPolicyData;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class OAuthUtilsTest {

  @Test
  void fromNullReturnsEmpty() {
    assertEquals(Optional.empty(), OAuthUtils.from(null));
  }

  @Test
  void fromNonOAuthPolicyReturnsEmpty() {
    AuthenticationPolicyUnion union =
        new AuthenticationPolicyUnion()
            .withBasicAuthenticationPolicy(new BasicAuthenticationPolicy());
    assertTrue(OAuthUtils.from(union).isEmpty());
  }

  @Test
  void fromOAuth2InlineData() {
    OAuth2ConnectAuthenticationProperties props = new OAuth2ConnectAuthenticationProperties();
    AuthenticationPolicyUnion union =
        new AuthenticationPolicyUnion()
            .withOAuth2AuthenticationPolicy(
                new OAuth2AuthenticationPolicy()
                    .withOauth2(
                        new OAuth2AuthenticationPolicyConfiguration()
                            .withOAuth2ConnectAuthenticationProperties(props)));
    Optional<OAuthPolicyData> result = OAuthUtils.from(union);
    assertTrue(result.isPresent());
    OAuthPolicyData data = result.get();
    assertEquals(OAuthUtils.OAuthScheme.OAUTH2, data.scheme());
    assertEquals(props, data.data());
    assertNull(data.secret());
  }

  @Test
  void fromOAuth2Secret() {
    SecretBasedAuthenticationPolicy secret = new SecretBasedAuthenticationPolicy("mySecret");
    AuthenticationPolicyUnion union =
        new AuthenticationPolicyUnion()
            .withOAuth2AuthenticationPolicy(
                new OAuth2AuthenticationPolicy()
                    .withOauth2(
                        new OAuth2AuthenticationPolicyConfiguration()
                            .withOAuth2AuthenticationPolicySecret(secret)));
    Optional<OAuthPolicyData> result = OAuthUtils.from(union);
    assertTrue(result.isPresent());
    OAuthPolicyData data = result.get();
    assertEquals(OAuthUtils.OAuthScheme.OAUTH2, data.scheme());
    assertNull(data.data());
    assertEquals(secret, data.secret());
  }

  @Test
  void fromOidcInlineData() {
    OAuth2AuthenticationData oidcData = new OAuth2AuthenticationData();
    AuthenticationPolicyUnion union =
        new AuthenticationPolicyUnion()
            .withOpenIdConnectAuthenticationPolicy(
                new OpenIdConnectAuthenticationPolicy()
                    .withOidc(
                        new OpenIdConnectAuthenticationPolicyConfiguration()
                            .withOpenIdConnectAuthenticationProperties(oidcData)));
    Optional<OAuthPolicyData> result = OAuthUtils.from(union);
    assertTrue(result.isPresent());
    OAuthPolicyData data = result.get();
    assertEquals(OAuthUtils.OAuthScheme.OPENID_CONNECT, data.scheme());
    assertEquals(oidcData, data.data());
    assertNull(data.secret());
  }

  @Test
  void fromOidcSecret() {
    SecretBasedAuthenticationPolicy secret = new SecretBasedAuthenticationPolicy("oidcSecret");
    AuthenticationPolicyUnion union =
        new AuthenticationPolicyUnion()
            .withOpenIdConnectAuthenticationPolicy(
                new OpenIdConnectAuthenticationPolicy()
                    .withOidc(
                        new OpenIdConnectAuthenticationPolicyConfiguration()
                            .withOpenIdConnectAuthenticationPolicySecret(secret)));
    Optional<OAuthPolicyData> result = OAuthUtils.from(union);
    assertTrue(result.isPresent());
    OAuthPolicyData data = result.get();
    assertEquals(OAuthUtils.OAuthScheme.OPENID_CONNECT, data.scheme());
    assertNull(data.data());
    assertEquals(secret, data.secret());
  }
}
