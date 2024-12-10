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
 * SQLListArchiverOPToken is used for parsing and specifying the AST
 * used for showing the archiver implementation for a service.
 * <p>
 * Syntax:
 * <p>
 * LIST ARCHIVER 'service'
 *
 * @author tam  2014.10.20
 * @since 12.2.1
 */
public class SQLListArchiverOPToken
        extends SQLOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLListArchiverOPToken.
     *
     */
    public SQLListArchiverOPToken()
        {
        super("archiver");
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        OPScanner s = p.getScanner();

        Term termService = s.isEndOfStatement()
                           ? AtomicTerm.createNull()
                           : Terms.newTerm("service", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));

        return Terms.newTerm(FUNCTOR, termService);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlListArchiver";
    }
