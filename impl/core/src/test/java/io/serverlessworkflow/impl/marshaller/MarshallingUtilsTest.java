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
package io.serverlessworkflow.impl.marshaller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MarshallingUtilsTest {

  @Test
  void testCustomMarshallers() {
    CustomObjectMarshaller personMarshaller = Mockito.spy(CustomObjectMarshaller.class);
    Mockito.when(personMarshaller.priority()).thenReturn(1);
    Mockito.when(personMarshaller.getObjectClass()).thenReturn(Person.class);
    CustomObjectMarshaller employeeMarshaller = Mockito.spy(CustomObjectMarshaller.class);
    Mockito.when(employeeMarshaller.priority()).thenReturn(2);
    Mockito.when(employeeMarshaller.getObjectClass()).thenReturn(Employee.class);
    CustomObjectMarshaller objectMarshaller = Mockito.spy(CustomObjectMarshaller.class);
    Mockito.when(objectMarshaller.priority()).thenReturn(3);
    Mockito.when(objectMarshaller.getObjectClass()).thenReturn(Object.class);
    Object employee = new Employee();
    Object person = new Person();
    Object other = new byte[2];

    List<CustomObjectMarshaller> marshallers =
        Stream.of(objectMarshaller, employeeMarshaller, personMarshaller)
            .sorted()
            .collect(Collectors.toList());
    assertThat(marshallers).containsExactly(personMarshaller, employeeMarshaller, objectMarshaller);
    assertThat(MarshallingUtils.getCustomMarshaller(marshallers, employee.getClass()))
        .isEqualTo(employeeMarshaller);
    assertThat(MarshallingUtils.getCustomMarshaller(marshallers, person.getClass()))
        .isEqualTo(personMarshaller);
    assertThat(MarshallingUtils.getCustomMarshaller(marshallers, other.getClass()))
        .isEqualTo(objectMarshaller);
  }
}
