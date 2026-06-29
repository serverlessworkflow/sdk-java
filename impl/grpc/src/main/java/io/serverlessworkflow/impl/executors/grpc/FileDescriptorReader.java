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
package io.serverlessworkflow.impl.executors.grpc;

import com.github.os72.protocjar.Protoc;
import com.google.protobuf.DescriptorProtos;
import io.serverlessworkflow.impl.resources.ExternalResourceHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileDescriptorReader {

  private static final Logger logger = LoggerFactory.getLogger(FileDescriptorReader.class);

  static FileDescriptorContext readDescriptor(ExternalResourceHandler externalResourceHandler) {

    try (InputStream inputStream = externalResourceHandler.open()) {

      Path grpcDir = Files.createTempDirectory("serverless-workflow-");

      Path protoFile = grpcDir.resolve(externalResourceHandler.name());
      if (!Files.exists(protoFile)) {
        Files.createDirectories(protoFile.getParent());
      }

      Files.copy(inputStream, protoFile, StandardCopyOption.REPLACE_EXISTING);

      Path descriptorOutput = grpcDir.resolve("descriptor.protobin");

      generateFileDescriptor(grpcDir, protoFile, descriptorOutput);

      DescriptorProtos.FileDescriptorSet fileDescriptorSet =
          DescriptorProtos.FileDescriptorSet.newBuilder()
              .mergeFrom(Files.readAllBytes(descriptorOutput))
              .build();

      return new FileDescriptorContext(fileDescriptorSet, externalResourceHandler.name());

    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to process GRPC descriptor file associated with resource "
              + externalResourceHandler.name(),
          e);
    }
  }

  /*
   * Calls protoc binary with <code>--descriptor_set_out=</code> option set. First attempts to use
   * the embedded protoc from protoc-jar library. If that fails with FileNotFoundException
   * (unsupported architecture), falls back to using the system's installed protoc via
   * ProcessBuilder.
   *
   * @param grpcDir a temporary directory
   * @param protoFile the .proto file used by <code>protoc</code> to generate the file descriptor
   * @param descriptorOutput the output directory where the descriptor file will be generated
   * @throws IOException
   */
  private static void generateFileDescriptor(Path grpcDir, Path protoFile, Path descriptorOutput)
      throws IOException {
    String[] protocArgs =
        new String[] {
          "--include_imports",
          "--descriptor_set_out=" + descriptorOutput.toAbsolutePath(),
          "-I",
          grpcDir.toAbsolutePath().toString(),
          protoFile.toAbsolutePath().toString()
        };

    try {
      // First attempt: use protoc-jar library
      int status = Protoc.runProtoc(protocArgs);
      if (status != 0) {
        throw new IOException(
            "Unable to generate file descriptor, 'protoc' execution failed with status " + status);
      }
    } catch (FileNotFoundException e) {
      // Fallback: try using system's installed protoc
      logger.warn(
          "Protoc binary not available for this architecture via protoc-jar. "
              + "Attempting to use system-installed protoc...");
      generateFileDescriptorWithProcessBuilder(protocArgs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Thread interrupted while generating proto descriptor", e);
    }
  }

  /*
   * Fallback method to generate file descriptor using system's installed protoc via ProcessBuilder.
   *
   * @param protocArgs the arguments to pass to protoc command
   */
  private static void generateFileDescriptorWithProcessBuilder(String[] protocArgs)
      throws IOException {
    try {
      String[] command = new String[protocArgs.length + 1];
      command[0] = "protoc";
      System.arraycopy(protocArgs, 0, command, 1, protocArgs.length);

      ProcessBuilder processBuilder = new ProcessBuilder(command);

      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      int exitCode = process.waitFor();

      if (exitCode != 0) {
        throw new IOException(
            "Unable to generate file descriptor using system protoc. Exit code: " + exitCode);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Protoc execution was interrupted", e);
    }
  }

  private FileDescriptorReader() {}
}
