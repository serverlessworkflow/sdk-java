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
package io.serverlessworkflow.fluent.spec.dsl;

import io.serverlessworkflow.fluent.spec.AbstractEventPropertiesBuilder;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class EventFilterSpec<SELF, E extends AbstractEventPropertiesBuilder<?>> {

  private final List<Consumer<E>> steps;

  protected EventFilterSpec(List<Consumer<E>> steps) {
    this.steps = steps;
  }

  protected abstract SELF self();

  protected void addStep(Consumer<E> step) {
    steps.add(step);
  }

  protected List<Consumer<E>> getSteps() {
    return steps;
  }

  public SELF type(String eventType) {
    steps.add(e -> e.type(eventType));
    return self();
  }

  /** Sets the CloudEvent id to a random UUID */
  public SELF randomId() {
    steps.add(e -> e.id(UUID.randomUUID().toString()));
    return self();
  }

  /** Sets the CloudEvent time to the current system time */
  public SELF now() {
    steps.add(e -> e.time(Date.from(Instant.now())));
    return self();
  }

  public SELF contentType(String ct) {
    steps.add(e -> e.dataContentType(ct));
    return self();
  }

  /** Sets the CloudEvent dataContentType to `application/json` */
  public SELF JSON() {
    steps.add(e -> e.dataContentType("application/json"));
    return self();
  }

  public SELF OCTET_STREAM() {
    steps.add(e -> e.dataContentType("application/octet-stream"));
    return self();
  }

  public SELF source(String source) {
    steps.add(e -> e.source(source));
    return self();
  }

  public SELF source(URI source) {
    steps.add(e -> e.source(source));
    return self();
  }
}
