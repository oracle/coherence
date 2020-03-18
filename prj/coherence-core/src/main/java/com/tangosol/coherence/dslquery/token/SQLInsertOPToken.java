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
 * SQLInsertOPToken is used for parsing and specifying the AST
 * used for a insert statment.
 * <p>
 * Syntax:
 * <p>
 * INSERT INTO 'cache-name'
 * [KEY (literal | new java-constructor | static method)]
 * VALUE (literal | new java-constructor | static method)
 *
 * @author djl  2009.09.10
 */
public class SQLInsertOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLInsertOPToken with the given parameters.
     *
     * @param id          string identifier for this token
     *
     */
    public SQLInsertOPToken(String id)
        {
        super(id, OPToken.IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        Term      key;
        Term      value;
        Term      table;
        OPScanner s = parser.getScanner();

        if (s.advanceWhenMatching("into"))
            {
            table = Terms.newTerm("from", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));

            if (s.advanceWhenMatching("key"))
                {
                key = Terms.newTerm("key", parser.expression(0));
                }
            else
                {
                key = Terms.newTerm("key");
                }

            if (s.advanceWhenMatching("value"))
                {
                value = Terms.newTerm("value", parser.expression(0));
                }
            else
                {
                value = Terms.newTerm("value");
                }

            return Terms.newTerm(FUNCTOR, table, key, value);
            }
        else
            {
            return super.nud(parser);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlInsertNode";
    }
