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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.serverlessworkflow.api.types.AuthenticationPolicyReference;
import io.serverlessworkflow.api.types.AuthenticationPolicyUnion;
import io.serverlessworkflow.api.types.BearerAuthenticationPolicy;
import io.serverlessworkflow.api.types.BearerAuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.BearerAuthenticationProperties;
import io.serverlessworkflow.api.types.ReferenceableAuthenticationPolicy;
import io.serverlessworkflow.api.types.Use;
import io.serverlessworkflow.api.types.UseAuthentications;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.auth.DefaultAuthProviderFactory;
import org.junit.jupiter.api.Test;

public class ResolvePolicyTest {

  private static final AuthenticationPolicyUnion BEARER_POLICY =
      new AuthenticationPolicyUnion()
          .withBearerAuthenticationPolicy(
              new BearerAuthenticationPolicy(
                  new BearerAuthenticationPolicyConfiguration()
                      .withBearerAuthenticationProperties(
                          new BearerAuthenticationProperties("test-token"))));

  @Test
  void nullAuthReturnsNull() {
    assertNull(DefaultAuthProviderFactory.resolvePolicy(new Workflow(), null));
  }

  @Test
  void inlinePolicyReturnsPolicyDirectly() {
    ReferenceableAuthenticationPolicy auth =
        new ReferenceableAuthenticationPolicy().withAuthenticationPolicy(BEARER_POLICY);
    assertEquals(BEARER_POLICY, DefaultAuthProviderFactory.resolvePolicy(new Workflow(), auth));
  }

  @Test
  void referenceResolvesFromWorkflowUseAuthentications() {
    Workflow workflow =
        new Workflow()
            .withUse(
                new Use()
                    .withAuthentications(
                        new UseAuthentications().withAdditionalProperty("myAuth", BEARER_POLICY)));
    ReferenceableAuthenticationPolicy auth =
        new ReferenceableAuthenticationPolicy()
            .withAuthenticationPolicyReference(new AuthenticationPolicyReference("myAuth"));
    assertEquals(BEARER_POLICY, DefaultAuthProviderFactory.resolvePolicy(workflow, auth));
  }

  @Test
  void referenceWithNullWorkflowThrows() {
    ReferenceableAuthenticationPolicy auth =
        new ReferenceableAuthenticationPolicy()
            .withAuthenticationPolicyReference(new AuthenticationPolicyReference("myAuth"));
    assertThrows(
        IllegalArgumentException.class, () -> DefaultAuthProviderFactory.resolvePolicy(null, auth));
  }

  @Test
  void referenceWithNullUseReturnsNull() {
    ReferenceableAuthenticationPolicy auth =
        new ReferenceableAuthenticationPolicy()
            .withAuthenticationPolicyReference(new AuthenticationPolicyReference("myAuth"));
    assertNull(DefaultAuthProviderFactory.resolvePolicy(new Workflow(), auth));
  }

  @Test
  void referenceWithNullAuthenticationsReturnsNull() {
    Workflow workflow = new Workflow().withUse(new Use());
    ReferenceableAuthenticationPolicy auth =
        new ReferenceableAuthenticationPolicy()
            .withAuthenticationPolicyReference(new AuthenticationPolicyReference("myAuth"));
    assertNull(DefaultAuthProviderFactory.resolvePolicy(workflow, auth));
  }

  @Test
  void referenceToNonExistentKeyReturnsNull() {
    Workflow workflow =
        new Workflow()
            .withUse(
                new Use()
                    .withAuthentications(
                        new UseAuthentications()
                            .withAdditionalProperty("otherAuth", BEARER_POLICY)));
    ReferenceableAuthenticationPolicy auth =
        new ReferenceableAuthenticationPolicy()
            .withAuthenticationPolicyReference(new AuthenticationPolicyReference("myAuth"));
    assertNull(DefaultAuthProviderFactory.resolvePolicy(workflow, auth));
  }
}
