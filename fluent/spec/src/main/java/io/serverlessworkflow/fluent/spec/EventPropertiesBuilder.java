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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.EventData;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.api.types.EventSource;
import io.serverlessworkflow.api.types.EventTime;
import io.serverlessworkflow.api.types.UriTemplate;
import java.net.URI;
import java.util.Date;

public final class EventPropertiesBuilder {
  private final EventProperties properties = new EventProperties();

  public EventPropertiesBuilder id(String id) {
    properties.setId(id);
    return this;
  }

  public EventPropertiesBuilder source(String expr) {

    properties.setSource(new EventSource().withRuntimeExpression(expr));
    return this;
  }

  public EventPropertiesBuilder source(URI uri) {
    properties.setSource(new EventSource().withUriTemplate(new UriTemplate().withLiteralUri(uri)));
    return this;
  }

  public EventPropertiesBuilder type(String type) {
    properties.setType(type);
    return this;
  }

  public EventPropertiesBuilder time(Date time) {
    properties.setTime(new EventTime().withLiteralTime(time));
    return this;
  }

  public EventPropertiesBuilder subject(String subject) {
    properties.setSubject(subject);
    return this;
  }

  public EventPropertiesBuilder dataContentType(String ct) {
    properties.setDatacontenttype(ct);
    return this;
  }

  public EventPropertiesBuilder data(String expr) {
    properties.setData(new EventData().withRuntimeExpression(expr));
    return this;
  }

  public EventPropertiesBuilder data(Object obj) {
    properties.setData(new EventData().withObject(obj));
    return this;
  }

  public EventProperties build() {
    return properties;
  }
}
