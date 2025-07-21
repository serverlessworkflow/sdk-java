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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.Use;
import java.util.List;
import java.util.function.Consumer;

public class UseBuilder {

  private final Use use;

  UseBuilder() {
    this.use = new Use();
  }

  public UseBuilder secrets(final String... secrets) {
    if (secrets != null) {
      this.use.setSecrets(List.of(secrets));
    }
    return this;
  }

  public UseBuilder authentications(Consumer<UseAuthenticationsBuilder> authenticationsConsumer) {
    final UseAuthenticationsBuilder builder = new UseAuthenticationsBuilder();
    authenticationsConsumer.accept(builder);
    this.use.setAuthentications(builder.build());
    return this;
  }

  // TODO: implement the remaining `use` attributes

  public Use build() {
    return use;
  }
}
