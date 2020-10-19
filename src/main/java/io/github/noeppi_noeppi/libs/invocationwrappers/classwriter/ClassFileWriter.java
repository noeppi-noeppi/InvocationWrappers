package io.github.noeppi_noeppi.libs.invocationwrappers.classwriter;

import io.github.noeppi_noeppi.libs.invocationwrappers.InvocationWrapper;
import io.github.noeppi_noeppi.libs.invocationwrappers.constant.ClassModifier;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ClassFileWriter {


    public static final String HANDLER_FIELD = "invocationwrapper$handler";
    public static final String HANDLER_FIELD_DESC = "L" + InvocationWrapper.class.getName().replace('.', '/') + ";";


    private static int idx = 0;

    public static void write(DataOutput out, Class<?> parent) throws IOException, ReflectiveOperationException {
        String className = "invocationwrappers.Synthetic$" + (idx++);

        ConstantPool pool = new ConstantPool();
        pool.addCompileClass(className);
        pool.addClass(parent);
        pool.addUTF8(HANDLER_FIELD);
        pool.addUTF8(HANDLER_FIELD_DESC);

        MethodTable table = new MethodTable(className, parent, pool);
        for (Constructor<?> ctor : parent.getDeclaredConstructors()) {
            if (Modifier.isPublic(ctor.getModifiers()) || Modifier.isProtected(ctor.getModifiers())) {
                table.addOverride(ctor);
            }
        }
        for (Method method : parent.getDeclaredMethods()) {
            if ((Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers()))
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                table.addOverride(method);
            }
        }

        writeU4(out, 0xCAFEBABE);
        writeU2(out, 0);
        writeU2(out, 52);

        pool.write(out);

        // Access flags
        writeU2(out, ClassModifier.ACC_PUBLIC | ClassModifier.ACC_FINAL | ClassModifier.ACC_SUPER | ClassModifier.ACC_SYNTHETIC);

        // Class name
        writeU2(out, pool.getCompileClass(className));

        // Super class
        writeU2(out, pool.getClass(parent));

        // Interfaces (there are none)
        writeU2(out, 0);

        // Fields (We have one with the wrapper)
        writeU2(out, 1);
        writeU2(out, Modifier.PRIVATE | Modifier.FINAL | 0x1000 /* SYNTHETIC */);
        writeU2(out, pool.getUTF8(HANDLER_FIELD));
        writeU2(out, pool.getUTF8(HANDLER_FIELD_DESC));
        writeU2(out, 0); // Additional field attributes

        table.write(out);

        writeU2(out, 0); // Additional class attributes
    }

    public static void writeU1(DataOutput out, int value) throws IOException {
        out.writeByte(value);
    }

    public static void writeU2(DataOutput out, int value) throws IOException {
        out.writeShort(value);
    }

    public static void writeU4(DataOutput out, int value) throws IOException {
        out.writeInt(value);
    }
}
