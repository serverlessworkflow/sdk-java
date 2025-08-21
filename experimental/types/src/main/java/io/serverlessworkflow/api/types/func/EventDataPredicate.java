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
package io.serverlessworkflow.api.types.func;

import io.serverlessworkflow.api.types.EventData;
import java.util.Objects;
import java.util.function.Predicate;

public class EventDataPredicate extends EventData {

  public <T> EventDataPredicate withPredicate(Predicate<T> predicate) {
    setObject(predicate);
    return this;
  }

  public <T> EventDataPredicate withPredicate(Predicate<T> predicate, Class<T> clazz) {
    Objects.requireNonNull(clazz);
    setObject(new TypedPredicate<>(predicate, clazz));
    return this;
  }
}
