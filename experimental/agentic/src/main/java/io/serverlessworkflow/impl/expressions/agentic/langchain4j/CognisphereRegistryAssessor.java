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
package io.serverlessworkflow.impl.expressions.agentic.langchain4j;

import dev.langchain4j.agentic.cognisphere.CognisphereRegistry;
import dev.langchain4j.agentic.cognisphere.DefaultCognisphere;
import dev.langchain4j.agentic.internal.CognisphereOwner;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class CognisphereRegistryAssessor implements CognisphereOwner {

  private final AtomicReference<CognisphereRegistry> cognisphereRegistry = new AtomicReference<>();
  private final String agentId;
  private DefaultCognisphere cognisphere;
  private Object memoryId;

  public CognisphereRegistryAssessor(String agentId) {
    Objects.requireNonNull(agentId, "Agent id cannot be null");
    this.agentId = agentId;
  }

  // TODO: have access to the workflow definition and assign its name instead
  public CognisphereRegistryAssessor() {
    this.agentId = UUID.randomUUID().toString();
  }

  public void setMemoryId(Object memoryId) {
    this.memoryId = memoryId;
  }

  public DefaultCognisphere getCognisphere() {
    if (cognisphere != null) {
      return cognisphere;
    }

    if (memoryId != null) {
      this.cognisphere = registry().getOrCreate(memoryId);
    } else {
      this.cognisphere = registry().createEphemeralCognisphere();
    }
    return this.cognisphere;
  }

  @Override
  public CognisphereOwner withCognisphere(DefaultCognisphere cognisphere) {
    this.cognisphere = cognisphere;
    return this;
  }

  @Override
  public CognisphereRegistry registry() {
    cognisphereRegistry.compareAndSet(null, new CognisphereRegistry(agentId));
    return cognisphereRegistry.get();
  }
}
