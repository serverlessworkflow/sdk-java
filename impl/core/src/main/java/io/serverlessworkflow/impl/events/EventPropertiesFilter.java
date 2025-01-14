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

import io.serverlessworkflow.api.types.EventData;
import io.serverlessworkflow.api.types.EventDataschema;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.api.types.EventSource;
import io.serverlessworkflow.api.types.EventTime;
import io.serverlessworkflow.impl.ExpressionHolder;
import io.serverlessworkflow.impl.StringFilter;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

public record EventPropertiesFilter(
    Optional<StringFilter> idFilter,
    Optional<StringFilter> sourceFilter,
    Optional<StringFilter> subjectFilter,
    Optional<StringFilter> contentTypeFilter,
    Optional<StringFilter> typeFilter,
    Optional<StringFilter> dataSchemaFilter,
    Optional<ExpressionHolder<OffsetDateTime>> timeFilter,
    Optional<WorkflowFilter> dataFilter,
    Optional<WorkflowFilter> additionalFilter) {

  public static EventPropertiesFilter build(
      EventProperties properties, ExpressionFactory exprFactory) {
    Optional<StringFilter> idFilter = buildFilter(exprFactory, properties.getId());
    EventSource source = properties.getSource();
    Optional<StringFilter> sourceFilter =
        source == null
            ? Optional.empty()
            : Optional.of(
                WorkflowUtils.buildStringFilter(
                    exprFactory,
                    source.getRuntimeExpression(),
                    WorkflowUtils.toString(source.getUriTemplate())));
    Optional<StringFilter> subjectFilter = buildFilter(exprFactory, properties.getSubject());
    Optional<StringFilter> contentTypeFilter =
        buildFilter(exprFactory, properties.getDatacontenttype());
    Optional<StringFilter> typeFilter = buildFilter(exprFactory, properties.getType());
    EventDataschema dataSchema = properties.getDataschema();
    Optional<StringFilter> dataSchemaFilter =
        dataSchema == null
            ? Optional.empty()
            : Optional.of(
                WorkflowUtils.buildStringFilter(
                    exprFactory,
                    dataSchema.getExpressionDataSchema(),
                    WorkflowUtils.toString(dataSchema.getLiteralDataSchema())));
    EventTime time = properties.getTime();
    Optional<ExpressionHolder<OffsetDateTime>> timeFilter =
        time == null
            ? Optional.empty()
            : Optional.of(
                WorkflowUtils.buildExpressionHolder(
                    exprFactory,
                    time.getRuntimeExpression(),
                    time.getLiteralTime().toInstant().atOffset(ZoneOffset.UTC),
                    JsonUtils::toOffsetDateTime));

    EventData data = properties.getData();
    Optional<WorkflowFilter> dataFilter =
        properties.getData() == null
            ? Optional.empty()
            : Optional.of(
                WorkflowUtils.buildWorkflowFilter(
                    exprFactory, data.getRuntimeExpression(), data.getObject()));
    Map<String, Object> ceAttrs = properties.getAdditionalProperties();
    Optional<WorkflowFilter> additionalFilter =
        ceAttrs == null || ceAttrs.isEmpty()
            ? Optional.empty()
            : Optional.of(WorkflowUtils.buildWorkflowFilter(exprFactory, null, ceAttrs));
    return new EventPropertiesFilter(
        idFilter,
        sourceFilter,
        subjectFilter,
        contentTypeFilter,
        typeFilter,
        dataSchemaFilter,
        timeFilter,
        dataFilter,
        additionalFilter);
  }

  private static Optional<StringFilter> buildFilter(ExpressionFactory exprFactory, String str) {
    return str == null
        ? Optional.empty()
        : Optional.of(WorkflowUtils.buildStringFilter(exprFactory, str));
  }
}
