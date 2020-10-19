package io.github.noeppi_noeppi.libs.invocationwrappers;

import java.lang.reflect.Method;

public interface InvocationWrapper {

    Option<Object> invoke(Object proxy, Method method, Object[] args) throws Throwable;
}
