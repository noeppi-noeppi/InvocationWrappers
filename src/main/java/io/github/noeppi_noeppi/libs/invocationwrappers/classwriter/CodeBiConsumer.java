package io.github.noeppi_noeppi.libs.invocationwrappers.classwriter;

import java.io.IOException;

@FunctionalInterface
public interface CodeBiConsumer<T, U> {

    void accept(T t, U u) throws IOException, ReflectiveOperationException;
}
