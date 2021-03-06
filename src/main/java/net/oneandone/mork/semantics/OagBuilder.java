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
package net.oneandone.mork.semantics;

import net.oneandone.graph.EdgeIterator;
import net.oneandone.graph.Graph;
import net.oneandone.mork.grammar.Grammar;
import net.oneandone.mork.misc.GenericException;
import net.oneandone.mork.misc.StringArrayList;
import net.oneandone.sushi.util.IntBitSet;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Build visit sequence for ordered attribute grammar. Based on the paper
 * Uwe Kastens: "Ordered Attribute Grammars", Acta Informatics, 1980.
 */
public class OagBuilder {
    private final Ag semantics;
    private final Layout layout;
    private final Grammar grammar; // TODO: use grammarBuffer instead
    private final IntBitSet symbols;

    public OagBuilder(Ag semantics, Layout layout) {
        this.semantics = semantics;
        this.layout = layout;
        this.grammar = semantics.getGrammar();
        this.symbols = new IntBitSet();
        grammar.getSymbols(symbols);
    }

    //--

    public static Visits[] run(Ag ag, Layout layout, PrintWriter verbose) throws GenericException {
        OagBuilder builder;
        Graph<AttributeOccurrence>[] dp;
        Graph<AttributeOccurrence>[] idp;
        Graph<Attribute>[] ids;
        int i;
        List<Attribute>[][] as;
        StringArrayList symbolTable;
        Graph<Attribute>[] ds;
        Graph[] edp;
        Visits[] visits;

        builder = new OagBuilder(ag, layout);
        dp = builder.createDP();
        idp = builder.createIDP(dp);
        ids = builder.createIDS(idp);
        as = builder.createA(ids);
        ds = builder.createDS(ids, as);
        edp = builder.createEDP(dp, ds);
        visits = builder.createVisits(edp, as);
        if (verbose != null) {
            symbolTable = ag.getGrammar().getSymbolTable();
            for (i = 0; i < dp.length; i++) {
                verbose.println("prod=" + i);
                verbose.println("  dp\t" + aos(symbolTable, dp[i]));
                verbose.println("  idp\t" + aos(symbolTable, idp[i]));
                verbose.println("  edp\t" + aos(symbolTable, edp[i]));
            }
            for (i = 0; i < ids.length; i++) {
                verbose.println(symbolTable.get(i) + ":");
                verbose.println(" ids\t" + as(symbolTable, ids[i]));
                verbose.println(" as\t");
                print(as[i], verbose);
                verbose.println(" ds\t" + as(symbolTable, ds[i]));
            }
        }
        return visits;
    }

    private static void print(List[] as, PrintWriter dest) {
        int i;
        int j;
        int max;

        for (i = 0; i < as.length; i++) {
            dest.print("\t\t" + i + ":");
            max = as[i].size();
            for (j = 0; j < max; j++) {
                dest.print(' ');
                dest.print(((Attribute) as[i].get(j)).name);
            }
            dest.println();
        }
    }

    private static String as(StringArrayList symbolTable, Graph relation) {
        StringBuilder buffer;
        EdgeIterator iter;
        Attribute a;

        buffer = new StringBuilder();
        buffer.append('{');
        iter = relation.edges();
        while (iter.step()) {
            buffer.append(" (");
            a = (Attribute) iter.left();
            buffer.append(a.toString(symbolTable));
            buffer.append(", ");
            a = (Attribute) iter.right();
            buffer.append(a.toString(symbolTable));
            buffer.append(") ");
        }
        buffer.append('}');
        return buffer.toString();
    }

    private static String aos(StringArrayList symbolTable, Graph relation) {
        StringBuilder buffer;
        EdgeIterator iter;
        AttributeOccurrence ao;

        buffer = new StringBuilder();
        buffer.append('{');
        iter = relation.edges();
        while (iter.step()) {
            buffer.append(" (");
            ao = (AttributeOccurrence) iter.left();
            buffer.append(ao.toString(symbolTable));
            buffer.append(", ");
            ao = (AttributeOccurrence) iter.right();
            buffer.append(ao.toString(symbolTable));
            buffer.append(") ");
        }
        buffer.append('}');
        return buffer.toString();
    }


    /**
     * Computes "dependency" relation DP (definition 1).
     *
     * @return array indexed by productions where each Relation contains pairs of
     *         AttributeOccurrences.
     */
    public Graph<AttributeOccurrence>[] createDP() {
        Graph<AttributeOccurrence>[] dp;
        int i;
        int max;
        AttributionBuffer ab;

        dp = new Graph[semantics.getGrammar().getProductionCount()];
        for (i = 0; i < dp.length; i++) {
            dp[i] = new Graph<AttributeOccurrence>();
        }
        max = semantics.getSize();
        for (i = 0; i < max; i++) {
            ab = semantics.get(i);
            addDP(dp[ab.production], ab);
        }
        return dp;
    }

    private void addDP(Graph<AttributeOccurrence> dp, AttributionBuffer ab) {
        int i;
        int max;

        max = ab.getArgCount();
        for (i = 0; i < max; i++) {
            dp.addEdge(ab.getArg(i), ab.result);
        }
    }

    /**
     * Computes "induced dependency" releation IDP (definition 2).
     * The relation contains pairs of AttributeOcurrences.
     *
     * @param dp dependency relation
     */
    public Graph<AttributeOccurrence>[] createIDP(Graph<AttributeOccurrence>[] dp) {
        Graph<AttributeOccurrence>[] idp;
        Graph<AttributeOccurrence>[] idpClosure;
        boolean[] touched;
        int p;
        int q;
        AttributeOccurrence left;
        AttributeOccurrence right;
        int symbol;
        int ofs;
        boolean modified;
        AttributeOccurrence newLeft;
        AttributeOccurrence newRight;
        EdgeIterator<AttributeOccurrence> iter;

        idp = new Graph[dp.length];
        idpClosure = new Graph[dp.length];
        touched = new boolean[dp.length];
        for (p = 0; p < idp.length; p++) {
            idp[p] = new Graph<AttributeOccurrence>();
            idp[p].addGraph(dp[p]);
            idpClosure[p] = new Graph<AttributeOccurrence>();
            idpClosure[p].addGraph(dp[p]);
            idpClosure[p].closureHere();
            touched[p] = true;
        }

        do {
            modified = false;
            for (q = 0; q < idp.length; q++) {
                if (touched[q]) {
                    touched[q] = false;
                    iter = idpClosure[q].edges();
                    while (iter.step()) {
                        left = iter.left();
                        right = iter.right();
                        if (left.sameSymbolOccurrence(right)) {
                            for (p = 0; p < idp.length; p++) {
                                for (ofs = 0; ofs <= grammar.getLength(p); ofs++) {
                                    symbol = semantics.getGrammar().getSymbol(p, ofs);
                                    if (symbol == left.attr.symbol) {
                                        newLeft = new AttributeOccurrence(left.attr, ofs - 1);
                                        newRight = new AttributeOccurrence(right.attr, ofs - 1);
                                        if (idp[p].addEdge(newLeft, newRight)) {
                                            idpClosure[p].addEdge(newLeft, newRight);
                                            idpClosure[p].closureHere();
                                            touched[p] = true;
                                            modified = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } while (modified);
        return idp;
    }

    /**
     * Computes "induced dependencies between attributes of symbols" IDS (Definition 3).
     *
     * @return Array indexed by symbols; relation with pairs of attributes.
     */
    public Graph<Attribute>[] createIDS(Graph<AttributeOccurrence>[] idp) {
        int p;
        int i;
        Graph<Attribute>[] ids;
        AttributeOccurrence left;
        AttributeOccurrence right;
        EdgeIterator<AttributeOccurrence> iter;

        ids = new Graph[symbols.last() + 1];
        for (i = 0; i < ids.length; i++) {
            ids[i] = new Graph<Attribute>();
        }
        for (p = 0; p < idp.length; p++) {
            iter = idp[p].edges();
            while (iter.step()) {
                left = iter.left();
                right = iter.right();
                if (left.sameSymbolOccurrence(right)) {
                    ids[left.attr.symbol].addEdge(left.attr, right.attr);
                }
            }
        }
        return ids;
    }

    /**
     * Computes Axy. (Definition 4).
     */
    public List<Attribute>[][] createA(Graph<Attribute>[] ids) throws GenericException {
        Set<Attribute> internal;
        Set<Attribute> synthesized;
        Set<Attribute> inherited;
        int i;
        List<Attribute>[][] result;

        internal = new HashSet<Attribute>();
        inherited = new HashSet<Attribute>();
        synthesized = new HashSet<Attribute>();
        result = new List[ids.length][];
        for (i = 0; i < ids.length; i++) {
            internal.clear();
            inherited.clear();
            synthesized.clear();
            semantics.getAttributes(i, internal, synthesized, inherited);
            synthesized.addAll(internal);
            result[i] = Partition.createA(synthesized, inherited, ids[i]);
        }
        return result;
    }

    /**
     * Computes DS, the completion of IDS using A. (Definition 5).
     */
    public Graph<Attribute>[] createDS(Graph<Attribute>[] ids, List<Attribute>[][] a) {
        int i;
        Graph<Attribute>[] ds;

        ds = new Graph[ids.length];
        for (i = 0; i < ds.length; i++) {
            ds[i] = createDSx(ids[i], a[i]);
        }
        return ds;
    }

    private Graph<Attribute> createDSx(Graph<Attribute> ids, List<Attribute>[] a) {
        Graph<Attribute> ds;
        int i;
        List<Attribute> leftList;
        int leftSize;
        List<Attribute> rightList;
        int rightSize;
        int left;
        int right;

        ds = new Graph<Attribute>();
        ds.addGraph(ids);
        for (i = 1; i < a.length; i++) {
            leftList = a[i];
            leftSize = leftList.size();
            rightList = a[i - 1];
            rightSize = rightList.size();
            for (left = 0; left < leftSize; left++) {
                for (right = 0; right < rightSize; right++) {
                    ds.addEdge(leftList.get(left), rightList.get(right));
                }
            }
        }
        return ds;
    }

    public Graph[] createEDP(Graph[] dp, Graph<Attribute>[] ds) {
        int i;
        Graph[] eds;

        eds = new Graph[dp.length];
        for (i = 0; i < eds.length; i++) {
            eds[i] = createEDPx(i, dp[i], ds);
        }
        return eds;
    }

    private Graph<AttributeOccurrence> createEDPx(int p, Graph<AttributeOccurrence> dp, Graph<Attribute>[] ds) {
        Graph<AttributeOccurrence> edsP;
        int ofs;
        int maxOfs;
        int symbol;
        EdgeIterator<Attribute> iter;

        edsP = new Graph<AttributeOccurrence>();
        edsP.addGraph(dp);
        maxOfs = semantics.getGrammar().getLength(p);
        for (ofs = 0; ofs <= maxOfs; ofs++) {
            symbol = semantics.getGrammar().getSymbol(p, ofs);
            iter = ds[symbol].edges();
            while (iter.step()) {
                edsP.addEdge(new AttributeOccurrence(iter.left(), ofs - 1),
                        new AttributeOccurrence(iter.right(), ofs - 1));
            }
        }
        return edsP;
    }

    public Visits[] createVisits(Graph<AttributeOccurrence>[] edp, List<Attribute>[][] as) throws GenericException {
        int p;
        Visits[] visits;

        visits = new Visits[edp.length];
        for (p = 0; p < edp.length; p++) {
            visits[p] = Visits.forEDP(p, edp[p], semantics, as, layout);
        }
        return visits;
    }
}
