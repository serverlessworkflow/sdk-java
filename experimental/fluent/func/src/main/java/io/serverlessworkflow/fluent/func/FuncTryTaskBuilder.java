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
package io.serverlessworkflow.fluent.func;

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
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TimeoutAfter;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.TryTaskCatch;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FuncTryTaskBuilder extends TaskBaseBuilder<FuncTryTaskBuilder>
    implements FuncTaskTransformations<FuncTryTaskBuilder>,
        ConditionalTaskBuilder<FuncTryTaskBuilder> {

  private final TryTask tryTask;

  FuncTryTaskBuilder() {
    this.tryTask = new TryTask();
    setTask(this.tryTask);
  }

  @Override
  protected FuncTryTaskBuilder self() {
    return this;
  }

  public FuncTryTaskBuilder tryHandler(Consumer<FuncTaskItemListBuilder> consumer) {
    List<TaskItem> existingTasks = this.tryTask.getTry();
    int currentOffset = (existingTasks == null) ? 0 : existingTasks.size();

    final FuncTaskItemListBuilder taskItemListBuilder = new FuncTaskItemListBuilder(currentOffset);
    consumer.accept(taskItemListBuilder);

    List<TaskItem> newTasks = taskItemListBuilder.build();
    if (existingTasks == null || existingTasks.isEmpty()) {
      this.tryTask.setTry(newTasks);
    } else {
      List<TaskItem> merged = new ArrayList<>(existingTasks);
      merged.addAll(newTasks);
      this.tryTask.setTry(merged);
    }

    return this;
  }

  public FuncTryTaskBuilder catchHandler(Consumer<TryTaskCatchBuilder> consumer) {
    final TryTaskCatchBuilder catchBuilder = new TryTaskCatchBuilder();
    consumer.accept(catchBuilder);
    this.tryTask.setCatch(catchBuilder.build());
    return this;
  }

  public TryTask build() {
    return tryTask;
  }

  public static final class TryTaskCatchBuilder {
    private final TryTaskCatch tryTaskCatch;

    TryTaskCatchBuilder() {
      this.tryTaskCatch = new TryTaskCatch();
    }

    public TryTaskCatchBuilder as(final String as) {
      this.tryTaskCatch.setAs(as);
      return this;
    }

    public TryTaskCatchBuilder when(final String when) {
      this.tryTaskCatch.setWhen(when);
      return this;
    }

    public TryTaskCatchBuilder exceptWhen(final String exceptWhen) {
      this.tryTaskCatch.setExceptWhen(exceptWhen);
      return this;
    }

    public TryTaskCatchBuilder retry(Consumer<RetryPolicyBuilder> consumer) {
      final RetryPolicyBuilder retryPolicyBuilder = new RetryPolicyBuilder();
      consumer.accept(retryPolicyBuilder);
      this.tryTaskCatch.setRetry(new Retry().withRetryPolicyDefinition(retryPolicyBuilder.build()));
      return this;
    }

    public TryTaskCatchBuilder errorsWith(Consumer<CatchErrorsBuilder> consumer) {
      final CatchErrorsBuilder catchErrorsBuilder = new CatchErrorsBuilder();
      consumer.accept(catchErrorsBuilder);
      this.tryTaskCatch.setErrors(catchErrorsBuilder.build());
      return this;
    }

    public TryTaskCatchBuilder doTasks(Consumer<FuncTaskItemListBuilder> consumer) {
      List<TaskItem> existingTasks = this.tryTaskCatch.getDo();
      int currentOffset = (existingTasks == null) ? 0 : existingTasks.size();

      final FuncTaskItemListBuilder taskItemListBuilder =
          new FuncTaskItemListBuilder(currentOffset);
      consumer.accept(taskItemListBuilder);

      List<TaskItem> newTasks = taskItemListBuilder.build();
      if (existingTasks == null || existingTasks.isEmpty()) {
        this.tryTaskCatch.setDo(newTasks);
      } else {
        List<TaskItem> merged = new ArrayList<>(existingTasks);
        merged.addAll(newTasks);
        this.tryTaskCatch.setDo(merged);
      }

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

  public static final class DurationInlineBuilder {
    private final io.serverlessworkflow.api.types.DurationInline durationInline;

    DurationInlineBuilder() {
      this.durationInline = new io.serverlessworkflow.api.types.DurationInline();
    }

    public DurationInlineBuilder seconds(int seconds) {
      this.durationInline.setSeconds(seconds);
      return this;
    }

    public DurationInlineBuilder milliseconds(int milliseconds) {
      this.durationInline.setMilliseconds(milliseconds);
      return this;
    }

    public DurationInlineBuilder minutes(int minutes) {
      this.durationInline.setMinutes(minutes);
      return this;
    }

    public DurationInlineBuilder hours(int hours) {
      this.durationInline.setHours(hours);
      return this;
    }

    public DurationInlineBuilder days(int days) {
      this.durationInline.setDays(days);
      return this;
    }

    public io.serverlessworkflow.api.types.DurationInline build() {
      return this.durationInline;
    }
  }
}
