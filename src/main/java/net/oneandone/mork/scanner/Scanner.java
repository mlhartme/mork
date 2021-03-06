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
package net.oneandone.mork.scanner;

import net.oneandone.sushi.util.IntBitSet;

import java.io.IOException;
import java.io.Reader;

/**
 * A token stream, input for parsers.
 */
public class Scanner {
    public static final int EOF = -1;
    public static final int ERROR = -2;

    // this should be a value with a short representation in a Utf8 String
    // because it is saved as part of a String
    public static final int ERROR_PC = 1;

    // the last value with a two-byte representation in utf8
    // (this constant is less important than ERROR_PC because most states
    // are end states; in my Java grammar 40-non-end vs. 260 end states)
    public static final int NO_TERMINAL = 0x07ff;

    /** start state */
    private final int start;

    /** number of modes */
    private final int modeCount;

    /** see ScannerFactory for a description */
    private final char[] table;

    private final Buffer src;

    public Scanner(int start, int modeCount, char[] table, Position pos, Reader reader) {
        this.start = start;
        this.modeCount = modeCount;
        this.table = table;
        this.src = new Buffer();
        src.open(pos, reader);
    }

    /** assigns the position of the last terminal returned by eat. */
    public void getPosition(Position result) {
        src.getPosition(result);
    }

    /** returns the text of the last terminal returned by eat. */
    public String getText() {
        return src.createString();
    }

    /**
     * Scans the next terminal.
     * @return terminal or ERROR or EOF
     */
    public int next(int mode) throws IOException {
        src.eat();
        return scan(mode);
    }


    public int find(int mode, IntBitSet terminals) throws IOException {
        int ofs;
        int terminal;

        ofs = src.getEndOfs();
        do {
            terminal = scan(mode);
        } while (terminal != EOF && !terminals.contains(terminal));
        src.resetEndOfs(ofs);
        return terminal;
    }

    public boolean match(int mode, int eof, int[] terminals) throws IOException {
        int ofs;
        int found;

        ofs = src.getEndOfs();
        try {
            for (int terminal : terminals) {
                found = scan(mode);
                if (found == EOF) {
                    found = eof;
                }
                if (found != terminal) {
                    return false;
                }
            }
            return true;
        } finally {
            src.resetEndOfs(ofs);
        }
    }

    private int scan(int mode) throws IOException {
        int pc;    // idx in table
        int c;
        int terminal;
        int matchedTerminal;
        int matchedEndOfs;
        int endOfs;

        matchedTerminal = ERROR;
        matchedEndOfs = 0;
        endOfs = src.getEndOfs();
        pc = start;
        do {
            terminal = table[pc + mode];
            pc += modeCount;
            if (terminal != NO_TERMINAL) {
                matchedTerminal = terminal;
                matchedEndOfs = endOfs;
            }
            c = src.read();
            if (c == Scanner.EOF) {
                src.resetEndOfs(matchedEndOfs);
                return matchedTerminal == ERROR ? EOF : matchedTerminal;
            }
            endOfs++;
            while (c > table[pc]) {
                pc += 2;
            }
            pc = table[pc + 1];
        } while (pc != ERROR_PC);
        src.resetEndOfs(matchedEndOfs);
        return matchedTerminal;
    }
}
