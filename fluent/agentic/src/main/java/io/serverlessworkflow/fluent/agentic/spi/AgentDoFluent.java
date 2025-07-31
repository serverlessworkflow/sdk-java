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
package io.serverlessworkflow.fluent.agentic.spi;

import io.serverlessworkflow.fluent.agentic.LoopAgentsBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncDoFluent;
import java.util.UUID;
import java.util.function.Consumer;

public interface AgentDoFluent<SELF extends AgentDoFluent<SELF>> extends FuncDoFluent<SELF> {

  SELF agent(String name, Object agent);

  default SELF agent(Object agent) {
    return agent(UUID.randomUUID().toString(), agent);
  }

  SELF sequence(String name, Object... agents);

  default SELF sequence(Object... agents) {
    return sequence("seq-" + UUID.randomUUID(), agents);
  }

  SELF loop(String name, Consumer<LoopAgentsBuilder> builder);

  default SELF loop(Consumer<LoopAgentsBuilder> builder) {
    return loop("loop-" + UUID.randomUUID(), builder);
  }

  SELF loop(String name, LoopAgentsBuilder builder);

  default SELF loop(LoopAgentsBuilder builder) {
    return loop("loop-" + UUID.randomUUID(), builder);
  }

  SELF parallel(String name, Object... agents);

  default SELF parallel(Object... agents) {
    return parallel("par-" + UUID.randomUUID(), agents);
  }
}
