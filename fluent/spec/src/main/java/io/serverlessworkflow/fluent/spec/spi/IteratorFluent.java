package io.serverlessworkflow.fluent.spec.spi;

import java.util.function.Consumer;

import io.serverlessworkflow.fluent.spec.BaseTaskItemListBuilder;

public interface IteratorFluent<SELF, L extends BaseTaskItemListBuilder<L>> {

    /**
     * The name of the variable used to store the index of the current item being enumerated.
     * Defaults to index.
     */
    SELF at(String at);

    /**
     * `do` in the specification.
     * <p/>
     * The tasks to perform for each consumed item.
     */
    SELF tasks(Consumer<L> doBuilderConsumer);

}
