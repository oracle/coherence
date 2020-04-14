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
 * SQLListArchivedSnapshotsOPToken is used for parsing and specifying the AST
 * used for listing archived snapshots.
 * <p>
 * Syntax:
 * <p>
 * LIST ARCHIVED SNAPSHOTS 'service'
 *
 * @author tam  2014.02.25
 * @since 12.2.1
 */
public class SQLListArchivedSnapshotsOPToken
        extends SQLOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLListArchivedSnapshotsOPToken.
     */
    public SQLListArchivedSnapshotsOPToken()
        {
        super("archived");
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        OPScanner s = p.getScanner();

        if (s.advanceWhenMatching("snapshots"))
            {
            Term termService = s.isEndOfStatement()
                               ? AtomicTerm.createNull()
                               : Terms.newTerm("service", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));
            Term termArchive = Terms.newTerm("archived", AtomicTerm.createString("true"));

            return Terms.newTerm(FUNCTOR, termService, termArchive);
            }
        else
            {
            return super.nud(p);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST.
     * Note: this intentionaly calls list snapshots.
     */
    public static final String FUNCTOR = "sqlListSnapshots";
    }
