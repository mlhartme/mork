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

package net.sf.beezle.mork.scanner;

import java.util.ArrayList;
import java.util.List;

import net.sf.beezle.sushi.util.IntBitSet;

import net.sf.beezle.mork.grammar.IllegalSymbols;
import net.sf.beezle.mork.grammar.Rule;
import net.sf.beezle.mork.regexpr.Action;
import net.sf.beezle.mork.regexpr.Choice;
import net.sf.beezle.mork.regexpr.Loop;
import net.sf.beezle.mork.regexpr.Range;
import net.sf.beezle.mork.regexpr.RegExpr;
import net.sf.beezle.mork.regexpr.Sequence;
import net.sf.beezle.mork.regexpr.Symbol;
import net.sf.beezle.mork.regexpr.Without;

/** stores the result from visiting a node */

public class Expander extends Action {
    private IllegalSymbols exception;

    private Rule[] rules;

    /** symbols actually used for expanding */
    private IntBitSet used;

    private IntBitSet expanding;

    public Expander(Rule[] rules) {
        this.rules = rules;
        this.used = new IntBitSet();
        this.expanding = new IntBitSet();
        this.exception = null;
    }

    public IntBitSet getUsed() {
        return used;
    }

    public RegExpr run(RegExpr re) throws IllegalSymbols {
        RegExpr result;

        result = (RegExpr) re.visit(this);
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    //-----------------------------------------------------------------

    @Override
    public Object symbol(int symbol) {
        List lst;
        int i;
        int max;
        RegExpr re;
        RegExpr[] args;

        if (expanding.contains(symbol)) {
            exception = new IllegalSymbols("illegal recursion in scanner section", symbol);
            return new Symbol(symbol);
        }
        used.add(symbol);

        lst = new ArrayList();
        for (i = 0; i < rules.length; i++) {
            if (rules[i].getLeft() == symbol) {
                lst.add(rules[i].getRight());
            }
        }
        max = lst.size();
        if (max == 0) {
            exception = new IllegalSymbols(
                "illegal reference to parser symbol from scanner section", symbol);
            return new Symbol(symbol);
        } else if (max == 1) {
            re = (RegExpr) lst.get(0);
        } else {
            args = new RegExpr[max];
            lst.toArray(args);
            re = new Choice(args);
        }
        expanding.add(symbol);
        re = (RegExpr) re.visit(this);
        expanding.remove(symbol);
        return re;
    }

    @Override
    public Object range(char first, char last) {
        return new Range(first, last);
    }

    @Override
    public Object choice(Object[] body) {
        RegExpr[] args;

        args = new RegExpr[body.length];
        System.arraycopy(body, 0, args, 0, body.length);
        return new Choice(args);
    }

    @Override
    public Object sequence(Object[] body) {
        RegExpr[] args;

        args = new RegExpr[body.length];
        System.arraycopy(body, 0, args, 0, body.length);
        return new Sequence(args);
    }

    @Override
    public Object loop(Object rawBody) {
        return new Loop((RegExpr) rawBody);
    }

    @Override
    public Object without(Object left, Object right) {
        return new Without((RegExpr) left, (RegExpr) right);
    }
}