package io.github.noeppi_noeppi.libs.invocationwrappers.classwriter;

import io.github.noeppi_noeppi.libs.invocationwrappers.InvocationWrapper;
import io.github.noeppi_noeppi.libs.invocationwrappers.Option;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static io.github.noeppi_noeppi.libs.invocationwrappers.classwriter.ClassFileWriter.*;

public class CodeUtil {

    public static void writeConstructorCode(DataOutput out, ConstantPool pool, String className, Class<?> superClass, Constructor<?> ctor) throws IOException, ReflectiveOperationException {
        writeCode(out, pool,
                Math.max(stackSize(true, ctor.getParameterTypes()), 1 + stackSize(InvocationWrapper.class)),
                stackSize(true, ctor.getParameterTypes()) + stackSize(InvocationWrapper.class),
                code -> {
                    Class<?>[] classesSuper = ctor.getParameterTypes();
                    Class<?>[] classes = new Class[classesSuper.length + 1];
                    classes[0] = InvocationWrapper.class;
                    System.arraycopy(classesSuper, 0, classes, 1, classesSuper.length);

                    loadThis(code);
                    loadLocal(code, true, 0, classes);
                    putField(code, pool.addField(className, ClassFileWriter.HANDLER_FIELD, ClassFileWriter.HANDLER_FIELD_DESC));
                    loadThis(code);
                    for (int i = 1; i < classes.length; i++) {
                        loadLocal(code, true, i, classes);
                    }
                    invokeSpecial(code, pool.addMethod(ctor));
                    finish(code, void.class);
                }, (attr, code) -> {
                    writeU2(attr, 0); // Exception table size (we have no)
                    writeU2(attr, 0); // Additional attributes size (we have no)
                });
    }

    public static void writeMethodCode(DataOutput out, ConstantPool pool, String className, Class<?> superClass, Method method) throws IOException, ReflectiveOperationException {
        writeCode(out, pool,
                Math.max(stackSize(true, method.getParameterTypes()), 8),
                stackSize(true, method.getParameterTypes()) + stackSize(Option.class),
                code -> {
                    Class<?>[] classes = method.getParameterTypes();

                    loadThis(code);
                    // stack: this
                    loadField(code, pool.addField(className, ClassFileWriter.HANDLER_FIELD, ClassFileWriter.HANDLER_FIELD_DESC));
                    // stack: handler
                    loadThis(code);
                    // stack: handler, this
                    loadPool(code, pool.addClass(method.getDeclaringClass()));
                    // stack: handler, this, superClass
                    loadPool(code, pool.addJavaString(method.getName()));
                    // stack: handler, this, superClass, methodName
                    loadPool(code, pool.addJavaInt(classes.length));
                    // stack: handler, this, superClass, methodName, paramCount
                    loadNewArrayConsumeLength(code, pool.addClass(Class.class));
                    // stack: handler, this, superClass, methodName, paramTypes
                    for (int i = 0; i < classes.length; i++) {
                        loadCopy(code);
                        // stack: handler, this, superClass, methodName, paramTypes, paramTypes
                        loadPool(code, pool.addJavaInt(i));
                        // stack: handler, this, superClass, methodName, paramTypes, paramTypes, i
                        loadClass(code, pool, classes[i]);
                        // stack: handler, this, superClass, methodName, paramTypes, paramTypes, i, classes[i]
                        putArray(code);
                        // stack: handler, this, superClass, methodName, paramTypes
                    }
                    invokeVirtual(code, pool.addMethod(Class.class.getMethod("getDeclaredMethod", String.class, Class[].class)));
                    // stack: handler, this, method
                    loadPool(code, pool.addJavaInt(classes.length));
                    // stack: handler, this, method, paramCount
                    loadNewArrayConsumeLength(code, pool.addClass(Object.class));
                    // stack: handler, this, method, params
                    for (int i = 0; i < classes.length; i++) {
                        loadCopy(code);
                        // stack: handler, this, method, params, params
                        loadPool(code, pool.addJavaInt(i));
                        // stack: handler, this, method, params, params, i
                        loadLocal(code, true, i, classes);
                        // stack: handler, this, method, params, params, i, param
                        box(code, pool, classes[i]);
                        // stack: handler, this, method, params, params, i, param
                        putArray(code);
                        // stack: handler, this, method, params
                    }
                    Method invokeMethod = InvocationWrapper.class.getMethod("invoke", Object.class, Method.class, Object[].class);
                    invokeInterface(code, pool.addMethod(invokeMethod), stackSize(true, invokeMethod.getParameterTypes()));
                    // stack: optional
                    putLocalNoParam(code, true, classes, Option.class, 0);
                    // stack:
                    loadLocalNoParam(code, true, classes, Option.class, 0);
                    // stack: optional
                    invokeVirtual(code, pool.addMethod(Option.class.getMethod("isPresent")));
                    // stack: isPresent
                    ifZero(code, 3 + 2 + 3 + 3 + 3 + 1); // ifZero + loadLocalNoParam + invokeVirtual + checkCast + unbox + checkCast + finish

                    // stack:
                    loadLocalNoParam(code, true, classes, Option.class, 0);
                    // stack: optional
                    invokeVirtual(code, pool.addMethod(Option.class.getMethod("get")));
                    // stack: result
                    checkCast(code, pool.addClass(boxed(method.getReturnType(), false)));
                    // stack: result
                    unbox(code, pool, method.getReturnType());
                    // stack: result
                    finish(code, method.getReturnType());

                    // StackMap Frame (end - (1 + (2 * classes.length) + 3 + 1) (loadThis + (loadLocal * classes.length) + invokeSpecial + finish
                    // stack:
                    loadThis(code);
                    // stack: this
                    for (int i = 0; i < classes.length; i++) {
                        loadLocal(code, true, i, classes);
                    }
                    // stack: this, params...
                    invokeSpecial(code, pool.addMethod(method));
                    // stack: result
                    finish(code, method.getReturnType());
                }, (attr, code) -> {
                    writeU2(attr, 0); // Exception table size (we have no)
                    writeU2(attr, 1); // Additional attributes size (Write StackMap)
                    writeMethodStackMap(attr, pool, className, superClass, method, code);
                });
    }

    public static void writeMethodStackMap(DataOutput out, ConstantPool pool, String className, Class<?> superClass, Method method, byte[] code) throws IOException, ReflectiveOperationException {
        writeStackMap(out, pool, 1,
                stackMap -> {
                    Class<?>[] classes = method.getParameterTypes();

                    writeU1(stackMap, 252); // append_frame - additional variables: 1
                    writeU2(stackMap, code.length - (1 + (2 * classes.length) + 3 + 1));
                    writeU1(stackMap, 7); // Object_variable_info
                    writeU2(stackMap, pool.getClass(Option.class));
                });
    }

    public static void writeCode(DataOutput out, ConstantPool pool, int maxStack, int maxLocal, CodeConsumer<DataOutput> code, CodeBiConsumer<DataOutput, byte[]> attributes) throws IOException, ReflectiveOperationException {
        writeU2(out, pool.addUTF8("Code"));
        ByteArrayOutputStream attrBytes = new ByteArrayOutputStream();
        DataOutputStream attrData = new DataOutputStream(attrBytes);

        writeU2(attrData, maxStack);
        writeU2(attrData, maxLocal);

        ByteArrayOutputStream codeBytes = new ByteArrayOutputStream();
        DataOutputStream codeData = new DataOutputStream(codeBytes);

        code.accept(codeData);
        codeData.close();
        codeBytes.close();

        byte[] writtenCode = codeBytes.toByteArray();
        writeU4(attrData, writtenCode.length);
        attrData.write(writtenCode);

        attributes.accept(attrData, writtenCode);

        attrData.close();
        attrBytes.close();

        byte[] writtenAttr = attrBytes.toByteArray();
        writeU4(out, writtenAttr.length);
        out.write(writtenAttr);
    }

    public static void writeStackMap(DataOutput out, ConstantPool pool, int stackMapEntries, CodeConsumer<DataOutput> code) throws IOException, ReflectiveOperationException {
        writeU2(out, pool.addUTF8("StackMapTable"));
        ByteArrayOutputStream attrBytes = new ByteArrayOutputStream();
        DataOutputStream attrData = new DataOutputStream(attrBytes);

        writeU2(attrData, stackMapEntries);
        code.accept(attrData);
        attrData.close();
        attrBytes.close();

        byte[] writtenAttr = attrBytes.toByteArray();
        writeU4(out, writtenAttr.length);
        out.write(writtenAttr);
    }


    // May use null for unknown-non-primitive class
    public static int stackSize(boolean includeThis, Class<?>... classes) {
        int stackSize = includeThis ? 1 : 0;
        for (Class<?> clazz : classes) {
            stackSize += stackSize(clazz);
        }
        return stackSize;
    }

    public static int stackSize(Class<?> clazz) {
        if (long.class.equals(clazz) || double.class.equals(clazz)) {
            return 2;
        } else {
            return 1;
        }
    }

    public static void loadThis(DataOutput out) throws IOException {
        out.write(42); // aload_0
    }

    public static void loadLocal(DataOutput out, boolean includeThis, int index, Class<?>... classes) throws IOException {
        int offset = includeThis ? 1 : 0;
        for (int i = 0; i < index; i++) {
            offset += stackSize(classes[i]);
        }
        Class<?> clazz = classes[index];
        if (boolean.class.equals(clazz) || byte.class.equals(clazz) || char.class.equals(clazz)
                || short.class.equals(clazz) || int.class.equals(clazz)) {
            out.write(21); // iload
            out.write(offset);
        } else if (long.class.equals(clazz)) {
            out.write(22); // lload
            out.write(offset);
        } else if (float.class.equals(clazz)) {
            out.write(23); // fload
            out.write(offset);
        } else if (double.class.equals(clazz)) {
            out.write(24); // dload
            out.write(offset);
        } else {
            out.write(25); // aload
            out.write(offset);
        }
    }

    public static void loadLocalNoParam(DataOutput out, boolean includeThis, Class<?>[] paramClasses, Class<?> clazz, int idx) throws IOException {
        int offset = includeThis ? 1 : 0;
        for (Class<?> paramClass : paramClasses) {
            offset += stackSize(paramClass);
        }
        offset += idx;
        if (boolean.class.equals(clazz) || byte.class.equals(clazz) || char.class.equals(clazz)
                || short.class.equals(clazz) || int.class.equals(clazz)) {
            out.write(21); // iload
            out.write(offset);
        } else if (long.class.equals(clazz)) {
            out.write(22); // lload
            out.write(offset);
        } else if (float.class.equals(clazz)) {
            out.write(23); // fload
            out.write(offset);
        } else if (double.class.equals(clazz)) {
            out.write(24); // dload
            out.write(offset);
        } else {
            out.write(25); // aload
            out.write(offset);
        }
    }

    public static void putLocalNoParam(DataOutput out, boolean includeThis, Class<?>[] paramClasses, Class<?> clazz, int idx) throws IOException {
        int offset = includeThis ? 1 : 0;
        for (Class<?> paramClass : paramClasses) {
            offset += stackSize(paramClass);
        }
        offset += idx;
        if (boolean.class.equals(clazz) || byte.class.equals(clazz) || char.class.equals(clazz)
                || short.class.equals(clazz) || int.class.equals(clazz)) {
            out.write(54); // istore
            out.write(offset);
        } else if (long.class.equals(clazz)) {
            out.write(55); // lstore
            out.write(offset);
        } else if (float.class.equals(clazz)) {
            out.write(56); // fstore
            out.write(offset);
        } else if (double.class.equals(clazz)) {
            out.write(57); // dstore
            out.write(offset);
        } else {
            out.write(58); // astore
            out.write(offset);
        }
    }


    public static void invokeSpecial(DataOutput out, int ref) throws IOException {
        out.write(183); // invokespecial
        out.writeShort(ref);
    }

    public static void invokeVirtual(DataOutput out, int ref) throws IOException {
        out.write(182); // invokevirtual
        out.writeShort(ref);
    }

    public static void invokeStatic(DataOutput out, int ref) throws IOException {
        out.write(184); // invokestatic
        out.writeShort(ref);
    }

    public static void invokeInterface(DataOutput out, int ref, int count) throws IOException {
        out.write(185); // invokeinterface
        out.writeShort(ref);
        out.write(count);
        out.write(0);
    }

    public static void putField(DataOutput out, int ref) throws IOException {
        out.write(181); // putfield
        out.writeShort(ref);
    }

    public static void loadField(DataOutput out, int ref) throws IOException {
        out.write(180); // getfield
        out.writeShort(ref);
    }

    public static void loadStaticField(DataOutput out, int ref) throws IOException {
        out.write(178); // getstatic
        out.writeShort(ref);
    }

    public static void loadPool(DataOutput out, int ref) throws IOException {
        out.write(19); // ldc_w
        out.writeShort(ref);
    }

    public static void loadNewArrayConsumeLength(DataOutput out, int typeRef) throws IOException {
        out.write(189); // anewarray
        out.writeShort(typeRef);
    }

    public static void loadCopy(DataOutput out) throws IOException {
        out.write(89); // dup
    }

    public static void putArray(DataOutput out) throws IOException {
        out.write(83); // aastore
    }

    public static void ifZero(DataOutput out, int jmpOff) throws IOException {
        out.write(153); // ifeq
        out.writeShort(jmpOff);
    }

    public static void checkCast(DataOutput out, int ref) throws IOException {
        out.write(192); // checkcast
        out.writeShort(ref);
    }

    public static void finish(DataOutput out, Class<?> clazz) throws IOException {
        if (void.class.equals(clazz)) {
            out.write(177); // return
        } else if (boolean.class.equals(clazz) || byte.class.equals(clazz) || char.class.equals(clazz)
                || short.class.equals(clazz) || int.class.equals(clazz)) {
            out.write(172); // ireturn
        } else if (long.class.equals(clazz)) {
            out.write(173); // lreturn
        } else if (float.class.equals(clazz)) {
            out.write(174); // freturn
        } else if (double.class.equals(clazz)) {
            out.write(175); // dreturn
        } else {
            out.write(176); // areturn
        }
    }

    // Will always write 3 bytes
    public static void box(DataOutput out, ConstantPool pool, Class<?> providedTypeClass) throws IOException, ReflectiveOperationException {
        if (boolean.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Boolean.class.getMethod("valueOf", boolean.class)));
        } else if (byte.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Byte.class.getMethod("valueOf", byte.class)));
        } else if (char.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Character.class.getMethod("valueOf", char.class)));
        } else if (short.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Short.class.getMethod("valueOf", short.class)));
        } else if (int.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Integer.class.getMethod("valueOf", int.class)));
        } else if (long.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Long.class.getMethod("valueOf", long.class)));
        } else if (float.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Float.class.getMethod("valueOf", float.class)));
        } else if (double.class.equals(providedTypeClass)) {
            invokeStatic(out, pool.addMethod(Double.class.getMethod("valueOf", double.class)));
        } else {
            out.write(0);
            out.write(0);
            out.write(0);
        }
    }

    // Will always write 3 bytes
    public static void unbox(DataOutput out, ConstantPool pool, Class<?> desiredTypeClass) throws IOException, ReflectiveOperationException {
        if (boolean.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Boolean.class.getMethod("booleanValue")));
        } else if (byte.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Byte.class.getMethod("byteValue")));
        } else if (char.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Character.class.getMethod("charValue")));
        } else if (short.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Short.class.getMethod("shortValue")));
        } else if (int.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Integer.class.getMethod("intValue")));
        } else if (long.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Long.class.getMethod("longValue")));
        } else if (float.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Float.class.getMethod("floatValue")));
        } else if (double.class.equals(desiredTypeClass)) {
            invokeVirtual(out, pool.addMethod(Double.class.getMethod("doubleValue")));
        } else {
            out.write(0);
            out.write(0);
            out.write(0);
        }
    }

    public static Class<?> boxed(Class<?> clazz, boolean includeVoid) {
        if (boolean.class.equals(clazz)) {
            return Boolean.class;
        } else if (byte.class.equals(clazz)) {
            return Byte.class;
        } else if (char.class.equals(clazz)) {
            return Character.class;
        } else if (short.class.equals(clazz)) {
            return Short.class;
        } else if (int.class.equals(clazz)) {
            return Integer.class;
        } else if (long.class.equals(clazz)) {
            return Long.class;
        } else if (float.class.equals(clazz)) {
            return Float.class;
        } else if (double.class.equals(clazz)) {
            return Double.class;
        } else if (void.class.equals(clazz) && includeVoid) {
            return Void.class;
        } else {
            return clazz;
        }
    }

    public static void loadClass(DataOutput out, ConstantPool pool, Class<?> clazz) throws IOException {
        if (boolean.class.equals(clazz) || byte.class.equals(clazz) || char.class.equals(clazz) || short.class.equals(clazz)
                || int.class.equals(clazz) || long.class.equals(clazz) || float.class.equals(clazz) || double.class.equals(clazz)
                || void.class.equals(clazz)) {
            System.out.println(boxed(clazz, true));
            loadStaticField(out, pool.addField(boxed(clazz, true), "TYPE", "L" + Class.class.getName().replace('.', '/') + ";"));
        } else {
            loadPool(out, pool.addClass(clazz));
        }
    }
}
