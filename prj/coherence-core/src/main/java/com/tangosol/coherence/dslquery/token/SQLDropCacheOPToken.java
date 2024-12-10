/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLDropCacheOPToken is used for parsing and specifying the AST
 * used for drop cache
 * <p>
 * Syntax:
 * <p>
 * DROP CACHE 'cache-name'
 *
 * @author jk  2014.02.12
 */
public class SQLDropCacheOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLDropCacheOPToken with the given parameters.
     */
    public SQLDropCacheOPToken()
        {
        super("cache");
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        OPScanner scanner = parser.getScanner();
        Term      table   = Terms.newTerm("from", AtomicTerm.createString(scanner.getCurrentAsStringWithAdvance()));

        return Terms.newTerm(FUNCTOR, table);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlDropCacheNode";
    }
