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
 * SQLTruncateCacheOPToken is used for parsing and specifying the AST
 * used for truncate cache.
 * <p>
 * Syntax:
 * <p>
 * TRUNCATE CACHE 'cache-name'
 *
 * @author bbc 2015.09.01
 */
public class SQLTruncateCacheOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLTruncateCacheOPToken with the given parameters.
     *
     * @param id   the id
     */
    public SQLTruncateCacheOPToken(String id)
        {
        super(id, OPToken.IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        OPScanner scanner = parser.getScanner();
        if (scanner.advanceWhenMatching("cache"))
            {
            Term table = Terms.newTerm("from", AtomicTerm.createString(scanner.getCurrentAsStringWithAdvance()));

            return Terms.newTerm(FUNCTOR, table);
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
    public static final String FUNCTOR = "sqlTruncateCacheNode";
    }
