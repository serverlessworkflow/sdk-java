/*
 * Copyright 2022-Present The Serverless Workflow Specification Authors
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
package io.serverlessworkflow.validation.state;

import static io.serverlessworkflow.validation.state.CommonStateValidator.EMPTY_STATE_NAME_MSG;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.validation.ValidationError;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CommonStateValidatorTest extends StateValidatorTestUtil {

  public static Stream<Arguments> testInvalidWorkflowSource() {
    return Stream.of(
        Arguments.of("/states/invalid/empty_state_name.json", EMPTY_STATE_NAME_MSG),
        Arguments.of("/states/invalid/missing_state_name.json", EMPTY_STATE_NAME_MSG));
  }

  @ParameterizedTest
  @MethodSource("testInvalidWorkflowSource")
  void testInvalidWorkflow(String workflowFilePath, String expectedError) throws IOException {
    Collection<ValidationError> errors = validateFile(workflowFilePath);

    assertThat(errors).hasSize(1).allMatch(error -> expectedError.equals(error.getMessage()));
  }
}
