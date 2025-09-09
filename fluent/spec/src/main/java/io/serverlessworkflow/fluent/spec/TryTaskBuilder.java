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

import io.serverlessworkflow.api.types.CatchErrors;
import io.serverlessworkflow.api.types.Constant;
import io.serverlessworkflow.api.types.ConstantBackoff;
import io.serverlessworkflow.api.types.ErrorFilter;
import io.serverlessworkflow.api.types.Exponential;
import io.serverlessworkflow.api.types.ExponentialBackOff;
import io.serverlessworkflow.api.types.Linear;
import io.serverlessworkflow.api.types.LinearBackoff;
import io.serverlessworkflow.api.types.Retry;
import io.serverlessworkflow.api.types.RetryBackoff;
import io.serverlessworkflow.api.types.RetryLimit;
import io.serverlessworkflow.api.types.RetryLimitAttempt;
import io.serverlessworkflow.api.types.RetryPolicy;
import io.serverlessworkflow.api.types.RetryPolicyJitter;
import io.serverlessworkflow.api.types.TimeoutAfter;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.TryTaskCatch;
import java.util.function.Consumer;

public class TryTaskBuilder<T extends BaseTaskItemListBuilder<T>>
    extends TaskBaseBuilder<TryTaskBuilder<T>> {

  private final TryTask tryTask;
  private final T doTaskBuilderFactory;

  TryTaskBuilder(T doTaskBuilderFactory) {
    this.tryTask = new TryTask();
    this.doTaskBuilderFactory = doTaskBuilderFactory;
  }

  @Override
  protected TryTaskBuilder<T> self() {
    return this;
  }

  public TryTaskBuilder<T> tryHandler(Consumer<T> consumer) {
    final T taskItemListBuilder = this.doTaskBuilderFactory.newItemListBuilder();
    consumer.accept(taskItemListBuilder);
    this.tryTask.setTry(taskItemListBuilder.build());
    return this;
  }

  public TryTaskBuilder<T> catchHandler(Consumer<TryTaskCatchBuilder<T>> consumer) {
    final TryTaskCatchBuilder<T> catchBuilder =
        new TryTaskCatchBuilder<>(this.doTaskBuilderFactory);
    consumer.accept(catchBuilder);
    this.tryTask.setCatch(catchBuilder.build());
    return this;
  }

  public TryTask build() {
    return tryTask;
  }

  public static final class TryTaskCatchBuilder<T extends BaseTaskItemListBuilder<T>> {
    private final TryTaskCatch tryTaskCatch;
    private final T doTaskBuilderFactory;

    TryTaskCatchBuilder(T doTaskBuilderFactory) {
      this.doTaskBuilderFactory = doTaskBuilderFactory;
      this.tryTaskCatch = new TryTaskCatch();
    }

    public TryTaskCatchBuilder<T> as(final String as) {
      this.tryTaskCatch.setAs(as);
      return this;
    }

    public TryTaskCatchBuilder<T> when(final String when) {
      this.tryTaskCatch.setWhen(when);
      return this;
    }

    public TryTaskCatchBuilder<T> exceptWhen(final String exceptWhen) {
      this.tryTaskCatch.setExceptWhen(exceptWhen);
      return this;
    }

    public TryTaskCatchBuilder<T> retry(Consumer<RetryPolicyBuilder> consumer) {
      final RetryPolicyBuilder retryPolicyBuilder = new RetryPolicyBuilder();
      consumer.accept(retryPolicyBuilder);
      this.tryTaskCatch.setRetry(new Retry().withRetryPolicyDefinition(retryPolicyBuilder.build()));
      return this;
    }

    public TryTaskCatchBuilder<T> errorsWith(Consumer<CatchErrorsBuilder> consumer) {
      final CatchErrorsBuilder catchErrorsBuilder = new CatchErrorsBuilder();
      consumer.accept(catchErrorsBuilder);
      this.tryTaskCatch.setErrors(catchErrorsBuilder.build());
      return this;
    }

    public TryTaskCatchBuilder<T> doTasks(Consumer<T> consumer) {
      final T taskItemListBuilder = this.doTaskBuilderFactory.newItemListBuilder();
      consumer.accept(taskItemListBuilder);
      this.tryTaskCatch.setDo(taskItemListBuilder.build());
      return this;
    }

    public TryTaskCatch build() {
      return tryTaskCatch;
    }
  }

  public static final class CatchErrorsBuilder {
    private final ErrorFilter errorFilter;

    CatchErrorsBuilder() {
      this.errorFilter = new ErrorFilter();
    }

    public CatchErrorsBuilder type(final String type) {
      this.errorFilter.setType(type);
      return this;
    }

    public CatchErrorsBuilder status(final int status) {
      this.errorFilter.setStatus(status);
      return this;
    }

    public CatchErrorsBuilder instance(final String instance) {
      this.errorFilter.setInstance(instance);
      return this;
    }

    public CatchErrorsBuilder title(final String title) {
      this.errorFilter.setTitle(title);
      return this;
    }

    public CatchErrorsBuilder details(final String details) {
      this.errorFilter.setDetails(details);
      return this;
    }

    public CatchErrors build() {
      return new CatchErrors().withWith(this.errorFilter);
    }
  }

  public static final class RetryPolicyJitterBuilder {
    private final RetryPolicyJitter retryPolicyJitter;

    RetryPolicyJitterBuilder() {
      this.retryPolicyJitter = new RetryPolicyJitter();
    }

    public RetryPolicyJitterBuilder to(Consumer<DurationInlineBuilder> consumer) {
      final DurationInlineBuilder durationInlineBuilder = new DurationInlineBuilder();
      consumer.accept(durationInlineBuilder);
      this.retryPolicyJitter.setTo(
          new TimeoutAfter().withDurationInline(durationInlineBuilder.build()));
      return this;
    }

    public RetryPolicyJitterBuilder to(String expression) {
      this.retryPolicyJitter.setTo(new TimeoutAfter().withDurationExpression(expression));
      return this;
    }

    public RetryPolicyJitterBuilder from(Consumer<DurationInlineBuilder> consumer) {
      final DurationInlineBuilder durationInlineBuilder = new DurationInlineBuilder();
      consumer.accept(durationInlineBuilder);
      this.retryPolicyJitter.setFrom(
          new TimeoutAfter().withDurationInline(durationInlineBuilder.build()));
      return this;
    }

    public RetryPolicyJitterBuilder from(String expression) {
      this.retryPolicyJitter.setFrom(new TimeoutAfter().withDurationExpression(expression));
      return this;
    }

    public RetryPolicyJitter build() {
      return retryPolicyJitter;
    }
  }

  public static final class RetryPolicyBuilder {
    private final RetryPolicy retryPolicy;

    RetryPolicyBuilder() {
      this.retryPolicy = new RetryPolicy();
    }

    public RetryPolicyBuilder when(final String when) {
      this.retryPolicy.setWhen(when);
      return this;
    }

    public RetryPolicyBuilder exceptWhen(final String exceptWhen) {
      this.retryPolicy.setExceptWhen(exceptWhen);
      return this;
    }

    public RetryPolicyBuilder backoff(Consumer<BackoffBuilder> consumer) {
      final BackoffBuilder backoffBuilder = new BackoffBuilder();
      consumer.accept(backoffBuilder);
      this.retryPolicy.setBackoff(backoffBuilder.build());
      return this;
    }

    public RetryPolicyBuilder delay(Consumer<DurationInlineBuilder> consumer) {
      final DurationInlineBuilder builder = new DurationInlineBuilder();
      consumer.accept(builder);
      this.retryPolicy.setDelay(new TimeoutAfter().withDurationInline(builder.build()));
      return this;
    }

    public RetryPolicyBuilder delay(String expression) {
      this.retryPolicy.setDelay(new TimeoutAfter().withDurationExpression(expression));
      return this;
    }

    public RetryPolicyBuilder limit(Consumer<RetryLimitBuilder> consumer) {
      final RetryLimitBuilder limitBuilder = new RetryLimitBuilder();
      consumer.accept(limitBuilder);
      this.retryPolicy.setLimit(limitBuilder.build());
      return this;
    }

    public RetryPolicyBuilder jitter(Consumer<RetryPolicyJitterBuilder> consumer) {
      final RetryPolicyJitterBuilder jitterBuilder = new RetryPolicyJitterBuilder();
      consumer.accept(jitterBuilder);
      this.retryPolicy.setJitter(jitterBuilder.build());
      return this;
    }

    public RetryPolicy build() {
      return this.retryPolicy;
    }
  }

  public static final class RetryLimitBuilder {
    private final RetryLimit retryLimit;

    RetryLimitBuilder() {
      this.retryLimit = new RetryLimit();
    }

    public RetryLimitBuilder duration(Consumer<DurationInlineBuilder> consumer) {
      final DurationInlineBuilder builder = new DurationInlineBuilder();
      consumer.accept(builder);
      this.retryLimit.setDuration(new TimeoutAfter().withDurationInline(builder.build()));
      return this;
    }

    public RetryLimitBuilder duration(String expression) {
      this.retryLimit.setDuration(new TimeoutAfter().withDurationExpression(expression));
      return this;
    }

    public RetryLimitBuilder attempt(Consumer<RetryLimitAttemptBuilder> consumer) {
      final RetryLimitAttemptBuilder retryLimitAttemptBuilder = new RetryLimitAttemptBuilder();
      consumer.accept(retryLimitAttemptBuilder);
      this.retryLimit.setAttempt(retryLimitAttemptBuilder.build());
      return this;
    }

    public RetryLimit build() {
      return this.retryLimit;
    }
  }

  public static final class RetryLimitAttemptBuilder {
    private final RetryLimitAttempt retryLimitAttempt;

    RetryLimitAttemptBuilder() {
      this.retryLimitAttempt = new RetryLimitAttempt();
    }

    public RetryLimitAttemptBuilder count(int count) {
      this.retryLimitAttempt.setCount(count);
      return this;
    }

    public RetryLimitAttemptBuilder duration(Consumer<DurationInlineBuilder> consumer) {
      final DurationInlineBuilder builder = new DurationInlineBuilder();
      consumer.accept(builder);
      this.retryLimitAttempt.setDuration(new TimeoutAfter().withDurationInline(builder.build()));
      return this;
    }

    public RetryLimitAttemptBuilder duration(String expression) {
      this.retryLimitAttempt.setDuration(new TimeoutAfter().withDurationExpression(expression));
      return this;
    }

    public RetryLimitAttempt build() {
      return this.retryLimitAttempt;
    }
  }

  public static final class BackoffBuilder {
    private final RetryBackoff retryBackoff;
    private final ConstantBackoff constantBackoff;
    private final ExponentialBackOff exponentialBackOff;
    private final LinearBackoff linearBackoff;

    BackoffBuilder() {
      this.retryBackoff = new RetryBackoff();

      this.constantBackoff = new ConstantBackoff();
      this.constantBackoff.setConstant(new Constant());
      this.exponentialBackOff = new ExponentialBackOff();
      this.exponentialBackOff.setExponential(new Exponential());
      this.linearBackoff = new LinearBackoff();
      this.linearBackoff.setLinear(new Linear());
    }

    public BackoffBuilder constant(String key, String value) {
      this.constantBackoff.getConstant().withAdditionalProperty(key, value);
      return this;
    }

    public BackoffBuilder exponential(String key, String value) {
      this.exponentialBackOff.getExponential().withAdditionalProperty(key, value);
      return this;
    }

    public BackoffBuilder linear(String key, String value) {
      this.linearBackoff.getLinear().withAdditionalProperty(key, value);
      return this;
    }

    public RetryBackoff build() {
      this.retryBackoff.setConstantBackoff(constantBackoff);
      this.retryBackoff.setExponentialBackOff(exponentialBackOff);
      this.retryBackoff.setLinearBackoff(linearBackoff);
      return this.retryBackoff;
    }
  }
}
