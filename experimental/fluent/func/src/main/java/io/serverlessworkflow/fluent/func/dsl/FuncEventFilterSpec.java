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
package io.serverlessworkflow.fluent.func.dsl;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import io.serverlessworkflow.api.types.func.ContextPredicate;
import io.serverlessworkflow.api.types.func.FilterPredicate;
import io.serverlessworkflow.fluent.func.FuncEventFilterBuilder;
import io.serverlessworkflow.fluent.func.FuncEventFilterPropertiesBuilder;
import io.serverlessworkflow.fluent.spec.dsl.AbstractEventFilterSpec;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.util.Map;
import java.util.Objects;

public final class FuncEventFilterSpec
    extends AbstractEventFilterSpec<
        FuncEventFilterSpec, FuncEventFilterPropertiesBuilder, FuncEventFilterBuilder> {

  @Override
  protected FuncEventFilterSpec self() {
    return this;
  }

  /**
   * Configures the filter to match incoming event data based on a Predicate. This is the Listen
   * counterpart to Emit's jsonData(Function).
   */
  public FuncEventFilterSpec dataCE(SerializablePredicate<CloudEvent> predicate) {
    addPropertyStep(e -> e.dataCE(predicate));
    return this;
  }

  public FuncEventFilterSpec dataCE(ContextPredicate<CloudEvent> predicate) {
    addPropertyStep(e -> e.dataCE(predicate));
    return this;
  }

  public FuncEventFilterSpec dataCE(FilterPredicate<CloudEvent> predicate) {
    addPropertyStep(e -> e.dataCE(predicate));
    return this;
  }

  public FuncEventFilterSpec data(SerializablePredicate<CloudEventData> predicate) {
    addPropertyStep(e -> e.data(predicate));
    return this;
  }

  public FuncEventFilterSpec data(ContextPredicate<CloudEventData> predicate) {
    addPropertyStep(e -> e.data(predicate));
    return this;
  }

  public FuncEventFilterSpec data(FilterPredicate<CloudEventData> predicate) {
    addPropertyStep(e -> e.data(predicate));
    return this;
  }

  /**
   * Evaluates the given predicate against the CloudEvent data payload, automatically parsed as a
   * Map.
   *
   * <p>For example, you can evaluate this filter as:
   *
   * <pre>
   * .dataAsMap(map -> "123".equals(map.get("orderId")))
   * </pre>
   *
   * @param predicate the predicate to evaluate against the parsed Map.
   */
  public FuncEventFilterSpec dataAsMap(SerializablePredicate<Map<String, Object>> predicate) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce) -> {
                  Map<String, Object> ceDataMap = asCEDataMap(ce);
                  return !ceDataMap.isEmpty() && predicate.test(ceDataMap);
                }));
    return this;
  }

  /**
   * Evaluates the given ContextPredicate against the CloudEvent data payload (parsed as a Map) and
   * the current WorkflowContextData.
   */
  public FuncEventFilterSpec dataAsMap(ContextPredicate<Map<String, Object>> predicate) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce, WorkflowContextData context) -> {
                  Map<String, Object> ceDataMap = asCEDataMap(ce);
                  return !ceDataMap.isEmpty() && predicate.test(ceDataMap, context);
                }));
    return this;
  }

  /**
   * Evaluates the given FilterPredicate against the CloudEvent data payload (parsed as a Map), the
   * current WorkflowContextData, and the TaskContextData.
   */
  public FuncEventFilterSpec dataAsMap(FilterPredicate<Map<String, Object>> predicate) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce, WorkflowContextData context, TaskContextData taskContext) -> {
                  Map<String, Object> ceDataMap = asCEDataMap(ce);
                  return !ceDataMap.isEmpty() && predicate.test(ceDataMap, context, taskContext);
                }));
    return this;
  }

  /**
   * Evaluates the given predicate against the CloudEvent data payload, automatically parsed into
   * the specified target type.
   *
   * <p>For example, you can evaluate this filter as:
   *
   * <pre>
   * .dataAs(Order.class, order -> order.getId() == 123)
   * </pre>
   *
   * @param targetType The class of the type <T> to deserialize the payload into.
   * @param predicate The predicate to evaluate against the parsed object.
   * @param <T> The target type.
   */
  public <T> FuncEventFilterSpec dataAs(Class<T> targetType, SerializablePredicate<T> predicate) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce) -> {
                  T parsedData = parseCEData(ce, targetType);
                  return parsedData != null && predicate.test(parsedData);
                }));
    return this;
  }

  /**
   * Evaluates the given ContextPredicate against the CloudEvent data payload (parsed into the
   * specified target type) and the current WorkflowContextData.
   */
  public <T> FuncEventFilterSpec dataAs(Class<T> targetType, ContextPredicate<T> predicate) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce, WorkflowContextData context) -> {
                  T parsedData = parseCEData(ce, targetType);
                  return parsedData != null && predicate.test(parsedData, context);
                }));
    return this;
  }

  /**
   * Evaluates the given FilterPredicate against the CloudEvent data payload (parsed into the
   * specified target type), the current WorkflowContextData, and the TaskContextData.
   */
  public <T> FuncEventFilterSpec dataAs(Class<T> targetType, FilterPredicate<T> predicate) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce, WorkflowContextData context, TaskContextData taskContext) -> {
                  T parsedData = parseCEData(ce, targetType);
                  return parsedData != null && predicate.test(parsedData, context, taskContext);
                }));
    return this;
  }

  /**
   * Filter events which field carries the current workflow instance ID. For example, given the data
   * payload:
   *
   * <pre>
   * {
   *      "order": { "number": 123 },
   *      "workflowInstanceId": "123456789"
   * }
   * </pre>
   *
   * <p>You would call <code>dataByInstanceId("workflowInstanceId")</code>. Events matching the
   * current instance ID in the field <code>workflowInstanceId</code> would match this filter.
   *
   * @param fieldName name of the field in the CE data that carries the instance ID.
   */
  public FuncEventFilterSpec dataByInstanceId(String fieldName) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce, WorkflowContextData context) -> {
                  Map<String, Object> ceDataMap = asCEDataMap(ce);
                  return Objects.equals(ceDataMap.get(fieldName), context.instanceData().id());
                }));
    return this;
  }

  /**
   * Same as {@link #dataByInstanceId(String)}, but now the filter looks at the CE extension name.
   *
   * @param extensionName the extension name where to fetch the given workflow instance ID to match
   *     with the current execution.
   */
  public FuncEventFilterSpec dataByInstanceIdOnExtension(String extensionName) {
    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce, WorkflowContextData context) ->
                    context.instanceData().id().equals(ce.getExtension(extensionName))));
    return this;
  }

  /**
   * Matches events that carry in the CE data payload fields with the same values as the input of
   * the current task.
   *
   * <p>For example, you can filter events carrying specific data using <code>
   * dataFields("orderId", "customerId")</code>. Events where the CE data payload matches the task
   * input for all provided fields will pass this filter.
   *
   * @param fieldNames the field names to match this filter.
   */
  public FuncEventFilterSpec dataFields(String... fieldNames) {
    if (fieldNames == null || fieldNames.length == 0) return this;

    addPropertyStep(
        e ->
            e.dataCE(
                (CloudEvent ce, WorkflowContextData context, TaskContextData taskContext) -> {
                  Map<String, Object> input = taskContext.rawInput().asMap().orElse(Map.of());
                  Map<String, Object> ceDataMap = asCEDataMap(ce);

                  return !ceDataMap.isEmpty()
                      && java.util.Arrays.stream(fieldNames)
                          .allMatch(
                              fieldName ->
                                  Objects.equals(ceDataMap.get(fieldName), input.get(fieldName)));
                }));
    return this;
  }

  private Map<String, Object> asCEDataMap(CloudEvent ce) {
    if (ce.getData() == null) return Map.of();
    PojoCloudEventData<Map<String, Object>> mappedData =
        CloudEventUtils.mapData(
            ce, PojoCloudEventDataMapper.from(JsonUtils.mapper(), new TypeReference<>() {}));
    if (mappedData == null || mappedData.getValue() == null) {
      return Map.of();
    }

    return mappedData.getValue();
  }

  private <T> T parseCEData(CloudEvent ce, Class<T> targetType) {
    if (ce.getData() == null) return null;
    try {
      return JsonUtils.mapper().readValue(ce.getData().toBytes(), targetType);
    } catch (Exception e) {
      return null;
    }
  }
}
