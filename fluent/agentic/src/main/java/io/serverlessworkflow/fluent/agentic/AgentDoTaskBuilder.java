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
package io.serverlessworkflow.fluent.agentic;

import io.serverlessworkflow.fluent.agentic.spi.AgentDoFluent;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncForTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncForkTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSetTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.spec.BaseDoTaskBuilder;
import java.util.function.Consumer;

public class AgentDoTaskBuilder
    extends BaseDoTaskBuilder<AgentDoTaskBuilder, AgentTaskItemListBuilder>
    implements ConditionalTaskBuilder<AgentDoTaskBuilder>, AgentDoFluent<AgentDoTaskBuilder> {

  public AgentDoTaskBuilder() {
    super(new AgentTaskItemListBuilder());
  }

  @Override
  protected AgentDoTaskBuilder self() {
    return this;
  }

  @Override
  public AgentDoTaskBuilder agent(String name, Object agent) {
    this.listBuilder().agent(name, agent);
    return self();
  }

  @Override
  public AgentDoTaskBuilder sequence(String name, Object... agents) {
    this.listBuilder().sequence(name, agents);
    return self();
  }

  @Override
  public AgentDoTaskBuilder loop(String name, Consumer<LoopAgentsBuilder> builder) {
    this.listBuilder().loop(name, builder);
    return self();
  }

  @Override
  public AgentDoTaskBuilder loop(String name, LoopAgentsBuilder builder) {
    this.listBuilder().loop(name, builder);
    return self();
  }

  @Override
  public AgentDoTaskBuilder parallel(String name, Object... agents) {
    this.listBuilder().parallel(name, agents);
    return self();
  }

  @Override
  public AgentDoTaskBuilder callFn(String name, Consumer<FuncCallTaskBuilder> cfg) {
    this.listBuilder().callFn(name, cfg);
    return self();
  }

  @Override
  public AgentDoTaskBuilder emit(String name, Consumer<FuncEmitTaskBuilder> itemsConfigurer) {
    this.listBuilder().emit(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentDoTaskBuilder forEach(String name, Consumer<FuncForTaskBuilder> itemsConfigurer) {
    this.listBuilder().forEach(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentDoTaskBuilder fork(String name, Consumer<FuncForkTaskBuilder> itemsConfigurer) {
    this.listBuilder().fork(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentDoTaskBuilder set(String name, Consumer<FuncSetTaskBuilder> itemsConfigurer) {
    this.listBuilder().set(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentDoTaskBuilder set(String name, String expr) {
    this.listBuilder().set(name, expr);
    return self();
  }

  @Override
  public AgentDoTaskBuilder switchCase(
      String name, Consumer<FuncSwitchTaskBuilder> itemsConfigurer) {
    this.listBuilder().switchCase(name, itemsConfigurer);
    return self();
  }
}
