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
package io.serverlessworkflow.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultExecutorServiceFactory implements ExecutorServiceFactory {

  private static final ExecutorServiceFactory instance = new DefaultExecutorServiceFactory();

  public static ExecutorServiceFactory instance() {
    return instance;
  }

  private static class ExecutorServiceHolder {
    private static ExecutorService instance = Executors.newCachedThreadPool();
  }

  @Override
  public ExecutorService get() {
    return ExecutorServiceHolder.instance;
  }

  private DefaultExecutorServiceFactory() {}
}
