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
 * SQLCreateOPToken is used for parsing and specifying the AST
 * used for a create cache.
 * <p>
 * Syntax:
 * <p>
 * (ENSURE | CREATE) CACHE 'cache-name'
 *
 * @author jk 2014.02.12
 */
public class SQLCreateCacheOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLCreateCacheOPToken with the given parameters.
     */
    public SQLCreateCacheOPToken()
        {
        super("cache");
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        OPScanner scanner     = parser.getScanner();
        Term      termService = Terms.newTerm("service");
        Term      termLoader  = Terms.newTerm("loader");
        Term      termTable   = Terms.newTerm("from", AtomicTerm.createString(scanner.getCurrentAsStringWithAdvance()));

        if (scanner.advanceWhenMatching("service"))
            {
            termService = Terms.newTerm("service", parser.expression(0));

            if (scanner.advanceWhenMatching("loader"))
                {
                termLoader = Terms.newTerm("loader", parser.expression(0));
                }
            }

        return Terms.newTerm(FUNCTOR, termTable, termService, termLoader);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlCreateCacheNode";
    }
