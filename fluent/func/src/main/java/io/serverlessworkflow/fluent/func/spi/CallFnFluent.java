package io.serverlessworkflow.fluent.func.spi;

import java.util.UUID;
import java.util.function.Consumer;

import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;

public interface CallFnFluent<SELF extends TaskBaseBuilder<?>, LIST> {

    LIST callFn(String name, Consumer<SELF> cfg);

    default LIST callFn(Consumer<SELF> cfg) {
        return this.callFn(UUID.randomUUID().toString(), cfg);
    }
}
