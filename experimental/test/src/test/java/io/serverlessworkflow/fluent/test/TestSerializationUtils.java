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
package io.serverlessworkflow.fluent.test;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflow;
import static io.serverlessworkflow.api.WorkflowWriter.writeWorkflow;

import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.types.Workflow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestSerializationUtils {

  private static final Logger logger = LoggerFactory.getLogger(TestSerializationUtils.class);

  private TestSerializationUtils() {}

  static Workflow writeAndReadInMemory(Workflow workflow) throws IOException {
    byte[] bytes;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writeWorkflow(out, workflow, WorkflowFormat.YAML);
      bytes = out.toByteArray();
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Workflow string representation is {}", new String(bytes));
    }
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
      return readWorkflow(in, WorkflowFormat.YAML);
    }
  }
}
