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
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorkflowReader {

  public static Workflow readWorkflow(InputStream input, WorkflowFormat format) throws IOException {
    return format.mapper().readValue(input, Workflow.class);
  }

  public static Workflow readWorkflow(Reader input, WorkflowFormat format) throws IOException {
    return format.mapper().readValue(input, Workflow.class);
  }

  public static Workflow readWorkflow(Path path, WorkflowFormat format) throws IOException {
    return format.mapper().readValue(Files.readAllBytes(path), Workflow.class);
  }

  public static Workflow readWorkflow(byte[] content, WorkflowFormat format) throws IOException {
    try (InputStream input = new ByteArrayInputStream(content)) {
      return readWorkflow(input, format);
    }
  }

  public static Workflow readWorkflowFromString(String content, WorkflowFormat format)
      throws IOException {
    try (Reader reader = new StringReader(content)) {
      return readWorkflow(reader, format);
    }
  }

  public static Workflow readWorkflowFromClasspath(String classpath) throws IOException {
    return readWorkflowFromClasspath(
        classpath,
        Thread.currentThread().getContextClassLoader(),
        WorkflowFormat.fromFileName(classpath));
  }

  public static Workflow readWorkflowFromClasspath(
      String classpath, ClassLoader cl, WorkflowFormat format) throws IOException {
    try (InputStream in = cl.getResourceAsStream(classpath)) {
      if (in == null) {
        throw new FileNotFoundException(classpath);
      }
      return readWorkflow(in, format);
    }
  }

  private WorkflowReader() {}
}
