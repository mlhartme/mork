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
package net.oneandone.mork.regexpr;

import java.util.ArrayList;
import java.util.List;

/**
 * Character ranges. Immutable objects, they can be shared.
 */

public class Range extends RegExpr {
    private char first;
    private char last;

    public static final Range ALL = new Range((char) 0, Character.MAX_VALUE);

    //--

    public Range(char firstAndLast) {
        this(firstAndLast, firstAndLast);
    }

    // TODO: throw a checked exception to get positional error message
    public Range(char first, char last) {
        if (first > last) {
            throw new IllegalArgumentException();
        }

        this.first = first;
        this.last = last;
    }

    //--

    public char getFirst() {
        return first;
    }

    public char getLast() {
        return last;
    }

    public boolean contains(char c) {
        return (first <= c) && (c <= last);
    }

    public boolean contains(Range operand) {
        return contains(operand.first) && contains(operand.last);
    }


    @Override
    public boolean equals(Object obj) {
        Range range;

        if (obj instanceof Range) {
            range = (Range) obj;
            return (first == range.first) && (last == range.last);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return first;
    }

    @Override
    public Object visit(Action action) throws ActionException {
        return action.range(first, last);
    }

    //--

    /**
     * @return intersection between both ranges
     */
    public Range and(Range operand) {
        int fst, lst;

        fst = Math.max(first, operand.first);
        lst = Math.min(last, operand.last);
        if (fst <= lst) {
            return new Range((char) fst, (char) lst);
        } else {
            return null;
        }
    }

    /**
     * @return true if intersection would return != null. But faster!!
     */
    public boolean touches(Range operand) {
        int fst, lst;

        fst = Math.max(first, operand.first);
        lst = Math.min(last, operand.last);
        return fst <= lst;
    }

    /**
     * @param ranges in-out argument
     *
     * TODO:
     * o  expensive, the list is modified ...
     * *  states that touch each other are not merged
     */
    public static void normalizeRanges(List<Range> ranges) {
        int i, todo, max;
        Range current, op, and;

        todo = 0;
        while (todo < ranges.size()) {
            // take the first range, and-it with all others and
            // append fractions to the end.
            current = (Range) ranges.get(todo);
            max = ranges.size(); // don't grow max inside the for-loop
            for (i = todo + 1; i < max; i++) {
                op = (Range) ranges.get(i);
                and = current.and(op);
                if (and != null) {
                    current.remove(and, ranges);
                    op.remove(and, ranges);

                    ranges.remove(i);
                    i--;
                    max--;

                    current = and;
                }
            }
            ranges.set(todo, current);
            todo++;
        }
    }

    public static void remove(List here, Range operand) {
        List<Range> result;
        int i, max;
        Range tmp;

        result = new ArrayList<Range>();
        max = here.size();
        for (i = 0; i < max; i++) {
            tmp = (Range) here.get(i);
            if (tmp.and(operand) != null) {
                tmp.remove(operand, result);
            } else {
                result.add(tmp);
            }
        }
        here.clear();
        here.addAll(result);
    }

    /**
     * only valid if this.and(operand) is not empty!
     */
    public void remove(Range operand, List<Range> result) {
        // a piece left of operand
        // |--this--|
        //     |--op--|
        // |--|
        if (first < operand.first) {
            result.add(new Range(first, (char) (operand.first - 1)));
        }

        // a piece right of operand
        //   |--this--|
        // |--op--|
        //         |--|
        if (operand.last < last) {
            result.add(new Range((char) (operand.last + 1), last));
        }

        // result is not changes this is completly covered by operand
    }


    //--

    @Override
    public String toString() {
        if (first == last) {
            return "[" + charString(first) + "]";
        } else {
            return "[" + charString(first) + "-" + charString(last) + "]";
        }
    }
    private static String charString(char c) {
        if (c >= ' ') {
            return "'" + c + "' (" + ((int) c) + ")";
        } else {
            return "(" + ((int) c) + ")";
        }
    }
}
