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
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLDeleteOPToken is used for parsing and specifying the AST
 * used for a delete query.
 * <p>
 * Syntax:
 * <p>
 * DELETE FROM 'cache-name' [[AS] alias] [WHERE conditional-expression]
 *
 * @author djl  2009.09.10
 */
public class SQLDeleteOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLDeleteOPToken with the given parameters.
     *
     * @param id  string identifier for this token
     */
    public SQLDeleteOPToken(String id)
        {
        super(id, OPToken.IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        Term      whereClause;
        OPScanner s = parser.getScanner();

        if (s.advanceWhenMatching("from"))
            {
            Term table = Terms.newTerm("from", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));
            Term alias = checkAlias(parser, "where");

            if (s.advanceWhenMatching("where"))
                {
                whereClause = Terms.newTerm("whereClause", parser.expression(0));
                }
            else
                {
                whereClause = Terms.newTerm("whereClause");
                }

            return Terms.newTerm(FUNCTOR, table, alias, whereClause);
            }
        else
            {
            return super.nud(parser);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST.
     */
    public static final String FUNCTOR = "sqlDeleteNode";
    }
