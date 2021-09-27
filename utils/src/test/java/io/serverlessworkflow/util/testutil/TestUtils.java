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
package io.serverlessworkflow.util.testutil;

import io.serverlessworkflow.api.Workflow;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class TestUtils {

  public static Workflow createWorkflow(String source) {
    return Workflow.fromSource(source);
  }

  public static Workflow createWorkflowFromTestResource(String fileRelativePath) {
    InputStreamReader reader = getTestResourceStreamReader(fileRelativePath);
    return createWorkflow(readFileAsString(reader));
  }

  public static String readFileAsString(Reader reader) {
    try {
      StringBuilder fileData = new StringBuilder(1000);
      char[] buf = new char[1024];
      int numRead;
      while ((numRead = reader.read(buf)) != -1) {
        String readData = String.valueOf(buf, 0, numRead);
        fileData.append(readData);
        buf = new char[1024];
      }
      reader.close();
      return fileData.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static InputStreamReader getTestResourceStreamReader(String fileRelativePath) {
    return new InputStreamReader(TestUtils.class.getResourceAsStream(fileRelativePath));
  }
}
