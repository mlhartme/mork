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

public class Prefix implements Comparable<Prefix> {
    private int[] data;

    private int size;

    public Prefix(int ... elements) {
        data = new int[3];
        size = 0;
        for (int element : elements) {
            add(element);
        }
    }

    private Prefix(Prefix orig) {
        data = new int[orig.data.length];
        size = orig.size;
        System.arraycopy(orig.data, 0, data, 0, size);
    }

    public int first() {
        return data[0];
    }

    public void ensureCapacity(int min) {
        int[] tmp;
        int old;
        int capacity;

        old = data.length;
        if (min > old) {
            tmp = data;
            capacity = (old * 5) / 3 + 1;
            if (capacity < min) {
                capacity = min;
            }
            data = new int[capacity];
            System.arraycopy(tmp, 0, data, 0, size);
        }
    }

    private void add(int ele) {
        ensureCapacity(size + 1);
        data[size++] = ele;
    }

    public Prefix concat(Prefix right, int k) {
        Prefix result;

        if (size() > k) {
            throw new IllegalArgumentException();
        }
        result = new Prefix(this);
        for (int i = 0; i < right.size(); i++) {
            if (result.size() >= k) {
                break;
            }
            result.add(right.data[i]);
        }
        return result;
    }

    public int size() {
        return size;
    }

    @Override
    public String toString() {
        StringBuilder buffer;
        int i, max;

        max = size();
        buffer = new StringBuilder();
        for (i = 0; i < max; i++) {
            buffer.append(' ');
            buffer.append(data[i]);
        }
        return buffer.toString();
    }

    public void toString(StringArrayList symbolTable, StringBuilder result) {
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(symbolTable.getOrIndex(data[i]));
        }
    }

    public int[] follows(int first) {
        int[] terminals;

        if (size() > 0) {
            if (data[0] == first) {
                terminals = new int[size() - 1];
                for (int i = 0; i < terminals.length; i++) {
                    terminals[i] = data[(i + 1)];
                }
                return terminals;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return size();
    }

    @Override
    public boolean equals(Object obj) {
        Prefix operand;
        int i;

        if (obj instanceof Prefix) {
            operand = (Prefix) obj;
            if (size == operand.size) {
                for (i = 0; i < size; i++) {
                    if (data[i] != operand.data[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Prefix right) {
        if (size == right.size()) {
            for (int i = 0; i < size; i++) {
                if (data[i] < right.data[i]) {
                    return -1;
                } else if (data[i] > right.data[i]) {
                    return 1;
                }
            }
            return 0;
        } else {
            return size - right.size();
        }
    }
}
