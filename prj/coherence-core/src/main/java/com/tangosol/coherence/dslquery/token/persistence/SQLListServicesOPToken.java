/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token.persistence;

import com.tangosol.coherence.dslquery.token.SQLOPToken;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLListServicesOPToken is used for parsing and specifying the AST
 * used for listing services and their persistence mode.
 * <p>
 * Syntax:
 * <p>
 * LIST SERVICES [MANAGER]
 *
 * @author tam  2014.02.25
 * @since 12.2.1
 */
public class SQLListServicesOPToken
        extends SQLOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLListServicesOPToken.
     *
     */
    public SQLListServicesOPToken()
        {
        super("services");
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        OPScanner s = p.getScanner();
        Term termVerbose = Terms.newTerm("environment",
                                         AtomicTerm.createString(s.advanceWhenMatching("environment") ? "true" : "false"));

        return Terms.newTerm(FUNCTOR, termVerbose);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlListServices";
    }
