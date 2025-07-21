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

import io.serverlessworkflow.api.types.ErrorDetails;
import io.serverlessworkflow.api.types.ErrorTitle;
import io.serverlessworkflow.api.types.ErrorType;
import io.serverlessworkflow.api.types.RaiseTask;
import io.serverlessworkflow.api.types.RaiseTaskConfiguration;
import io.serverlessworkflow.api.types.RaiseTaskError;
import io.serverlessworkflow.api.types.UriTemplate;
import java.net.URI;
import java.util.function.Consumer;

public class RaiseTaskBuilder extends TaskBaseBuilder<RaiseTaskBuilder> {

  private final RaiseTask raiseTask;

  RaiseTaskBuilder() {
    this.raiseTask = new RaiseTask();
    setTask(raiseTask);
  }

  @Override
  protected RaiseTaskBuilder self() {
    return this;
  }

  public RaiseTaskBuilder error(Consumer<RaiseTaskErrorBuilder> consumer) {
    final RaiseTaskErrorBuilder raiseTaskErrorBuilder = new RaiseTaskErrorBuilder();
    consumer.accept(raiseTaskErrorBuilder);
    this.raiseTask.setRaise(new RaiseTaskConfiguration().withError(raiseTaskErrorBuilder.build()));
    return this;
  }

  // TODO: validation, one or the other

  public RaiseTaskBuilder error(String errorReference) {
    this.raiseTask.setRaise(
        new RaiseTaskConfiguration()
            .withError(new RaiseTaskError().withRaiseErrorReference(errorReference)));
    return this;
  }

  public RaiseTask build() {
    return this.raiseTask;
  }

  public static final class RaiseTaskErrorBuilder {
    private final io.serverlessworkflow.api.types.Error error;

    private RaiseTaskErrorBuilder() {
      this.error = new io.serverlessworkflow.api.types.Error();
    }

    public RaiseTaskErrorBuilder type(String expression) {
      this.error.setType(new ErrorType().withExpressionErrorType(expression));
      return this;
    }

    public RaiseTaskErrorBuilder type(URI errorType) {
      this.error.setType(
          new ErrorType().withLiteralErrorType(new UriTemplate().withLiteralUri(errorType)));
      return this;
    }

    public RaiseTaskErrorBuilder status(int status) {
      this.error.setStatus(status);
      return this;
    }

    // TODO: change signature to Expression interface since literal and expressions are String

    public RaiseTaskErrorBuilder title(String expression) {
      this.error.setTitle(new ErrorTitle().withExpressionErrorTitle(expression));
      return this;
    }

    public RaiseTaskErrorBuilder detail(String expression) {
      this.error.setDetail(new ErrorDetails().withExpressionErrorDetails(expression));
      return this;
    }

    public RaiseTaskError build() {
      return new RaiseTaskError().withRaiseErrorDefinition(this.error);
    }
  }
}
