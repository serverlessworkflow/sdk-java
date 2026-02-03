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
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class FileDescriptorReader {

  public FileDescriptorContext readDescriptor(ExternalResourceHandler externalResourceHandler) {
    Path grpcDir =
        tryCreateTempGrpcDir()
            .orElseThrow(
                () -> new IllegalStateException("Could not create temporary gRPC directory"));

    try (InputStream inputStream = externalResourceHandler.open()) {

      Path protoFile = grpcDir.resolve(externalResourceHandler.name());
      if (!Files.exists(protoFile)) {
        Files.createDirectories(protoFile);
      }

      Files.copy(inputStream, protoFile, StandardCopyOption.REPLACE_EXISTING);

      Path descriptorOutput = grpcDir.resolve("descriptor.protobin");

      try {

        generateFileDescriptor(grpcDir, protoFile, descriptorOutput);

        DescriptorProtos.FileDescriptorSet fileDescriptorSet =
            DescriptorProtos.FileDescriptorSet.newBuilder()
                .mergeFrom(Files.readAllBytes(descriptorOutput))
                .build();

        return new FileDescriptorContext(fileDescriptorSet, externalResourceHandler.name());

      } catch (IOException e) {
        throw new UncheckedIOException(
            "Unable to read external resource handler: " + externalResourceHandler.name(), e);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read descriptor file", e);
    }
  }

  private Optional<Path> tryCreateTempGrpcDir() {
    try {
      return Optional.of(Files.createTempDirectory("serverless-workflow-"));
    } catch (IOException e) {
      throw new UncheckedIOException("Error while creating temporary gRPC directory", e);
    }
  }

  /**
   * Calls protoc binary with <code>--descriptor_set_out=</code> option set.
   *
   * @param grpcDir a temporary directory
   * @param protoFile the .proto file used by <code>protoc</code> to generate the file descriptor
   * @param descriptorOutput the output directory where the descriptor file will be generated
   */
  private static void generateFileDescriptor(Path grpcDir, Path protoFile, Path descriptorOutput) {
    String[] protocArgs =
        new String[] {
          "--include_imports",
          "--descriptor_set_out=" + descriptorOutput.toAbsolutePath(),
          "-I",
          grpcDir.toAbsolutePath().toString(),
          protoFile.toAbsolutePath().toString()
        };

    try {

      int status = Protoc.runProtoc(protocArgs);

      if (status != 0) {
        throw new RuntimeException(
            "Unable to generate file descriptor, 'protoc' execution failed with status " + status);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Unable to generate file descriptor", e);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to generate file descriptor", e);
    }
  }
}
