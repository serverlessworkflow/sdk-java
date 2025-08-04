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
package io.serverlessworkflow.impl.expressions;

import io.cloudevents.CloudEventData;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.ServicePriority;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.WorkflowPredicate;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ExpressionFactory extends ServicePriority {

  WorkflowValueResolver<String> resolveString(ExpressionDescriptor desc);

  WorkflowValueResolver<OffsetDateTime> resolveDate(ExpressionDescriptor desc);

  WorkflowValueResolver<CloudEventData> resolveCE(ExpressionDescriptor desc);

  WorkflowValueResolver<Map<String, Object>> resolveMap(ExpressionDescriptor desc);

  WorkflowValueResolver<Collection<?>> resolveCollection(ExpressionDescriptor desc);

  WorkflowFilter buildFilter(ExpressionDescriptor desc);

  WorkflowPredicate buildPredicate(ExpressionDescriptor desc);

  WorkflowModelFactory modelFactory();

  default Optional<WorkflowPredicate> buildIfFilter(TaskBase task) {
    return task.getIf() != null
        ? Optional.of(buildPredicate(ExpressionDescriptor.from(task.getIf())))
        : Optional.empty();
  }
}
