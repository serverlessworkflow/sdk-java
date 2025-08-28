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
package io.serverlessworkflow.mermaid;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class Ids {
  private final String salt = Integer.toString(ThreadLocalRandom.current().nextInt(), 36);
  private final AtomicInteger seq = new AtomicInteger();

  private String build() {
    return "n_" + salt + "_" + Integer.toString(seq.getAndIncrement(), 36);
  }

  public static String newId() {
    return new Ids().build();
  }
}
