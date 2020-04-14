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

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

/**
 * SQLExplainOPToken is used for parsing and specifying the AST
 * used for an explain plan statement.
 * <p>
 * Syntax:
 * <p>
 * EXPLAIN PLAN FOR select stmt | update stmt | delete stmt
 *
 * @author tb  2011.06.05
 */
public class SQLExplainOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLExplainOPToken with the given parameters.
     *
     * @param id string identifier for this token
     */
    public SQLExplainOPToken(String id)
        {
        super(id, OPToken.IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        OPScanner s        = parser.getScanner();
        String    sCurrent = s.getCurrentAsString();

        if (sCurrent == null)
            {
            return super.nud(parser);
            }
        else if (sCurrent.length() == 1)
            {
            char cCurrent = sCurrent.charAt(0);

            if (cCurrent == '.' || cCurrent == ',' || cCurrent == '(' || cCurrent == ';')
                {
                return super.nud(parser);
                }
            }

        Term stmt;

        advanceToStmt(s);

        if (s.advanceWhenMatching("select"))
            {
            SQLSelectOPToken token = SQL_SELECT_OP_TOKEN;

            stmt = token.nud(parser);
            }
        else if (s.advanceWhenMatching("update"))
            {
            SQLUpdateOPToken token = SQL_UPDATE_OP_TOKEN;

            stmt = token.nud(parser);
            }
        else if (s.advanceWhenMatching("delete"))
            {
            SQLDeleteOPToken token = SQL_DELETE_OP_TOKEN;

            stmt = token.nud(parser);
            }
        else
            {
            return super.nud(parser);
            }

        return new NodeTerm(getFunctor(), new Term[] {stmt});
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Advance the scanner past any tokens preceding the statement.
     *
     * @param s the scanner
     */
    protected void advanceToStmt(OPScanner s)
        {
        s.advance("plan");
        s.advance("for");
        }

    /**
     * Get the functor for the new term.
     *
     * @return the functor
     */
    protected String getFunctor()
        {
        return FUNCTOR;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The select token.
     */
    private static final SQLSelectOPToken SQL_SELECT_OP_TOKEN = new SQLSelectOPToken("select");

    /**
     * The update token.
     */
    private static final SQLUpdateOPToken SQL_UPDATE_OP_TOKEN = new SQLUpdateOPToken("update");

    /**
     * The delete token.
     */
    private static final SQLDeleteOPToken SQL_DELETE_OP_TOKEN = new SQLDeleteOPToken("delete");

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlExplainNode";
    }
