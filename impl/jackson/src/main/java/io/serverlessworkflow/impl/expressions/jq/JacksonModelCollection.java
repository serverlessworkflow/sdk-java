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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

public class JacksonModelCollection implements WorkflowModelCollection {

  private ArrayNode node;

  JacksonModelCollection() {
    this.node = JsonUtils.array();
  }

  JacksonModelCollection(ArrayNode node) {
    this.node = node;
  }

  @Override
  public <T> Optional<T> as(Class<T> clazz) {
    return clazz.isAssignableFrom(ArrayNode.class)
        ? Optional.of(clazz.cast(node))
        : Optional.empty();
  }

  @Override
  public int size() {
    return node.size();
  }

  @Override
  public boolean isEmpty() {
    return node.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<WorkflowModel> iterator() {
    return new WrapperIterator(node.iterator());
  }

  private class WrapperIterator implements Iterator<WorkflowModel> {

    private Iterator<JsonNode> iterator;

    public WrapperIterator(Iterator<JsonNode> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {

      return iterator.hasNext();
    }

    @Override
    public WorkflowModel next() {
      return new JacksonModel(iterator.next());
    }
  }

  @Override
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(WorkflowModel e) {
    node.add(
        e.as(JsonNode.class).orElseThrow(() -> new IllegalArgumentException("Not a json node")));
    return true;
  }

  @Override
  public boolean remove(Object o) {
    int size = node.size();
    node.removeIf(i -> i.equals(o));
    return node.size() < size;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends WorkflowModel> c) {
    c.forEach(this::add);
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    int size = node.size();
    c.forEach(o -> node.removeIf(i -> i.equals(o)));
    return node.size() < size;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    node.removeAll();
  }

  @Override
  public String toString() {
    return node.toPrettyString();
  }

  @Override
  public Object asJavaObject() {
    return JsonUtils.toJavaValue(node);
  }

  @Override
  public Class<?> objectClass() {
    return ArrayNode.class;
  }
}
