package io.serverlessworkflow.fluent.agentic;

import java.util.function.Predicate;

import io.serverlessworkflow.api.types.AnyEventConsumptionStrategy;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.func.UntilPredicate;
import io.serverlessworkflow.fluent.spec.ListenTaskBuilder;

public class AgentListenTaskBuilder extends ListenTaskBuilder<AgentTaskItemListBuilder> {

    private UntilPredicate untilPredicate;

    public AgentListenTaskBuilder() {
        super(new AgentTaskItemListBuilder());
    }

    public <T> AgentListenTaskBuilder until(Predicate<T> predicate, Class<T> predClass) {
        untilPredicate = new UntilPredicate().withPredicate(predicate, predClass);
        return this;
    }

    @Override
    public ListenTask build() {
        ListenTask task = super.build();
        AnyEventConsumptionStrategy anyEvent =
                task.getListen().getTo().getAnyEventConsumptionStrategy();
        if (untilPredicate != null && anyEvent != null) {
            anyEvent.withUntil(untilPredicate);
        }
        return task;
    }

}
