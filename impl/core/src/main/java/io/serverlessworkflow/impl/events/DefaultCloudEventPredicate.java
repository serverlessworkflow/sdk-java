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
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.impl.ExpressionHolder;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.util.Optional;

public class DefaultCloudEventPredicate implements CloudEventPredicate {

  private final EventPropertiesFilter props;

  public DefaultCloudEventPredicate(EventProperties properties, ExpressionFactory exprFactory) {
    this.props = EventPropertiesFilter.build(properties, exprFactory);
  }

  @Override
  public boolean test(CloudEvent event, WorkflowContext workflow, TaskContext task) {
    return test(props.idFilter(), event.getId(), workflow, task)
        && test(props.sourceFilter(), event.getSource().toString(), workflow, task)
        && test(props.subjectFilter(), event.getSubject(), workflow, task)
        && test(props.contentTypeFilter(), event.getDataContentType(), workflow, task)
        && test(props.typeFilter(), event.getType(), workflow, task)
        && test(props.dataSchemaFilter(), event.getDataSchema().toString(), workflow, task)
        && test(props.timeFilter(), event.getTime(), workflow, task)
        && test(props.dataFilter(), CloudEventUtils.toJsonNode(event.getData()), workflow, task)
        && test(
            props.additionalFilter(),
            JsonUtils.fromValue(CloudEventUtils.extensions(event)),
            workflow,
            task);
  }

  private <T, V extends ExpressionHolder<T>> boolean test(
      Optional<V> optFilter, T value, WorkflowContext workflow, TaskContext task) {
    return optFilter.map(filter -> filter.apply(workflow, task).equals(value)).orElse(true);
  }

  private boolean test(
      Optional<WorkflowFilter> optFilter,
      JsonNode value,
      WorkflowContext workflow,
      TaskContext task) {
    return optFilter
        .map(filter -> filter.apply(workflow, task, task.input()).equals(value))
        .orElse(true);
  }
}
