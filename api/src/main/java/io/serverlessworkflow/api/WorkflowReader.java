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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorkflowReader {

  public static Workflow readWorkflow(InputStream input, WorkflowFormat format) throws IOException {
    return defaultReader().read(input, format);
  }

  public static Workflow readWorkflow(Reader input, WorkflowFormat format) throws IOException {
    return defaultReader().read(input, format);
  }

  public static Workflow readWorkflow(byte[] input, WorkflowFormat format) throws IOException {
    return defaultReader().read(input, format);
  }

  public static Workflow readWorkflow(Path path) throws IOException {
    return readWorkflow(path, WorkflowFormat.fromPath(path), defaultReader());
  }

  public static Workflow readWorkflow(Path path, WorkflowFormat format) throws IOException {
    return readWorkflow(path, format, defaultReader());
  }

  public static Workflow readWorkflowFromString(String input, WorkflowFormat format)
      throws IOException {
    return defaultReader().read(input, format);
  }

  public static Workflow readWorkflowFromClasspath(String classpath) throws IOException {
    return readWorkflowFromClasspath(classpath, defaultReader());
  }

  public static Workflow readWorkflowFromClasspath(
      String classpath, ClassLoader cl, WorkflowFormat format) throws IOException {
    return readWorkflowFromClasspath(classpath, defaultReader());
  }

  public static Workflow readWorkflow(Path path, WorkflowReaderOperations reader)
      throws IOException {
    return readWorkflow(path, WorkflowFormat.fromPath(path), reader);
  }

  public static Workflow readWorkflow(
      Path path, WorkflowFormat format, WorkflowReaderOperations reader) throws IOException {
    return reader.read(Files.readAllBytes(path), format);
  }

  public static Workflow readWorkflowFromClasspath(
      String classpath, WorkflowReaderOperations reader) throws IOException {
    return readWorkflowFromClasspath(
        classpath,
        Thread.currentThread().getContextClassLoader(),
        WorkflowFormat.fromFileName(classpath),
        reader);
  }

  public static Workflow readWorkflowFromClasspath(
      String classpath, ClassLoader cl, WorkflowFormat format, WorkflowReaderOperations reader)
      throws IOException {
    try (InputStream in = cl.getResourceAsStream(classpath)) {
      if (in == null) {
        throw new FileNotFoundException(classpath);
      }
      return reader.read(in, format);
    }
  }

  public static WorkflowReaderOperations noValidation() {
    return NoValidationHolder.instance;
  }

  public static WorkflowReaderOperations validation() {
    return ValidationHolder.instance;
  }

  private static class NoValidationHolder {
    private static final WorkflowReaderOperations instance = new DirectReader();
  }

  private static class ValidationHolder {
    private static final WorkflowReaderOperations instance = new ValidationReader();
  }

  private static WorkflowReaderOperations defaultReader() {
    return NoValidationHolder.instance;
  }

  private WorkflowReader() {}
}
