package io.github.noeppi_noeppi.libs.invocationwrappers.classwriter;

import io.github.noeppi_noeppi.libs.invocationwrappers.InvocationWrapper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.noeppi_noeppi.libs.invocationwrappers.classwriter.ClassFileWriter.writeU2;

public class MethodTable {

    private int size = 0;
    private final String className;
    private final Class<?> superClass;
    private final ConstantPool pool;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(this.bytes);

    public MethodTable(String className, Class<?> superClass, ConstantPool pool) {
        this.className = className;
        this.superClass = superClass;
        this.pool = pool;
    }

    public void addOverride(Constructor<?> ctor) throws IOException, ReflectiveOperationException {
        int nameRef = this.pool.addUTF8("<init>");
        int signatureRef = this.pool.addUTF8(getDescriptorWithTypePrefix(ctor, InvocationWrapper.class));

        writeU2(this.out, Modifier.PUBLIC);
        writeU2(this.out, nameRef);
        writeU2(this.out, signatureRef);
        writeU2(this.out, 1); // Additional attributes count (write the code)
        CodeUtil.writeConstructorCode(this.out, this.pool, this.className, this.superClass, ctor);
        this.size += 1;
    }

    public void addOverride(Method method) throws IOException, ReflectiveOperationException {
        int nameRef = this.pool.addUTF8(method.getName());
        int signatureRef = this.pool.addUTF8(getDescriptor(method));

        int modifiers = method.getModifiers();
        modifiers &= ~Modifier.ABSTRACT;

        writeU2(this.out, modifiers);
        writeU2(this.out, nameRef);
        writeU2(this.out, signatureRef);
        writeU2(this.out, 1); // Additional attributes count (write the code)
        CodeUtil.writeMethodCode(this.out, this.pool, this.className, this.superClass, method);
        this.size += 1;
    }

    public void write(DataOutput output) throws IOException {
        this.out.flush();
        this.out.close();
        this.bytes.close();

        writeU2(output, this.size);
        output.write(this.bytes.toByteArray());
    }

    public static String getDescriptor(Constructor<?> ctor) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> c : ctor.getParameterTypes()) {
            String sig = Array.newInstance(c, 0).toString().replace('.', '/');
            sb.append(sig, 1, sig.indexOf('@'));
        }
        sb.append(')');

        sb.append("V");

        return sb.toString();
    }

    public static String getDescriptorWithTypePrefix(Constructor<?> ctor, Class<?> firstArg) {
        StringBuilder sb = new StringBuilder("(");
        List<Class<?>> args = new ArrayList<>();
        args.add(firstArg);
        args.addAll(Arrays.asList(ctor.getParameterTypes()));
        for (Class<?> c : args) {
            String sig = Array.newInstance(c, 0).toString().replace('.', '/');
            sb.append(sig, 1, sig.indexOf('@'));
        }
        sb.append(')');

        sb.append("V");

        return sb.toString();
    }

    public static String getDescriptor(Method method) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> c : method.getParameterTypes()) {
            String sig = Array.newInstance(c, 0).toString().replace('.', '/');
            sb.append(sig, 1, sig.indexOf('@'));
        }
        sb.append(')');

        if (method.getReturnType() == void.class) {
            sb.append("V");
        } else {
            String sig = Array.newInstance(method.getReturnType(), 0).toString().replace('.', '/');
            sb.append(sig, 1, sig.indexOf('@'));
        }

        return sb.toString();
    }
}
