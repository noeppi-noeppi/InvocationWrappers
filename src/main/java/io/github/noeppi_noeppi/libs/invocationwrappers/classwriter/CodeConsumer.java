package io.github.noeppi_noeppi.libs.invocationwrappers.classwriter;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface CodeConsumer<T> {


    void accept(T t) throws IOException, ReflectiveOperationException;

    default CodeConsumer<T> andThen(CodeConsumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            this.accept(t);
            after.accept(t);
        };
    }

    default CodeConsumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
