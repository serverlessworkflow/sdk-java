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
package io.serverlessworkflow.impl.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class SortedArrayList<T> extends ArrayList<T> {

  private static final long serialVersionUID = 1L;
  private final Comparator<T> comparator;

  public SortedArrayList() {
    this(SortedArrayList::defaultCompare);
  }

  @SuppressWarnings("unchecked")
  private static <V> int defaultCompare(V a, V b) {
    return a instanceof Comparable ? ((Comparable<V>) a).compareTo(b) : 0;
  }

  public SortedArrayList(Comparator<T> comparator) {
    this.comparator = comparator;
  }

  public SortedArrayList(Collection<T> collection) {
    this(collection, SortedArrayList::defaultCompare);
  }

  public SortedArrayList(Collection<T> collection, Comparator<T> comparator) {
    super(collection.size());
    this.comparator = comparator;
    addAll(collection);
  }

  @Override
  public boolean add(T object) {
    int i;
    for (i = 0; i < size() && comparator.compare(object, get(i)) >= 0; i++) {}
    super.add(i, object);
    return true;
  }

  public boolean addAll(Collection<? extends T> c) {
    ensureCapacity(size() + c.size());
    c.forEach(this::add);
    return !c.isEmpty();
  }

  public T set(int index, T element) {
    throw new UnsupportedOperationException("Do not allow adding in a particular index");
  }
}
