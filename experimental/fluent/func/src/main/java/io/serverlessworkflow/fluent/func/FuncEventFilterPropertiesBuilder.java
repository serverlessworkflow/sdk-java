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

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.serverlessworkflow.api.types.func.ContextPredicate;
import io.serverlessworkflow.api.types.func.EventDataPredicate;
import io.serverlessworkflow.api.types.func.FilterPredicate;
import io.serverlessworkflow.fluent.spec.AbstractEventPropertiesBuilder;
import java.util.function.Predicate;

public class FuncEventFilterPropertiesBuilder
    extends AbstractEventPropertiesBuilder<FuncEventFilterPropertiesBuilder> {

  @Override
  protected FuncEventFilterPropertiesBuilder self() {
    return this;
  }

  public FuncEventFilterPropertiesBuilder data(Predicate<CloudEventData> predicate) {
    this.eventProperties.setData(
        new EventDataPredicate().withPredicate(predicate, CloudEventData.class));
    return this;
  }

  public FuncEventFilterPropertiesBuilder data(ContextPredicate<CloudEventData> predicate) {
    this.eventProperties.setData(
        new EventDataPredicate().withPredicate(predicate, CloudEventData.class));
    return this;
  }

  public FuncEventFilterPropertiesBuilder data(FilterPredicate<CloudEventData> predicate) {
    this.eventProperties.setData(
        new EventDataPredicate().withPredicate(predicate, CloudEventData.class));
    return this;
  }

  public FuncEventFilterPropertiesBuilder envelope(Predicate<CloudEvent> predicate) {
    this.eventProperties.setData(
        new EventDataPredicate().withPredicate(predicate, CloudEvent.class));
    return this;
  }

  public FuncEventFilterPropertiesBuilder envelope(ContextPredicate<CloudEvent> predicate) {
    this.eventProperties.setData(
        new EventDataPredicate().withPredicate(predicate, CloudEvent.class));
    return this;
  }

  public FuncEventFilterPropertiesBuilder envelope(FilterPredicate<CloudEvent> predicate) {
    this.eventProperties.setData(
        new EventDataPredicate().withPredicate(predicate, CloudEvent.class));
    return this;
  }
}
