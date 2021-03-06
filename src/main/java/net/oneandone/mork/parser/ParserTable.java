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
package net.oneandone.mork.parser;

import net.oneandone.mork.compiler.ConflictHandler;
import net.oneandone.mork.grammar.Grammar;
import net.oneandone.mork.misc.GenericException;
import net.oneandone.mork.pda.State;
import net.oneandone.sushi.util.IntBitSet;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Shift-Reduce-Table realized by an array.
 */
public class ParserTable implements Serializable {
    private static final int ACTION_BITS = 2;

    /** largest operand value possible */
    private static final int MAX_OPERAND = 0xbfff; // TODO: 0x3fff?

    /** Bit mask to obtain kind of action. */
    private static final int MASK  = 0x0003;


    //-- program for the parser'

    /** initial state for PDA */
    private final char startState;

    private final int symbolCount;

    private final int eofSymbol;

    /**
     * Values in the table. [state * symbolCount + symbol]. Each value
     * stores   operand &lt;&lt; ACTION_BITS | action.
     */
    private final char[] values;

    /** length of productions; [production] */
    private final int[] lengths;

    /** left-hand-side of productions. */
    private final int[] lefts;

    /** index by states. TODO: final  */
    private char[] modes;

    //-- object compilation

    /**
     * Constructor for compiled object.
     */
    public ParserTable(char startState, int symbolCount, int eofSymbol, char[] values, int[] lengths, int[] lefts, char[] modes) {
        this.startState = startState;
        this.symbolCount = symbolCount;
        this.eofSymbol = eofSymbol;
        this.values = values;
        this.lengths = lengths;
        this.lefts = lefts;
        this.modes = modes;
    }

    public ParserTable(
            char startState, int symbolCount, int eofSymbol, int stateCount,
            String[] packedValues, int[] lengths, int[] lefts, char[] modes) {
        this(startState, symbolCount, eofSymbol, new char[stateCount * symbolCount], lengths, lefts, modes);
        unpackValues(packedValues);
    }

    public ParserTable(int startState, int stateCount, int symbolCount, int eofSymbol, Grammar grm, char[] modes) throws GenericException {
        int i;
        int max;

        if (stateCount >= MAX_OPERAND) {
            throw new GenericException("too may states");
        }
        this.startState = (char) startState;
        this.symbolCount = symbolCount;
        this.eofSymbol = eofSymbol;
        this.modes = modes;

        values = new char[stateCount * symbolCount];
        for (i = 0; i < values.length; i++) {
            values[i] = createValue(Parser.SPECIAL, Parser.SPECIAL_ERROR);
        }

        max = grm.getSymbolCount();
        if (max >= MAX_OPERAND) {
            throw new GenericException("too may symbols");
        }

        max = grm.getProductionCount();
        lengths = new int[max];
        lefts = new int[max];
        for (i = 0; i < max; i++) {
            lengths[i] = grm.getLength(i);
            lefts[i] = grm.getLeft(i);
        }
    }

    public int getEofSymbol() {
        return eofSymbol;
    }

    public int getValueCount() {
        return values.length;
    }

    public void setModes(char[] modes) {
        this.modes = modes;
    }

    //-- building the table

    /** Cannot have conflicts. @param  sym  may be a nonterminal */
    public void addShift(int state, int sym, int nextState) {
        int idx;

        idx = state * symbolCount + sym;
        if (values[idx] != NOT_SET) {
            throw new IllegalStateException();
        }
        values[idx] = createValue(Parser.SHIFT, nextState);
    }

    public void addReduce(int stateId, State state, int terminal, int prod, ConflictHandler handler) {
        int idx;
        char old;
        char value;

        idx = stateId * symbolCount + terminal;
        old = values[idx];
        value = createValue(Parser.REDUCE, prod);
        if (old == NOT_SET) {
            values[idx] = value;
        } else if (value == old) {
            // nothing to do
        } else {
            values[idx] = handler.resolve(stateId, state, terminal, old, value);
        }
    }

    public void addAccept(int state, int eof) {
        // value is assigned untested, overwrites shift on EOF
        values[state * symbolCount + eof] = createValue(Parser.SPECIAL, Parser.SPECIAL_ACCEPT);
    }

    public void addWhitespace(IntBitSet whites) {
        int sym;
        int state;
        int stateCount;
        int idx;

        stateCount = getStateCount();
        for (sym = whites.first(); sym != -1; sym = whites.next(sym)) {
            for (state = 0; state < stateCount; state++) {
                idx = state * symbolCount + sym;
                if (values[idx] != NOT_SET) {
                    throw new IllegalStateException();
                }
                values[idx] = createValue(Parser.SKIP);
            }
        }
    }

    //--

    public static final int NOT_SET = createValue(Parser.SPECIAL, Parser.SPECIAL_ERROR);

    private char createValue(int action) {
        return createValue(action, 0);
    }

    public static char createValue(int action, int operand) {
        return (char) (action | operand << ACTION_BITS);
    }

    //-- create a representation to store the table effiziently

    /** has to be a unique value, i.e. something not produced by createValue. */
    private static final int COUNT_MARK = Parser.SKIP + 4;

    public String[] packValues() {
        StringBuilder difs;
        StringBuilder vals;
        int i;
        int prev;
        int count;
        int v;

        difs = new StringBuilder();
        vals = new StringBuilder();
        prev = 0;
        for (i = 0; i < values.length; i++) {
            v = values[i];
            if (v != createValue(Parser.SPECIAL, Parser.SPECIAL_ERROR)) {
                count = sameValues(i);
                difs.append((char) (i - prev));
                if (count <= 2) {
                    vals.append(values[i]);
                } else {
                    vals.append((char) COUNT_MARK);
                    difs.append((char) count);
                    vals.append(values[i]);
                    i += count - 1;  // 1 is added by loop-increment
                }
                prev = i;
            }
        }
        return packValue(difs, vals);
    }

    private static final int MAX_UTF8_LENGTH = 0xffff / 3;  // 3 max size of utf8 encoded char

    public String[] packValue(StringBuilder difs, StringBuilder vals) {
        List<String> lst;
        String[] array;

        if (difs.length() != vals.length()) {
            throw new IllegalArgumentException();
        }
        lst = new ArrayList<String>();
        split(difs, MAX_UTF8_LENGTH, lst);
        split(vals, MAX_UTF8_LENGTH, lst);
        array = new String[lst.size()];
        lst.toArray(array);
        return array;
    }

    private static void split(StringBuilder str, int chunkLength, List<String> result) {
        int i;
        int max;

        max = str.length();
        for (i = 0; i < max; i += chunkLength) {
            result.add(str.substring(i, Math.min(max, i + chunkLength)));
        }
    }


    private void unpackValues(String[] packed) {
        int chunk;
        int chunkCount;
        String difs;
        String vals;
        int idx;
        int i;
        int max;
        char ch;
        int end;
        boolean marked;

        idx = 0;
        marked = false;
        chunkCount = packed.length / 2;
        for (chunk = 0; chunk < chunkCount; chunk++) {
            difs = packed[chunk];
            vals = packed[chunk + chunkCount];
            max = difs.length();
            for (i = 0; i < max; i++) {
                ch = vals.charAt(i);
                if (marked) {
                    for (end = idx + difs.charAt(i); idx < end; idx++) {
                        values[idx] = ch;
                    }
                    idx--;
                    // idx point to the last value assigned
                    marked = false;
                } else if (ch == COUNT_MARK) {
                    idx += difs.charAt(i);
                    marked = true;
                } else {
                    idx += difs.charAt(i);
                    values[idx] = ch;
                }
            }
        }
    }

    private int sameValues(int ofs) {
        char cmp;
        int i;

        cmp = values[ofs];
        for (i = ofs + 1; i < values.length; i++) {
            if (values[i] != cmp) {
                return i - ofs;
            }
        }
        return values.length - ofs;
    }

    //--

    public int getSymbolCount() {
        return symbolCount;
    }

    public int getStateCount() {
        return values.length / symbolCount;
    }

    public int getStartState() {
        return startState;
    }

    public int getProductionCount() {
        return lefts.length; // same as "return lengths.length;"
    }

    public int getLeft(int prod) {
        return lefts[prod];
    }

    public int getLength(int prod) {
        return lengths[prod];
    }

    //-- using the table

    public static int getAction(int value) {
        return value & MASK;
    }

    public static int getOperand(int value) {
        return value >>> ACTION_BITS;
    }

    public int lookup(int state, int symbol) {
        return values[state * symbolCount + symbol];
    }

    public int lookupShift(int state, int production) {
        return values[state * symbolCount + lefts[production]] >>> ACTION_BITS;
    }

    public char getMode(int state) {
        return modes[state];
    }

    public void print(PrintWriter dest) {
        int i;

        for (i = 0; i < values.length; i++) {
            if (i % 30 == 0) {
                dest.println();
            }
            dest.print(" " + (int) values[i]);
        }
    }

    public IntBitSet getShifts(int state) {
        int i;
        int value;
        IntBitSet result;
        int symbolCount;
        int action;

        symbolCount = getSymbolCount();
        result = new IntBitSet();
        for (i = 0; i < symbolCount; i++) {
            value = lookup(state, i);
            action = getAction(value);
            if (action == Parser.SHIFT || action == Parser.REDUCE) {
                result.add(i);
            }
        }
        return result;
    }


    public int[] getLengths() {
        int[] result;
        int i;

        result = new int[getProductionCount()];
        for (i = 0; i < result.length; i++) {
            result[i] = getLength(i);
        }
        return result;
    }

    public int[] getLefts() {
        int[] result;
        int i;

        result = new int[getProductionCount()];
        for (i = 0; i < result.length; i++) {
            result[i] = getLeft(i);
        }
        return result;
    }

    //--

    public String toString(Grammar grammar) {
        int symbol;
        int state;
        int stateCount;
        int symbolCount;
        StringBuilder result;
        int value;

        stateCount = getStateCount();
        symbolCount = getSymbolCount();
        result = new StringBuilder();
        result.append('\t');
        for (symbol = 0; symbol < symbolCount; symbol++) {
            result.append(grammar.getSymbolTable().getOrIndex(symbol));
            result.append('\t');
        }
        result.append("\n\n");

        for (state = 0; state < stateCount; state++) {
            result.append(state);
            result.append('\t');
            for (symbol = 0; symbol < symbolCount; symbol++) {
                value = lookup(state, symbol);
                result.append(actionToString(value, grammar)).append('\t');
            }
            result.append('\n');
        }
        return result.toString();
    }

    public static String actionToString(int value, Grammar grammar) {
        int prod;

        switch (getAction(value)) {
            case Parser.SHIFT:
                return "S " + getOperand(value);
            case Parser.REDUCE:
                return "R " + grammar.prodToString(getOperand(value));
            case Parser.SPECIAL:
                if (getOperand(value) == Parser.SPECIAL_ACCEPT) {
                    return "A";
                } else {
                    return " ";
                }
            default:
                throw new RuntimeException("unknown action: " + getAction(value));
        }

    }
}
