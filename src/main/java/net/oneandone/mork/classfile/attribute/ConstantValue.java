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
package net.oneandone.mork.classfile.attribute;

import net.oneandone.mork.classfile.Input;
import net.oneandone.mork.classfile.Output;

import java.io.IOException;

public class ConstantValue extends Attribute {
    public static final String NAME = "ConstantValue";

    public final Object value;

    public ConstantValue(Object valueInit) {
        super(NAME);

        value = valueInit;
    }

    public ConstantValue(Input src) throws IOException {
        super(NAME);

        int len;

        len = src.readU4();
        if (len != 2) {
            throw new RuntimeException("ConstantValue attribute of length "
                                       + len);
        }
        value = src.readConstant();
    }

    @Override
    public void write(Output dest) throws IOException {
        dest.writeUtf8(name);
        dest.writeU4(2);
        dest.writeConstant(value);
    }

    @Override
    public String toString() {
        return name + " attribute " + value;
    }
}
