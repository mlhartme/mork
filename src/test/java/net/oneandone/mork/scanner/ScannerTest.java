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

import junit.framework.TestCase;
import net.oneandone.mork.grammar.Rule;
import net.oneandone.mork.misc.GenericException;
import net.oneandone.mork.misc.StringArrayList;
import net.oneandone.mork.regexpr.Choice;
import net.oneandone.mork.regexpr.Loop;
import net.oneandone.mork.regexpr.Range;
import net.oneandone.mork.regexpr.RegExpr;
import net.oneandone.mork.regexpr.Sequence;
import net.oneandone.mork.regexpr.Without;
import net.oneandone.sushi.util.IntBitSet;

import java.io.IOException;
import java.io.StringReader;

/**
 * TODO: more tests.
 * TODO: factor out the stuff common with XmlScannerTest.
 */

public class ScannerTest extends TestCase {
    private ScannerFactory factory;
    private Scanner scanner;

    public void testSimple() throws GenericException, IOException {
        table(
            keyword("Hello"),
            keyword("World")
        );

        input("HelloWorld");

        scan("Hello", 0, "Hello");
        scan("World", 1, "World");
        scan("EOF",   EOF, null);
    }

    public void testSimpleWithout() throws GenericException, IOException {
        table(
                new Without(new Range((char) 0, (char) 65535), new Range('a')),
                new Without(new Range((char) 0, (char) 65535), new Range('b'))
        );

        input("bbb");

        scan("not a", 0, "b");
        scan("not a", 0, "b");
        scan("not a", 0, "b");
        scan("EOF", EOF, null);

        input("aacd");

        scan("not b", 1, "a");
        scan("not b", 1, "a");
        scan("not a", 0, "c");
        scan("not a", 0, "d");
        scan("EOF", EOF, null);
    }

    public void testWithoutXY() throws GenericException, IOException {
        table(
                new Sequence(keyword("K"), new Without(Loop.createStar(any()), keyword("xy")))
        );

        input("K/**");
        scan("comment", 0, "K/**");
    }

    public void testWithout() throws GenericException, IOException {
        table(
            new Sequence(new RegExpr[] {
                keyword("/*"),
                new Without(Loop.createStar(any()),
                            new Sequence(new RegExpr[] {
                                Loop.createStar(any()), keyword("*/"), Loop.createStar(any())
                            })),
                keyword("*/")
            })
        );

        input("/**//* *//***//* /*//* **/");

        scan("comment", 0, "/**/");
        scan("comment", 0, "/* */");
        scan("comment", 0, "/***/");
        scan("comment", 0, "/* /*/");
        scan("comment", 0, "/* **/");  // this is the token that fails for the "simple" definition
                                       // with the body ('*'! | '*' '/'!)*
        scan("EOF", EOF, null);
    }

    public void testXmlWithout() throws GenericException, IOException {
        table(
            new Sequence(new RegExpr[] {
                new Without(new Sequence(any(), Loop.createStar(any())),
                            new Sequence(new RegExpr[] {
                                Loop.createStar(any()), keyword("]]>"), Loop.createStar(any())
                            }))
            })
        );

        input("abc");
        scan("chardata", 0, "abc");
        scan("EOF", EOF, null);

        input("]]>");
        scan("head", 0, "]]");  // to not recognize as a single token
        scan("tail", 0, ">");
        scan("EOF", EOF, null);

        input("xy]]>z");
        scan("head", 0, "xy]]");
        scan("tail", 0, ">z");
        scan("EOF", EOF, null);
    }

    private static RegExpr any() {
        return new Range((char) 0, (char) 65535);
    }

    private static final int EOF = Scanner.EOF;

    private void scan(String what, int terminal, String text) throws IOException {
        int currentTerminal;

        currentTerminal = scanner.next(0);
        assertEquals("terminal " + what, terminal, currentTerminal);
        if (text != null) {
            assertEquals("text " + what, text, scanner.getText());
        }
    }

    private void table(RegExpr ... token) throws GenericException {
        IntBitSet terminals;
        int i;
        Rule[] rules;
        FABuilder builder;

        terminals = new IntBitSet();
        terminals.addRange(0, token.length - 1);
        rules = new Rule[token.length];
        for (i = 0; i < rules.length; i++) {
            rules[i] = new Rule(i, token[i]);
        }
        builder = FABuilder.run(rules, terminals, new StringArrayList(), null);
        Modes.resolveScannerConflicts(builder.getFA(), rules);
        factory = ScannerFactory.createSimple(builder.getFA(), builder.getErrorState(), terminals);
    }

    private void input(String input) {
        scanner = factory.newInstance(new Position(), new StringReader(input));
    }

    private static void resolveConflicts(FA fa, RegExpr[] token) {
        int[] prios;
        int i;

        prios = new int[token.length];
        for (i = 0; i < prios.length; i++) {
            prios[i] = i;
        }
        Label.resolveConflicts(fa, prios);
    }

    private static RegExpr keyword(String str) {
        RegExpr result;
        int i;

        result = new Sequence();
        for (i = 0; i < str.length(); i++) {
            result = new Sequence(result, new Range(str.charAt(i)));
        }
        return result;
    }

    private static RegExpr createIdentifier() {
        RegExpr first, follow;

        first = new Choice(new Range('A', 'Z'), new Range('a', 'z'));
        follow = new Choice(new Choice(new Range('A', 'Z'), new Range('a', 'z')),
                            new Range('0', '9'));
        return new Sequence(first, new Loop(follow));
    }

    private static RegExpr createString() {
        RegExpr a, b, c;

        a = new Range('"');
        b = new Range('"');
        c = new Loop(new Choice(new Range((char) 0, (char) ('"'-1)),
                                     new Range((char) ('"' + 1), (char) -1)));
        c = new Sequence(a, c);
        c = new Sequence(c, b);

        return c;
    }

    private static RegExpr createLineComment() {
        RegExpr a, b, c;

        a = keyword("//");
        b = new Loop(new Range(' ', (char) 0x7f));
        c = new Sequence(a, b);
        c = new Sequence(c, new Range('\n'));
        return c;
    }
}
