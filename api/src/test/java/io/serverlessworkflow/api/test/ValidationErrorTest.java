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
package io.serverlessworkflow.api.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.serverlessworkflow.api.validation.ValidationError;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

public class ValidationErrorTest {

  @Test
  void duplicateTest() {

    Collection<ValidationError> duplicatedErrors = addMessagesToCollection(new ArrayList<>());
    Collection<ValidationError> errors = addMessagesToCollection(new LinkedHashSet<>());

    assertEquals(duplicatedErrors.size(), 3);
    assertEquals(errors.size(), 2);

    assertEquals(duplicatedErrors.iterator().next().getMessage(), "This is the first message");
    assertEquals(errors.iterator().next().getMessage(), "This is the first message");
  }

  private Collection<ValidationError> addMessagesToCollection(Collection<ValidationError> errors) {
    ValidationError first = new ValidationError();
    first.setMessage("This is the first message");
    ValidationError second = new ValidationError();
    second.setMessage("This is the duplicated message");
    ValidationError third = new ValidationError();
    third.setMessage("This is the duplicated message");
    errors.add(first);
    errors.add(second);
    errors.add(third);
    return errors;
  }
}
