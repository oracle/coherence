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
 * SQLSourceOPToken is used for parsing and specifying the AST
 * used for sourcing (including) a file.
 * <p>
 * Syntax:
 * <p>
 * SOURCE FROM [FILE] 'filename'
 *
 * @author djl  2009.09.10
 */
public class SQLSourceOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLSourceOPToken with the given parameters.
     *
     * @param id string identifier for this token
     *
     */
    public SQLSourceOPToken(String id)
        {
        super(id, IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        Term      file;
        OPScanner s    = parser.getScanner();
        boolean   flag = s.advanceWhenMatching("from");

        if (getId().equals("@") || flag)
            {
            s.advanceWhenMatching("file");
            file = Terms.newTerm("file", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));

            return Terms.newTerm(FUNCTOR, file);
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
    public static final String FUNCTOR = "sqlSourceNode";
    }
