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
package io.serverlessworkflow.impl.expressions.jq;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JQExpressionFactoryTest {

  @Test
  void testNestedMap() {
    WorkflowContext workflowContext = Mockito.mock(WorkflowContext.class);
    JQExpressionFactory factory = new JQExpressionFactory();
    WorkflowValueResolver<Map<String, Object>> expr =
        factory.resolveMap(
            ExpressionDescriptor.object(
                Map.of(
                    "name",
                    "${.name}",
                    "surname",
                    "Doe",
                    "nested",
                    Map.of("name", "${.name}", "surname", "Doe"))));
    Map<String, Object> result =
        expr.apply(
            workflowContext,
            null,
            new JacksonModel(JsonUtils.mapper().createObjectNode().put("name", "John")));
    assertThat(result.get("name")).isEqualTo("John");
    assertThat(result.get("surname")).isEqualTo("Doe");
    Map<String, Object> nested = (Map<String, Object>) result.get("nested");
    assertThat(result.get("name")).isEqualTo("John");
    assertThat(result.get("surname")).isEqualTo("Doe");
  }

  @Test
  void testNestedArray() {
    WorkflowContext workflowContext = Mockito.mock(WorkflowContext.class);
    JQExpressionFactory factory = new JQExpressionFactory();
    WorkflowValueResolver<Map<String, Object>> expr =
        factory.resolveMap(
            ExpressionDescriptor.object(
                Map.of("array", "${.array}", "nested", Map.of("array", "${.array}"))));
    Map<String, Object> result =
        expr.apply(
            workflowContext,
            null,
            new JacksonModel(
                JsonUtils.mapper()
                    .createObjectNode()
                    .set("array", JsonUtils.mapper().createArrayNode().add("John").add("Doe"))));
    assertThat(result.get("array")).isEqualTo(List.of("John", "Doe"));
    Map<String, Object> nested = (Map<String, Object>) result.get("nested");
    assertThat(nested.get("array")).isEqualTo(List.of("John", "Doe"));
  }

  @Test
  void testList() {
    WorkflowContext workflowContext = Mockito.mock(WorkflowContext.class);
    JQExpressionFactory factory = new JQExpressionFactory();
    WorkflowValueResolver<Map<String, Object>> expr =
        factory.resolveMap(
            ExpressionDescriptor.object(Map.of("array", List.of("item1", "item2", "${.name}"))));
    Map<String, Object> result =
        expr.apply(
            workflowContext,
            null,
            new JacksonModel(JsonUtils.mapper().createObjectNode().put("name", "item3")));
    Iterator<Object> iter = ((Collection<Object>) result.get("array")).iterator();
    assertThat(iter.next()).isEqualTo("item1");
    assertThat(iter.next()).isEqualTo("item2");
    assertThat(iter.next()).isEqualTo("item3");
  }
}
