package io.github.noeppi_noeppi.libs.invocationwrappers;

import java.util.NoSuchElementException;

// Is required because javas Optional may not have null values
public class Option<T> {

    private static final Option<Object> EMPTY = new Option<>(false, null);

    private final boolean present;
    private final T value;

    private Option(boolean present, T value) {
        this.present = present;
        if (present) {
            this.value = value;
        } else {
            this.value = null;
        }
    }

    public boolean isPresent() {
        return this.present;
    }

    public T get() {
        if (this.present) {
            return this.value;
        } else {
            throw new NoSuchElementException("The Option has no element.");
        }
    }

    public static <T> Option<T> empty() {
        //noinspection unchecked
        return (Option<T>) EMPTY;
    }

    public static <T> Option<T> of(T t) {
        return new Option<>(true, t);
    }
}
