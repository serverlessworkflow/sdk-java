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

/**
 * Framework-agnostic hook for capturing the context of the thread that starts a workflow instance
 * so it can be re-established around asynchronous task execution.
 */
@FunctionalInterface
public interface ContextPropagator {

  /**
   * Captures the current context. Invoked on the thread that starts the workflow instance (the
   * thread that calls {@link WorkflowInstance#start()}).
   *
   * @return a snapshot able to re-establish the captured context on another thread.
   */
  ContextSnapshot capture();

  ContextPropagator NOOP = () -> ContextSnapshot.NOOP;
}
