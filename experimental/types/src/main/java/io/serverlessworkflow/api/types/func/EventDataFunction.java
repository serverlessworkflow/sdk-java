package io.serverlessworkflow.api.types.func;

import io.serverlessworkflow.api.types.EventData;
import java.util.Objects;
import java.util.function.Function;

public class EventDataFunction extends EventData {

  public <T, V> EventData withFunction(Function<T, V> value) {
    setObject(value);
    return this;
  }

  public <T, V> EventData withFunction(Function<T, V> value, Class<T> argClass) {
    Objects.requireNonNull(argClass);
    setObject(new TypedFunction<>(value, argClass));
    return this;
  }
}
