/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.precedence.OPToken;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQLSelectOPToken is used for parsing and specifying the AST
 * used for a select statement.
 * <p>
 * Syntax:
 * <p>
 * SELECT (* | alias | (properties* aggregators*)
 * FROM 'cache-name' [[AS] alias]
 * [WHERE conditional-expression]
 * [GROUP [BY] properties+]
 *
 * @author djl  2009.09.10
 */
public class SQLSelectOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLSelectOPToken with the given parameters.
     *
     * @param id string identifier for this token
     */
    public SQLSelectOPToken(String id)
        {
        super(id);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        OPScanner s            = parser.getScanner();
        OPToken   currentToken = s.getCurrent();
        String    sCurrent     = currentToken.getValue();

        if (sCurrent == null)
            {
            return super.nud(parser);
            }
        else if (sCurrent.length() == 1)
            {
            char cCurrent = sCurrent.charAt(0);

            if (cCurrent == '.' || cCurrent == ',' || cCurrent == ';')
                {
                return super.nud(parser);
                }
            }

        Term fieldList;
        Term groupBy       = null;
        Term whereClause   = null;
        Term table         = null;
        Term subSelectTerm = null;
        Term alias         = null;
        Term isDistinct    = Terms.newTerm("isDistinct", AtomicTerm.createString("false"));

        if (s.advanceWhenMatching("*"))
            {
            fieldList = Terms.newTerm("fieldList", AtomicTerm.createString("*"));
            }
        else
            {
            if (s.advanceWhenMatching("distinct"))
                {
                isDistinct = Terms.newTerm("isDistinct", AtomicTerm.createString("true"));
                }

            fieldList = Terms.newTerm("fieldList", parser.nodeList("from"));
            }

        Map<Term, Term> subSelectMap = new LinkedHashMap<>();

        if (s.advanceWhenMatching("from"))
            {
            parseSubSelects(parser, s, subSelectMap);

            Term cacheName = parser.expression(OPToken.PRECEDENCE_ASSIGNMENT);

            if ("identifier".equalsIgnoreCase(cacheName.getFunctor())
                    || "literal".equalsIgnoreCase(cacheName.getFunctor()))
                {
                cacheName = AtomicTerm.createString(((AtomicTerm) cacheName.termAt(1)).getValue());
                }

            table = Terms.newTerm("from", cacheName);
            alias = checkAlias(parser, ",", "where", "group");

            s.advanceWhenMatching(",");
            parseSubSelects(parser, s, subSelectMap);

            if (subSelectMap.isEmpty())
                {
                subSelectTerm = Terms.newTerm("subQueries");
                }
            else
                {
                Term[] subSelects = new Term[subSelectMap.size()];
                int    subCount   = 0;

                for (Map.Entry<Term, Term> entry : subSelectMap.entrySet())
                    {
                    subSelects[subCount++] = Terms.newTerm("subQuery", Terms.newTerm("alias", entry.getKey()),
                            entry.getValue());
                    }

                subSelectTerm = Terms.newTerm("subQueries", subSelects);
                }

            if (s.advanceWhenMatching("where"))
                {
                whereClause = Terms.newTerm("whereClause", parser.expression(0));
                }
            else
                {
                whereClause = Terms.newTerm("whereClause");
                }

            if (s.advanceWhenMatching("group"))
                {
                s.advanceWhenMatching("by");
                groupBy = Terms.newTerm("groupBy", parser.nodeList());
                }
            else
                {
                groupBy = Terms.newTerm("groupBy");
                }
            }

        return new NodeTerm(FUNCTOR, new Term[]
            {
            isDistinct, fieldList, table, alias, subSelectTerm, whereClause, groupBy
            });
        }

    private void parseSubSelects(OPParser p, OPScanner s, Map<Term, Term> subSelectMap)
        {
        while (s.getCurrent().getId().equals("("))
            {
            Term   subSelect = p.expression(OPToken.PRECEDENCE_PARENTHESES);
            String subAlias  = s.getCurrentAsStringWithAdvance();

            subSelectMap.put(AtomicTerm.createString(subAlias), subSelect);
            s.advanceWhenMatching(",");
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlSelectNode";
    }
