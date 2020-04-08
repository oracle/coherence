/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token.persistence;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.token.SQLOPToken;
import com.tangosol.coherence.dsltools.precedence.OPException;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLRemoveSnapshotOPToken is used for parsing and specifying the AST
 * used for creating snapshots.
 * <p>
 * Syntax:
 * <p>
 * REMOVE [ARCHIVED] SNAPSHOT 'snapshot-name' 'service'
 *
 * @author tam  2014.08.04
 */
public class SQLRemoveSnapshotOPToken
        extends SQLOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLListServicesOPToken.
     *
     * @param id the string representing the command
     */
    public SQLRemoveSnapshotOPToken(String id)
        {
        super(id, IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        OPScanner s         = p.getScanner();
        boolean   fArchived = false;

        if (s.advanceWhenMatching("archived"))
            {
            fArchived = true;
            }

        if (s.advanceWhenMatching("snapshot"))
            {
            if (s.isEndOfStatement())
                {
                throw new CohQLException("Snapshot name required for REMOVE SNAPSHOT");
                }

            Term termSnapshot = Terms.newTerm("snapshotname",
                                              AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));

            if (s.isEndOfStatement())
                {
                throw new OPException("Service name required for REMOVE SNAPSHOT");
                }

            Term termService = Terms.newTerm("service", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));
            Term termArchive = Terms.newTerm("archived", AtomicTerm.createString(fArchived ? "true" : "false"));

            return Terms.newTerm(FUNCTOR, termSnapshot, termService, termArchive);
            }
        else
            {
            throw new OPException("Expected SNAPSHOT but found " + s.getCurrentAsString());
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlRemoveSnapshot";
    }
