package io.serverlessworkflow.fluent.spec.spi;

import java.util.function.Consumer;

import io.serverlessworkflow.fluent.spec.ExportBuilder;
import io.serverlessworkflow.fluent.spec.OutputBuilder;

public interface OutputFluent<SELF> {

    SELF output(Consumer<OutputBuilder> outputConsumer);

    SELF export(Consumer<ExportBuilder> exportConsumer);

    SELF exportAs(Object exportAs);

}
