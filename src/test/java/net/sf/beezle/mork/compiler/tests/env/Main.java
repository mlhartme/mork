/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.beezle.mork.compiler.tests.env;

import net.sf.beezle.mork.mapping.Mapper;
import java.io.StringReader;

/**
 * Test env arguments.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Mapper mapper;
        Object[] result;

        mapper = new Mapper("net.sf.beezle.mork.compiler.tests.env.Mapper");
        mapper.setEnvironment(new Integer(3));
        result = mapper.run("<const>", new StringReader("ab"));
        System.out.println("result: " + result[0]);
    }

    public static int add(Object left, Object right) {
        return ((Integer) left).intValue() + ((Integer) right).intValue();
    }
}