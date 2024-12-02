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
package io.serverlessworkflow.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.impl.generic.SortedArrayList;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SortedListTest {

  private record Person(String name, int age) {}

  @Test
  void testConstructor() {
    assertThat(new SortedArrayList<>(Arrays.asList(3, 2, 1))).isEqualTo(Arrays.asList(1, 2, 3));
  }

  @Test
  void testAdd() {
    List<Integer> list = new SortedArrayList<>();
    list.add(1);
    list.add(4);
    list.add(3);
    list.add(2);
    assertThat(list).isEqualTo(Arrays.asList(1, 2, 3, 4));
  }

  @Test
  void testAddPojo() {
    List<Person> list = new SortedArrayList<>((a, b) -> b.age() - a.age());
    list.add(new Person("Mariam", 5));
    list.add(new Person("Belen", 12));
    list.add(new Person("Alejandro", 7));
    list.add(new Person("Vicente", 16));
    list.add(new Person("Daniel", 14));
    assertThat(list.stream().map(Person::name))
        .isEqualTo(Arrays.asList("Vicente", "Daniel", "Belen", "Alejandro", "Mariam"));
  }

  @Test
  void testAddAll() {
    List<Instant> list = new SortedArrayList<>();
    Instant now = Instant.now();
    list.addAll(Arrays.asList(now.plusMillis(1000), now));
    assertThat(list.get(0)).isEqualTo(now);
  }
}
