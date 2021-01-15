package io.github.noeppi_noeppi.libs.invocationwrappers.classwriter;

import io.github.noeppi_noeppi.libs.invocationwrappers.constant.ConstantPoolType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static io.github.noeppi_noeppi.libs.invocationwrappers.classwriter.ClassFileWriter.*;

public class ConstantPool {

    private int size = 1; // 1 is empty, java is weird
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(this.bytes);

    private final Map<Class<?>, Integer> classMap = new HashMap<>();
    private final Map<String, Integer> compileClassMap = new HashMap<>();
    private final Map<String, Integer> fieldMap = new HashMap<>();
    private final Map<Constructor<?>, Integer> ctorMap = new HashMap<>();
    private final Map<Method, Integer> methodMap = new HashMap<>();
    private final Map<String, Integer> utf8Map = new HashMap<>();
    private final Map<String, Integer> stringMap = new HashMap<>();
    private final Map<Integer, Integer> intMap = new HashMap<>();

    public int addCompileClass(String fqn) throws IOException {
        if (this.compileClassMap.containsKey(fqn)) {
            return this.compileClassMap.get(fqn);
        } else {
            int ref = this.addUTF8(fqn.replace('.', '/'));
            writeU1(this.out, ConstantPoolType.CP_CLASS);
            writeU2(this.out, ref);
            this.compileClassMap.put(fqn, this.size);
            return this.size++;
        }
    }

    public int addClass(Class<?> clazz) throws IOException {
        if (this.classMap.containsKey(clazz)) {
            return this.classMap.get(clazz);
        } else {
            int ref = this.addUTF8(clazz.getName().replace('.', '/'));
            writeU1(this.out, ConstantPoolType.CP_CLASS);
            writeU2(this.out, ref);
            this.classMap.put(clazz, this.size);
            return this.size++;
        }
    }

    public int addField(String fqn, String name, String desc) throws IOException {
        return this.addField(this.addCompileClass(fqn), name, desc);
    }

    public int addField(Class<?> clazz, String name, String desc) throws IOException {
        return this.addField(this.addClass(clazz), name, desc);
    }

    private int addField(int classRef, String name, String desc) throws IOException {
        if (this.fieldMap.containsKey(classRef + "@" + name + "(" + desc + ")")) {
            return this.fieldMap.get(classRef + "@" + name + "(" + desc + ")");
        } else {
            int typeRef = this.addNameAndType(name, desc);
            writeU1(this.out, ConstantPoolType.CP_FIELDREF);
            writeU2(this.out, classRef);
            writeU2(this.out, typeRef);
            this.fieldMap.put(classRef + "@" + name + "(" + desc + ")", this.size);
            return this.size++;
        }
    }

    public int addMethod(Constructor<?> ctor) throws IOException {
        if (this.ctorMap.containsKey(ctor)) {
            return this.ctorMap.get(ctor);
        } else {
            int classRef = this.addClass(ctor.getDeclaringClass());
            int typeRef = this.addNameAndType("<init>", MethodTable.getDescriptor(ctor));
            if (ctor.getDeclaringClass().isInterface()) {
                writeU1(this.out, ConstantPoolType.CP_IFACE_METHODREF);
            } else {
                writeU1(this.out, ConstantPoolType.CP_METHODREF);
            }
            writeU2(this.out, classRef);
            writeU2(this.out, typeRef);
            this.ctorMap.put(ctor, this.size);
            return this.size++;
        }
    }

    public int addMethod(Method method) throws IOException {
        if (this.methodMap.containsKey(method)) {
            return this.methodMap.get(method);
        } else {
            int classRef = this.addClass(method.getDeclaringClass());
            int typeRef = this.addNameAndType(method.getName(), MethodTable.getDescriptor(method));
            if (method.getDeclaringClass().isInterface()) {
                writeU1(this.out, ConstantPoolType.CP_IFACE_METHODREF);
            } else {
                writeU1(this.out, ConstantPoolType.CP_METHODREF);
            }
            writeU2(this.out, classRef);
            writeU2(this.out, typeRef);
            this.methodMap.put(method, this.size);
            return this.size++;
        }
    }

    private int addNameAndType(String name, String descriptor) throws IOException {
        int nameRef = this.addUTF8(name);
        int descRef = this.addUTF8(descriptor);
        writeU1(this.out, ConstantPoolType.CP_NAMED_TYPE);
        writeU2(this.out, nameRef);
        writeU2(this.out, descRef);
        return this.size++;
    }

    public int addJavaString(String value) throws IOException {
        if (this.stringMap.containsKey(value)) {
            return this.stringMap.get(value);
        } else {
            int ref = this.addUTF8(value);
            writeU1(this.out, ConstantPoolType.CP_STRING);
            writeU2(this.out, ref);
            this.stringMap.put(value, this.size);
            return this.size++;
        }
    }

    public int addJavaInt(int value) throws IOException {
        if (this.intMap.containsKey(value)) {
            return this.intMap.get(value);
        } else {
            writeU1(this.out, ConstantPoolType.CP_INTEGER);
            writeU4(this.out, value);
            this.intMap.put(value, this.size);
            return this.size++;
        }
    }

    public int addUTF8(String value) throws IOException {
        if (this.utf8Map.containsKey(value)) {
            return this.utf8Map.get(value);
        } else {
            writeU1(this.out, ConstantPoolType.CP_UTF8);
            this.out.writeUTF(value);
            this.utf8Map.put(value, this.size);
            return this.size++;
        }
    }

    public int getJavaString(String value) {
        if (this.stringMap.containsKey(value)) {
            return this.stringMap.get(value);
        } else {
            throw new IllegalArgumentException("String value not present in the constant pool: " + value);
        }
    }

    public int getJavaInt(int value) {
        if (this.intMap.containsKey(value)) {
            return this.intMap.get(value);
        } else {
            throw new IllegalArgumentException("Integer value not present in the constant pool: " + value);
        }
    }

    public int getUTF8(String value) {
        if (this.utf8Map.containsKey(value)) {
            return this.utf8Map.get(value);
        } else {
            throw new IllegalArgumentException("UTF8 value not present in the constant pool: " + value);
        }
    }

    public int getClass(Class<?> value) {
        if (this.classMap.containsKey(value)) {
            return this.classMap.get(value);
        } else if (this.compileClassMap.containsKey(value.getName())) {
            return this.compileClassMap.get(value.getName());
        } else {
            throw new IllegalArgumentException("Class value not present in the constant pool: " + value.getName());
        }
    }

    public int getCompileClass(String value) {
        if (this.compileClassMap.containsKey(value)) {
            return this.compileClassMap.get(value);
        } else {
            for (Map.Entry<Class<?>, Integer> entry : this.classMap.entrySet()) {
                if (value.equals(entry.getKey().getName())) {
                    return entry.getValue();
                }
            }
            throw new IllegalArgumentException("Compile Class values not present in the constant pool: " + value);
        }
    }

    public int getMethod(Method value) {
        if (this.methodMap.containsKey(value)) {
            return this.methodMap.get(value);
        } else {
            throw new IllegalArgumentException("Method value not present in the constant pool: " + value.getName() + " " + MethodTable.getDescriptor(value) + " " + value.getDeclaringClass().getName());
        }
    }

    public int getMethod(Constructor<?> value) {
        if (this.ctorMap.containsKey(value)) {
            return this.ctorMap.get(value);
        } else {
            throw new IllegalArgumentException("Constructor value not present in the constant pool: " + value.getName() + " " + MethodTable.getDescriptor(value) + " " + value.getDeclaringClass().getName());
        }
    }

    public void write(DataOutput output) throws IOException {
        this.out.flush();
        this.out.close();
        this.bytes.close();

        writeU2(output, this.size);
        output.write(this.bytes.toByteArray());
    }
}
