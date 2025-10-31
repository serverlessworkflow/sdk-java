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
package io.serverlessworkflow.impl.test;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;

final class ForkedDbGen {
  private static String detectJavaCmd() {
    Optional<String> cmd = ProcessHandle.current().info().command();
    if (cmd.isPresent() && Files.isRegularFile(Path.of(cmd.get()))) return cmd.get();
    String home = System.getProperty("java.home");
    String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
    Path p = Path.of(home, "bin", exe);
    return Files.isRegularFile(p) ? p.toString() : "java";
  }

  static void run(Path db, boolean suspend) throws IOException, InterruptedException {
    Files.createDirectories(db.getParent());
    String javaCmd = detectJavaCmd();
    String cp = System.getProperty("java.class.path");

    ProcessBuilder pb =
        new ProcessBuilder(
            javaCmd,
            "-cp",
            cp,
            DBGeneratorCli.class.getCanonicalName(),
            db.toString(),
            String.valueOf(suspend));
    pb.redirectErrorStream(true);

    Process p = pb.start();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      for (String line; (line = r.readLine()) != null; ) System.out.println("[dbgen] " + line);
    }
    int code = p.waitFor();
    if (code != 0) throw new IllegalStateException("DB gen failed (" + code + ") for " + db);
  }
}
