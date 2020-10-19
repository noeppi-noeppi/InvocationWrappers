package io.github.noeppi_noeppi.libs.invocationwrappers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class AccessTransformer {

    private static Field modifiersFieldC = null;
    private static Field modifiersFieldM = null;

    public static void transform(Class<?> clazz, boolean forcePublic) throws ReflectiveOperationException {
        if (forcePublic) {
            System.err.println("InvocationWrappers-AccessTransformer: Transforming class " + clazz.getName() + " with the forcePublic option set. Avoid this.");
        }

        if (clazz == null) {
            System.err.println("InvocationWrappers-AccessTransformer: AccessTransformer.transform called with an null argument. This is not supported.");
        } else if (clazz.isArray()) {
            System.err.println("InvocationWrappers-AccessTransformer: Refusing to transform " + clazz.getName() + ". Is an array.");
        } else if (clazz.isInterface()) {
            System.err.println("InvocationWrappers-AccessTransformer: Refusing to transform " + clazz.getName() + ". Is an interface.");
        } else if (clazz.isEnum()) {
            System.err.println("InvocationWrappers-AccessTransformer: Refusing to transform " + clazz.getName() + ". Is an enum.");
        } else if (Object.class.equals(clazz) || Class.class.equals(clazz) || Boolean.class.equals(clazz)
                || Byte.class.equals(clazz) || Character.class.equals(clazz) || Short.class.equals(clazz)
                || Integer.class.equals(clazz) || Long.class.equals(clazz) || Float.class.equals(clazz)
                || Double.class.equals(clazz) || Void.class.equals(clazz) || Error.class.isAssignableFrom(clazz)
                || String.class.equals(clazz) || clazz.isPrimitive() || void.class.equals(clazz)) {
            System.err.println("InvocationWrappers-AccessTransformer: Refusing to transform " + clazz.getName() + ". This class is not supported.");
        } else if (Modifier.isFinal(clazz.getModifiers())) {
            System.err.println("InvocationWrappers-AccessTransformer: Refusing to transform " + clazz.getName() + ". Is final.");
        } else {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            Method[] methods = clazz.getDeclaredMethods();
            for (Constructor<?> ctor : constructors) {
                int modifiers = ctor.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                if (Modifier.isFinal(modifiers) || !Modifier.isPublic(modifiers)) {
                    makeAvailable(clazz, ctor, forcePublic);
                }
            }
            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                if (Modifier.isFinal(modifiers) || !Modifier.isPublic(modifiers)) {
                    makeAvailable(clazz, method, forcePublic);
                }
            }
            Class<?> parent = clazz.getSuperclass();
            if (!Object.class.equals(parent)) {
                transform(parent, forcePublic);
            }
        }
    }

    private static void makeAvailable(Class<?> clazz, Constructor<?> ctor, boolean forcePublic) throws ReflectiveOperationException {
        try {
            if (modifiersFieldC == null) {
                modifiersFieldC = Constructor.class.getDeclaredField("modifiers");
                modifiersFieldC.setAccessible(true);
            }
            int modifiers = ctor.getModifiers();
            modifiers &= ~Modifier.FINAL;
            if (forcePublic) {
                 modifiers &= ~Modifier.PRIVATE;
                 modifiers &= ~Modifier.PROTECTED;
                 modifiers |= Modifier.PUBLIC;
            } else if (!Modifier.isPublic(modifiers)) {
                modifiers &= ~Modifier.PRIVATE;
                modifiers |= Modifier.PROTECTED;
            }
            modifiersFieldC.set(ctor, modifiers);
        } catch (ReflectiveOperationException e) {
            System.err.println("InvocationWrappers-AccessTransformer: Failed to make constructor " + ctor.toString() + " of class " + clazz + " available");
            throw e;
        }
    }

    private static void makeAvailable(Class<?> clazz, Method method, boolean forcePublic) throws ReflectiveOperationException {
        try {
            if (modifiersFieldM == null) {
                modifiersFieldM = Method.class.getDeclaredField("modifiers");
                modifiersFieldM.setAccessible(true);
            }
            int modifiers = method.getModifiers();
            modifiers &= ~Modifier.FINAL;
            if (forcePublic) {
                modifiers &= ~Modifier.PRIVATE;
                modifiers &= ~Modifier.PROTECTED;
                modifiers |= Modifier.PUBLIC;
            } else if (!Modifier.isPublic(modifiers)) {
                modifiers &= ~Modifier.PRIVATE;
                modifiers |= Modifier.PROTECTED;
            }
            modifiersFieldM.set(method, modifiers);
        } catch (ReflectiveOperationException e) {
            System.err.println("InvocationWrappers-AccessTransformer: Failed to make method " + method.toString() + " of class " + clazz + " available");
            throw e;
        }
    }
}
