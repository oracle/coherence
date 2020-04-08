/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.OPException;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLRestoreOPToken is used for parsing and specifying the AST
 * used for restoring a cache.
 * <p>
 * Syntax:
 * <p>
 * RESTORE CACHE 'cache-name' [FROM] [ FILE ] 'filename'
 *
 * @author djl  2009.09.10
 */
public class SQLRestoreOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLRestoreOPToken with the given parameters.
     *
     * @param id string identifier for this token
     *
     */
    public SQLRestoreOPToken(String id)
        {
        super(id, IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        OPScanner s = parser.getScanner();

        if (s.advanceWhenMatching("cache"))
            {
            String cacheName = s.getCurrentAsStringWithAdvance();

            s.advanceWhenMatching("from");
            s.advanceWhenMatching("file");

            String fileName = s.getCurrentAsStringWithAdvance();

            return Terms.newTerm(FUNCTOR, Terms.newTerm("from", AtomicTerm.createString(cacheName)),
                                 Terms.newTerm("file", AtomicTerm.createString(fileName)));
            }
        else
            {
            throw new OPException("Expected CACHE but found " + s.getCurrentAsString());
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlRestoreCacheNode";
    }
