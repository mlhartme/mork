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

package net.sf.beezle.mork.lalrparser;

import net.sf.beezle.mork.grammar.Grammar;
import net.sf.beezle.sushi.util.IntBitSet;

import java.util.HashSet;
import java.util.Set;

public class LalrReduce {
    public final int production;
    public final IntBitSet lookahead;

    public final Set<LalrShift> lookback;

    public LalrReduce(int production) {
        this.production = production;
        this.lookahead = new IntBitSet();

        this.lookback = new HashSet<LalrShift>();
    }

    public void calcLookahead() {
        for (LalrShift sh : lookback) {
            sh.addFollow(lookahead);
        }
    }

    //--

    public String toString(Grammar grammar) {
        StringBuilder buffer;

        buffer = new StringBuilder();
        buffer.append("reduce ");
        grammar.prodToString(buffer, production);
        buffer.append(" on ");
        buffer.append(lookahead.toString(grammar.getSymbolTable().toList()));
        buffer.append('\n');

        return buffer.toString();
    }

    @Override
    public int hashCode() {
        return production;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}