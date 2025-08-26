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
package io.serverlessworkflow.api;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.BearerAuthenticationPolicy;
import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.OAuth2AuthenticationData.OAuth2AuthenticationDataGrant;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicy;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPropertiesEndpoints;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.Workflow;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class ApiTest {

  @Test
  void testCallHTTPAPI() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("features/callHttp.yaml");
    assertThat(workflow.getDo()).isNotEmpty();
    assertThat(workflow.getDo().get(0).getName()).isNotNull();
    assertThat(workflow.getDo().get(0).getTask()).isNotNull();
    Task task = workflow.getDo().get(0).getTask();
    if (task.get() instanceof CallTask) {
      CallTask callTask = task.getCallTask();
      assertThat(callTask).isNotNull();
      assertThat(task.getDoTask()).isNull();
      CallHTTP httpCall = callTask.getCallHTTP();
      assertThat(httpCall).isNotNull();
      assertThat(callTask.getCallAsyncAPI()).isNull();
      HTTPArguments httpParams = httpCall.getWith();
      assertThat(httpParams.getMethod()).isEqualTo("get");
      assertThat(
              httpParams
                  .getEndpoint()
                  .getEndpointConfiguration()
                  .getUri()
                  .getLiteralEndpointURI()
                  .getLiteralUriTemplate())
          .isEqualTo("https://petstore.swagger.io/v2/pet/{petId}");
    }
  }

  @Test
  void testCallFunctionAPIWithoutArguments() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("features/callFunction.yaml");
    assertThat(workflow.getDo()).isNotEmpty();
    assertThat(workflow.getDo().get(0).getName()).isNotNull();
    assertThat(workflow.getDo().get(0).getTask()).isNotNull();
    Task task = workflow.getDo().get(0).getTask();
    CallTask callTask = task.getCallTask();
    assertThat(callTask).isNotNull();
    assertThat(callTask.get()).isInstanceOf(CallFunction.class);
    CallFunction functionCall = callTask.getCallFunction();
    assertThat(functionCall).isNotNull();
    assertThat(callTask.getCallAsyncAPI()).isNull();
    assertThat(functionCall.getWith()).isNull();
  }

  @Test
  void testOauth2Auth() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("features/authentication-oauth2.yaml");
    assertThat(workflow.getDo()).isNotEmpty();
    assertThat(workflow.getDo().get(0).getName()).isNotNull();
    assertThat(workflow.getDo().get(0).getTask()).isNotNull();
    Task task = workflow.getDo().get(0).getTask();
    CallTask callTask = task.getCallTask();
    assertThat(callTask).isNotNull();
    assertThat(callTask.get()).isInstanceOf(CallHTTP.class);
    CallHTTP httpCall = callTask.getCallHTTP();
    OAuth2AuthenticationPolicy oauthPolicy =
        httpCall
            .getWith()
            .getEndpoint()
            .getEndpointConfiguration()
            .getAuthentication()
            .getAuthenticationPolicy()
            .getOAuth2AuthenticationPolicy();
    assertThat(oauthPolicy).isNotNull();
    OAuth2AuthenticationPolicyConfiguration oauth2Props =
        oauthPolicy.getOauth2().getOAuth2ConnectAuthenticationProperties();
    assertThat(oauth2Props).isNotNull();
    OAuth2AuthenticationPropertiesEndpoints endpoints =
        oauth2Props.getOAuth2ConnectAuthenticationProperties().getEndpoints();
    assertThat(endpoints.getToken()).isEqualTo("/auth/token");
    assertThat(endpoints.getIntrospection()).isEqualTo("/auth/introspect");

    OAuth2AuthenticationData oauth2Data = oauth2Props.getOAuth2AuthenticationData();
    assertThat(oauth2Data.getAuthority().getLiteralUri())
        .isEqualTo(URI.create("http://keycloak/realms/fake-authority"));
    assertThat(oauth2Data.getGrant()).isEqualTo(OAuth2AuthenticationDataGrant.CLIENT_CREDENTIALS);
    assertThat(oauth2Data.getClient().getId()).isEqualTo("workflow-runtime-id");
    assertThat(oauth2Data.getClient().getSecret()).isEqualTo("workflow-runtime-secret");
  }

  @Test
  void testBearerAuth() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("features/authentication-bearer.yaml");
    assertThat(workflow.getDo()).isNotEmpty();
    assertThat(workflow.getDo().get(0).getName()).isNotNull();
    assertThat(workflow.getDo().get(0).getTask()).isNotNull();
    Task task = workflow.getDo().get(0).getTask();
    CallTask callTask = task.getCallTask();
    assertThat(callTask).isNotNull();
    assertThat(callTask.get()).isInstanceOf(CallHTTP.class);
    CallHTTP httpCall = callTask.getCallHTTP();
    BearerAuthenticationPolicy bearerPolicy =
        httpCall
            .getWith()
            .getEndpoint()
            .getEndpointConfiguration()
            .getAuthentication()
            .getAuthenticationPolicy()
            .getBearerAuthenticationPolicy();
    assertThat(bearerPolicy).isNotNull();
    assertThat(bearerPolicy.getBearer().getBearerAuthenticationProperties().getToken())
        .isEqualTo("${ .token }");
  }
}
