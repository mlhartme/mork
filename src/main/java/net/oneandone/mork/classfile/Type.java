/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.mork.classfile;

/**
 * Stupid data container for ClassRef.
 */

public class Type implements Bytecodes, Constants {
    public final int id; // T_ constant
    public final String name;
    public final char descriptor;
    public final Class<?> type;
    public final Class<?> wrapper;
    public final Object zero;
    public final Object zeroLdc;
    public final int size;
    public final int load;
    public final int store;
    public final int arrayLoad;
    public final int arrayStore;

    private Type(int id, char descriptor, Class<?> wrapper,
                 Object zero, Object zeroLdc, int size,
                 int load, int store, int arrayLoad, int arrayStore) {
        this.id = id;
        this.type = getPrimitive(wrapper);
        this.name = (type == null ? null : type.getName());
        this.descriptor = descriptor;
        this.wrapper = wrapper;
        this.zero = zero;
        this.zeroLdc = zeroLdc;
        this.size = size;
        this.load = load;
        this.store = store;
        this.arrayLoad = arrayLoad;
        this.arrayStore = arrayStore;
    }

    private static Class<?> getPrimitive(Class<?> wrapper) {
        if (wrapper == null) {
            return null;
        }
        try {
            return (Class<?>) wrapper.getDeclaredField("TYPE").get(null);
        } catch (IllegalArgumentException | SecurityException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static final Integer ZERO = new Integer(0);

    public static final Type[] PRIMITIVES = {
        // primitive types
            new Type(T_VOID, 'V', Void.class, null, null, 0, -1, -1, -1, -1),

            new Type(T_BOOLEAN, 'Z', Boolean.class, new Boolean(false), ZERO,
                    1, ILOAD, ISTORE, BALOAD, BASTORE),
            // bytecode has no instruction to access boolean arrays,
            // they are accessed as byte arrays

            new Type(T_CHAR, 'C', Character.class, new Character((char) 0),
                    ZERO, 1, ILOAD, ISTORE, CALOAD, CASTORE),

            new Type(T_FLOAT, 'F', Float.class, new Float(0), new Float(0), 1,
                    FLOAD, FSTORE, FALOAD, FASTORE),

            new Type(T_DOUBLE, 'D', Double.class, new Double(0), new Double(0),
                    2, DLOAD, DSTORE, DALOAD, DASTORE),

            new Type(T_BYTE, 'B', Byte.class, new Byte((byte) 0), ZERO, 1,
                    ILOAD, ISTORE, BALOAD, BASTORE),

            new Type(T_SHORT, 'S', Short.class, new Short((short) 0), ZERO, 1,
                    ILOAD, ISTORE, SALOAD, SASTORE),

            new Type(T_INT, 'I', Integer.class, ZERO, ZERO, 1, ILOAD, ISTORE,
                    IALOAD, IASTORE),

            new Type(T_LONG, 'J', Long.class, new Long(0), new Long(0), 2,
                    LLOAD, LSTORE, LALOAD, LASTORE),
    };

    public static final Type REFERENCE =
        new Type(T_REFERENCE, 'L', null, null, null, 1, ALOAD, ASTORE, AALOAD, AASTORE);
}
