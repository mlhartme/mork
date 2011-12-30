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

package net.sf.beezle.mork.pda;

import net.sf.beezle.mork.grammar.Grammar;
import net.sf.beezle.sushi.util.IntBitSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LalrPDA extends BasePDA<LalrState> {
    public static LalrPDA create(Grammar grammar) {
        List<LalrShift> allShifts;
        LalrPDA pda;

        allShifts = new ArrayList<LalrShift>();
        pda = new LalrPDA(grammar);
        pda.calcLR0();
        pda.prepare(allShifts);
        pda.calc(allShifts);
        pda.finish();
        return pda;
    }

    public final IntBitSet nullable;

    //--

    public LalrPDA(Grammar grammar) {
        super(grammar);

        this.nullable = new IntBitSet();
        this.grammar.addNullable(nullable);
    }

    //--

    private void calcLR0() {
        LalrState start;
        int i;
        LalrState end;
        List<LalrState> todo;
        
        start = LalrState.create(this, grammar.getStart());
        add(start);
        todo = new ArrayList<LalrState>();
        todo.add(start);
        
        // note: the loop grows its upper bound
        for (i = 0; i < todo.size(); i++) {
            todo.get(i).expand(this, todo);
        }
        // TODO: hack hack hack
        end = start.createShifted(this, grammar.getStart());
        end.createShifted(this, getEofSymbol());
    }

    private void prepare(List<LalrShift> shifts) {
        for (LalrState state : this) {
            state.prepare(this, shifts);
        }
    }

    private void calc(List<LalrShift> shifts) {
        int i, max;
        LalrShift sh;
        List<LalrShift> stack;

        max = shifts.size();
        for (i = 0; i < max; i++) {
            sh = shifts.get(i);
            sh.initReadCalc();
        }
        stack = new ArrayList<LalrShift>();
        for (i = 0; i < max; i++) {
            sh = shifts.get(i);
            sh.digraph(stack);
        }
        for (i = 0; i < max; i++) {
            sh = shifts.get(i);
            sh.saveReadCalc();
            sh.initFollowCalc();
        }

        stack = new ArrayList<LalrShift>();
        for (i = 0; i < max; i++) {
            sh = shifts.get(i);
            sh.digraph(stack);
        }
        for (i = 0; i < max; i++) {
            sh = shifts.get(i);
            sh.saveFollowCalc();
        }
    }

    private void finish() {
        for (LalrState state : this) {
            state.calcLookahead();
        }
    }
}
