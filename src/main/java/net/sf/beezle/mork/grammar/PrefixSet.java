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

package net.sf.beezle.mork.grammar;

import net.sf.beezle.mork.misc.StringArrayList;
import net.sf.beezle.sushi.util.IntArrayList;

import java.util.HashSet;

public class PrefixSet extends HashSet<IntArrayList> {
    public static PrefixSet single(int symbol) {
        PrefixSet result;

        result = new PrefixSet();
        result.add(one(symbol));
        return result;
    }

    public static IntArrayList one(int symbol) {
        IntArrayList result;

        result = new IntArrayList();
        result.add(symbol);
        return result;
    }

    //--

    public PrefixSet concat(PrefixSet right, int k) {
        PrefixSet result;

        if (isEmpty() || right.isEmpty()) {
            return null;
        }
        result = new PrefixSet();
        for (IntArrayList l : this) {
            for (IntArrayList r : right) {
                result.add(concat(l, r, k));
            }
        }
        return result;
    }

    public static IntArrayList concat(IntArrayList left, IntArrayList right, int k) {
        IntArrayList result;

        if (left.size() > k) {
            throw new IllegalArgumentException();
        }
        result = new IntArrayList(left);
        for (int i = 0; i < right.size(); i++) {
            if (result.size() >= k) {
                break;
            }
            result.add(right.get(i));
        }
        return result;
    }

    public void toString(StringArrayList symbolTable, StringBuilder result) {
        boolean first;

        result.append('{');
        first = true;
        for (IntArrayList entry : this) {
            if (first) {
                first = false;
            } else {
                result.append(", ");
            }
            for (int i = 0, max = entry.size(); i < max; i++) {
                result.append(symbolTable.getOrIndex(entry.get(i)));
            }
        }
        result.append('}');
    }
}
