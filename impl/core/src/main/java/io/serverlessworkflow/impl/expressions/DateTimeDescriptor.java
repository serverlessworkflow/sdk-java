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
package io.serverlessworkflow.impl.expressions;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class DateTimeDescriptor {

  private final Instant instant;

  public static DateTimeDescriptor from(Instant instant) {
    return new DateTimeDescriptor(instant);
  }

  private DateTimeDescriptor(Instant instant) {
    this.instant = instant;
  }

  @JsonProperty("iso8601")
  public String iso8601() {
    return instant.toString();
  }

  @JsonProperty("epoch")
  public Epoch epoch() {
    return Epoch.of(instant);
  }

  public static record Epoch(long seconds, long milliseconds) {
    public static Epoch of(Instant instant) {
      return new Epoch(instant.getEpochSecond(), instant.toEpochMilli());
    }
  }
}
