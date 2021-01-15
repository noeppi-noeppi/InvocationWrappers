package io.github.noeppi_noeppi.libs.invocationwrappers;

import io.github.noeppi_noeppi.libs.invocationwrappers.classwriter.ClassFileWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class InvocationWrappers {

    // The new class will have all constructors the wrapped class has but every constructor has an InvocationWrapper
    // added as first argument.
    public static <T> Class<? extends T> createWrapped(Class<T> clazz) throws IOException, ReflectiveOperationException {
        return createWrapped(clazz, clazz.getPackage());
    }
    
    // This version allows for a custom destination package to be set to allow overriding of more methods that may
    // be package private. The default is the package of the superclass. This allows to overridepackage private
    // methods from the super class but not from the superclass of the superclass...
    public static <T> Class<? extends T> createWrapped(Class<T> clazz, Package destination) throws IOException, ReflectiveOperationException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bout);
        ClassFileWriter.write(out, clazz, destination);
        out.close();
        bout.close();

        byte[] bytes = bout.toByteArray();

        Method defineMethod = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
        defineMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Class<? extends T> wrapper = (Class<? extends T>) defineMethod.invoke(ClassLoader.getSystemClassLoader(), bytes, 0, bytes.length);

        Method resolveMethod = ClassLoader.class.getDeclaredMethod("resolveClass", Class.class);
        resolveMethod.setAccessible(true);
        resolveMethod.invoke(ClassLoader.getSystemClassLoader(), wrapper);

        return wrapper;
    }
}
