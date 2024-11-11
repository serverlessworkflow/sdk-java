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
package io.serverlessworkflow.api;

import io.serverlessworkflow.api.types.Workflow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorkflowWriter {

  public static void writeWorkflow(OutputStream output, Workflow workflow, WorkflowFormat format)
      throws IOException {
    format.mapper().writeValue(output, workflow);
  }

  public static void writeWorkflow(Writer output, Workflow workflow, WorkflowFormat format)
      throws IOException {
    format.mapper().writeValue(output, workflow);
  }

  public static void writeWorkflow(Path output, Workflow workflow) throws IOException {
    writeWorkflow(output, workflow, WorkflowFormat.fromPath(output));
  }

  public static void writeWorkflow(Path output, Workflow workflow, WorkflowFormat format)
      throws IOException {
    try (OutputStream out = Files.newOutputStream(output)) {
      writeWorkflow(out, workflow, format);
    }
  }

  public static String workflowAsString(Workflow workflow, WorkflowFormat format)
      throws IOException {
    try (Writer writer = new StringWriter()) {
      writeWorkflow(writer, workflow, format);
      return writer.toString();
    }
  }

  public static byte[] workflowAsBytes(Workflow workflow, WorkflowFormat format)
      throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writeWorkflow(out, workflow, format);
      return out.toByteArray();
    }
  }

  private WorkflowWriter() {}
}
