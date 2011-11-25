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

package net.sf.beezle.mork.bootstrap;

import net.sf.beezle.mork.compiler.Syntax;
import net.sf.beezle.mork.misc.GenericException;
import net.sf.beezle.mork.semantics.BuiltIn;
import net.sf.beezle.mork.semantics.IllegalLiteral;

/**
 * Helper functions referred by bootstrap mappers.
 */
public class Stubs04 {
    public static Syntax loadGrammar(String fileName) throws GenericException, IllegalLiteral {
        return Loader.loadGrammar(BuiltIn.parseString(fileName));
    }

    public static Syntax loadDtd(String fileName) throws GenericException {
        throw new GenericException("DTD syntax not supported in bootstrap version");
    }
}