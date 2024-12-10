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
 * SQLDropIndexOPToken is used for parsing and specifying the AST
 * used for drop index.
 * <p>
 * Syntax:
 * <p>
 * DROP INDEX [ON] 'cache-name' value-extractor-list
 *
 * @author jk 2014.02.12
 * @since Coherence 12.2.1
 */
public class SQLDropIndexOPToken
        extends SQLOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLDropIndexOPToken.
     */
    public SQLDropIndexOPToken()
        {
        super("index");
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser parser)
        {
        OPScanner scanner = parser.getScanner();

        scanner.advanceWhenMatching("on");

        Term table      = Terms.newTerm("from", AtomicTerm.createString(scanner.getCurrentAsStringWithAdvance()));
        Term extractors = Terms.newTerm("extractor", parser.nodeList());

        return Terms.newTerm(FUNCTOR, table, extractors);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST.
     */
    public static final String FUNCTOR = "sqlDropIndexNode";
    }
