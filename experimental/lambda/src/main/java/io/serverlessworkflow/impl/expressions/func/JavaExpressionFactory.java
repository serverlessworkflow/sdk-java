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
package io.serverlessworkflow.impl.expressions.func;

import io.cloudevents.CloudEventData;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskMetadata;
import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.api.types.func.TaskMetadataKeys;
import io.serverlessworkflow.api.types.func.TypedFunction;
import io.serverlessworkflow.api.types.func.TypedJavaContextFunction;
import io.serverlessworkflow.api.types.func.TypedJavaFilterFunction;
import io.serverlessworkflow.api.types.func.TypedPredicate;
import io.serverlessworkflow.impl.WorkflowPredicate;
import io.serverlessworkflow.impl.expressions.AbstractExpressionFactory;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import io.serverlessworkflow.impl.expressions.ObjectExpression;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class JavaExpressionFactory extends AbstractExpressionFactory {

  @Override
  public ObjectExpression buildExpression(ExpressionDescriptor descriptor) {
    Object value = descriptor.asObject();
    if (value instanceof Function func) {
      return (w, t, n) -> func.apply(n.asJavaObject());
    } else if (value instanceof TypedFunction func) {
      return (w, t, n) -> func.function().apply(n.as(func.argClass()).orElseThrow());
    } else if (value instanceof JavaFilterFunction func) {
      return (w, t, n) -> func.apply(n.asJavaObject(), w, t);
    } else if (value instanceof TypedJavaFilterFunction func) {
      return (w, t, n) -> func.function().apply(n.as(func.argClass()).orElseThrow(), w, t);
    } else if (value instanceof JavaContextFunction func) {
      return (w, t, n) -> func.apply(n.asJavaObject(), w);
    } else if (value instanceof TypedJavaContextFunction func) {
      return (w, t, n) -> func.function().apply(n.as(func.argClass()).orElseThrow(), w);
    } else {
      return (w, t, n) -> value;
    }
  }

  @Override
  public int priority(ExpressionDescriptor descriptor) {
    Object value = descriptor.asObject();
    if (value instanceof Function
        || value instanceof TypedFunction
        || value instanceof JavaFilterFunction
        || value instanceof TypedJavaFilterFunction
        || value instanceof JavaContextFunction
        || value instanceof TypedJavaContextFunction
        || value instanceof Predicate
        || value instanceof TypedPredicate
        || value instanceof Boolean) {
      return DEFAULT_PRIORITY - 500;
    } else if (descriptor.asString() == null) {
      return MIN_PRIORITY;
    } else {
      return DEFAULT_PRIORITY + 500;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private WorkflowPredicate fromPredicate(Predicate pred) {
    return (w, t, n) -> pred.test(n.asJavaObject());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private WorkflowPredicate fromPredicate(TypedPredicate pred) {
    return (w, t, n) -> pred.pred().test(n.as(pred.argClass()).orElseThrow());
  }

  @Override
  public Optional<WorkflowPredicate> buildIfFilter(TaskBase task) {
    TaskMetadata metadata = task.getMetadata();
    if (metadata != null) {
      Object obj = metadata.getAdditionalProperties().get(TaskMetadataKeys.IF_PREDICATE);
      if (obj instanceof Predicate pred) {
        return Optional.of(fromPredicate(pred));
      } else if (obj instanceof TypedPredicate pred) {
        return Optional.of(fromPredicate(pred));
      }
    }
    return Optional.empty();
  }

  @Override
  public WorkflowPredicate buildPredicate(ExpressionDescriptor desc) {
    Object value = desc.asObject();
    if (value instanceof Predicate pred) {
      return fromPredicate(pred);
    } else if (value instanceof TypedPredicate pred) {
      return fromPredicate(pred);
    } else if (value instanceof Boolean bool) {
      return (w, f, n) -> bool;
    } else {
      throw new IllegalArgumentException("value should be a predicate or a boolean");
    }
  }

  @Override
  protected String toString(Object eval) {
    return asClass(eval, String.class);
  }

  @Override
  protected CloudEventData toCloudEventData(Object eval) {
    return asClass(eval, CloudEventData.class);
  }

  @Override
  protected OffsetDateTime toDate(Object eval) {
    return asClass(eval, OffsetDateTime.class);
  }

  @Override
  protected Map<String, Object> toMap(Object eval) {
    return asClass(eval, Map.class);
  }

  @Override
  protected Collection<?> toCollection(Object obj) {
    return asClass(obj, Collection.class);
  }

  private <T> T asClass(Object obj, Class<T> clazz) {
    return clazz.cast(obj);
  }
}
