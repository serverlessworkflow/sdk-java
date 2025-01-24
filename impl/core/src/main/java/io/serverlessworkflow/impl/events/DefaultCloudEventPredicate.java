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
package io.serverlessworkflow.impl.events;

import com.fasterxml.jackson.databind.JsonNode;
import io.cloudevents.CloudEvent;
import io.serverlessworkflow.api.types.EventData;
import io.serverlessworkflow.api.types.EventDataschema;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.api.types.EventSource;
import io.serverlessworkflow.api.types.EventTime;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

public class DefaultCloudEventPredicate implements CloudEventPredicate {

  private final CloudEventAttrPredicate<String> idFilter;
  private final CloudEventAttrPredicate<URI> sourceFilter;
  private final CloudEventAttrPredicate<String> subjectFilter;
  private final CloudEventAttrPredicate<String> contentTypeFilter;
  private final CloudEventAttrPredicate<String> typeFilter;
  private final CloudEventAttrPredicate<URI> dataSchemaFilter;
  private final CloudEventAttrPredicate<OffsetDateTime> timeFilter;
  private final CloudEventAttrPredicate<JsonNode> dataFilter;
  private final CloudEventAttrPredicate<JsonNode> additionalFilter;

  private static final <T> CloudEventAttrPredicate<T> isTrue() {
    return x -> true;
  }

  public DefaultCloudEventPredicate(EventProperties properties, ExpressionFactory exprFactory) {
    idFilter = stringFilter(properties.getId());
    subjectFilter = stringFilter(properties.getSubject());
    typeFilter = stringFilter(properties.getType());
    contentTypeFilter = stringFilter(properties.getDatacontenttype());
    sourceFilter = sourceFilter(properties.getSource(), exprFactory);
    dataSchemaFilter = dataSchemaFilter(properties.getDataschema(), exprFactory);
    timeFilter = offsetTimeFilter(properties.getTime(), exprFactory);
    dataFilter = dataFilter(properties.getData(), exprFactory);
    additionalFilter = additionalFilter(properties.getAdditionalProperties(), exprFactory);
  }

  private CloudEventAttrPredicate<JsonNode> additionalFilter(
      Map<String, Object> additionalProperties, ExpressionFactory exprFactory) {
    return additionalProperties != null && !additionalProperties.isEmpty()
        ? from(WorkflowUtils.buildWorkflowFilter(exprFactory, null, additionalProperties))
        : isTrue();
  }

  private CloudEventAttrPredicate<JsonNode> from(WorkflowFilter filter) {
    return d -> filter.apply(null, null, d).asBoolean();
  }

  private CloudEventAttrPredicate<JsonNode> dataFilter(
      EventData data, ExpressionFactory exprFactory) {
    return data != null
        ? from(
            WorkflowUtils.buildWorkflowFilter(
                exprFactory, data.getRuntimeExpression(), data.getObject()))
        : isTrue();
  }

  private CloudEventAttrPredicate<OffsetDateTime> offsetTimeFilter(
      EventTime time, ExpressionFactory exprFactory) {
    if (time != null) {
      if (time.getRuntimeExpression() != null) {
        final Expression expr = exprFactory.getExpression(time.getRuntimeExpression());
        return s -> evalExpr(expr, toString(s));
      } else if (time.getLiteralTime() != null) {
        return s -> Objects.equals(s, CloudEventUtils.toOffset(time.getLiteralTime()));
      }
    }
    return isTrue();
  }

  private CloudEventAttrPredicate<URI> dataSchemaFilter(
      EventDataschema dataSchema, ExpressionFactory exprFactory) {
    if (dataSchema != null) {
      if (dataSchema.getExpressionDataSchema() != null) {
        final Expression expr = exprFactory.getExpression(dataSchema.getExpressionDataSchema());
        return s -> evalExpr(expr, toString(s));
      } else if (dataSchema.getLiteralDataSchema() != null) {
        return templateFilter(dataSchema.getLiteralDataSchema());
      }
    }
    return isTrue();
  }

  private CloudEventAttrPredicate<String> stringFilter(String str) {
    return str == null ? isTrue() : x -> x.equals(str);
  }

  private CloudEventAttrPredicate<URI> sourceFilter(
      EventSource source, ExpressionFactory exprFactory) {
    if (source != null) {
      if (source.getRuntimeExpression() != null) {
        final Expression expr = exprFactory.getExpression(source.getRuntimeExpression());
        return s -> evalExpr(expr, toString(s));
      } else if (source.getUriTemplate() != null) {
        return templateFilter(source.getUriTemplate());
      }
    }
    return isTrue();
  }

  private CloudEventAttrPredicate<URI> templateFilter(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return u -> Objects.equals(u, template.getLiteralUri());
    }
    throw new UnsupportedOperationException("Template not supporte here yet");
  }

  private <T> String toString(T uri) {
    return uri != null ? uri.toString() : null;
  }

  private <T> boolean evalExpr(Expression expr, T value) {
    return expr.eval(null, null, JsonUtils.fromValue(value)).asBoolean();
  }

  @Override
  public boolean test(CloudEvent event) {
    return idFilter.test(event.getId())
        && sourceFilter.test(event.getSource())
        && subjectFilter.test(event.getSubject())
        && contentTypeFilter.test(event.getDataContentType())
        && typeFilter.test(event.getType())
        && dataSchemaFilter.test(event.getDataSchema())
        && timeFilter.test(event.getTime())
        && dataFilter.test(CloudEventUtils.toJsonNode(event.getData()))
        && additionalFilter.test(JsonUtils.fromValue(CloudEventUtils.extensions(event)));
  }
}
