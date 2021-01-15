package io.github.noeppi_noeppi.libs.invocationwrappers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

// Just an example on how it works
public class Example {

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        Class<? extends Date> clazz = InvocationWrappers.createWrapped(Date.class);
        Constructor<? extends Date> ctor = clazz.getDeclaredConstructor(InvocationWrapper.class);
        Date date = ctor.newInstance(new Handler());

        System.out.println(date);
        System.out.println(date.getTime());
        //noinspection UnusedAssignment
        date = null;
        System.gc(); // Call finalize
    }

    public static class Handler implements InvocationWrapper {

        @Override
        public Option<Object> invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Date.class.getDeclaredMethod("toString").equals(method)) {
                // a method from Date
                return Option.of("No string for you!");
            } else if (Object.class.getDeclaredMethod("finalize").equals(method)) {
                // We can as well use a method that is not directly implemented in Date but in it's superclass Object
                System.out.println("Date was finalized");
                return Option.of(null); // Must be null for void methods if you don't want super to get called.
            } else if (Date.class.getDeclaredMethod("getTimeImpl").equals(method)) {
                // This will never get called as we can not override private and/or final methods.
                System.out.println("getTimeImpl was called. Returning 42");
                return Option.of(42L);
            } else if (Date.class.getDeclaredMethod("getTime").equals(method)) {
                // This however will work as the method is public and not final.
                System.out.println("getTime was called. Returning 42");
                return Option.of(42L);
            } else {
                return Option.empty(); // Call the super method
            }
        }
    }
}
