/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token.persistence;

import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
import com.tangosol.coherence.dslquery.token.SQLOPToken;
import com.tangosol.coherence.dsltools.precedence.OPException;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * SQLValidateSnapshotOPToken is used for parsing and specifying the AST
 * used for creating snapshots.
 * <p>
 * Syntax:
 * <p>
 * VALIDATE [ARCHIVED] SNAPSHOT '/path/to/snapshot' [VERBOSE]
 * <br>or<br>
 * VALIDATE SNAPSHOT 'snapshot-name' 'service-name' [VERBOSE]
 * <br>
 * or
 * <br>
 * VALIDATE ARCHIVED SNAPSHOT 'snapshot-name' 'service-name' [VERBOSE]
 *
 * @author tam  2014.08.06
 * @since 12.2.1
 */
public class SQLValidateSnapshotOPToken
        extends SQLOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLValidateSnapshotOPToken.
     *
     * @param id the string representing the command
     */
    public SQLValidateSnapshotOPToken(String id)
        {
        super(id, IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        OPScanner    s           = p.getScanner();
        boolean      fArchive    = s.advanceWhenMatching("archived");
        Term         termVerbose;
        final String sCommand    = "VALIDATE ARCHIVED SNAPSHOT";

        if (s.advanceWhenMatching("snapshot"))
            {
            Term termType = Terms.newTerm("type", AtomicTerm.createString(fArchive ? "archived" : "snapshot"));

            if (fArchive)
                {
                // should have snapshot-name, cluster-name, service-name & archiver-name
                Term termSnapshotName = PersistenceToolsHelper.getNextTerm(s, "snapshotname", "Snapshot Name", sCommand);
                Term termServiceName  = PersistenceToolsHelper.getNextTerm(s, "servicename", "Service Name", sCommand);

                termVerbose = Terms.newTerm("verbose",
                                            AtomicTerm.createString(s.advanceWhenMatching("verbose")
                                                ? "true" : "false"));

                return Terms.newTerm(FUNCTOR, new Term[]
                    {
                    termSnapshotName, termServiceName, termVerbose, termType
                    });
                }
            else
                {
                // two options here:
                // 1. user specifies snapshot-name and service name or
                // 2. user specifies directory
                // determine this by the number of tokens left

                String sNext = s.peekNextAsString();
                if (sNext == null || (sNext != null && "VERBOSE".equals(sNext.toUpperCase())))
                    {
                    // must be directory only
                    Term termSnapshotDir = Terms.newTerm("snapshotdirectory",
                          AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));

                    termVerbose = Terms.newTerm("verbose",
                          AtomicTerm.createString(s.advanceWhenMatching("verbose") ? "true" : "false"));

                    return Terms.newTerm(FUNCTOR, termSnapshotDir, termVerbose, termType);
                    }
                else
                    {
                    // should have snapshot-name, service-name
                    Term termSnapshotName = PersistenceToolsHelper.getNextTerm(s, "snapshotname", "Snapshot Name", sCommand);
                    Term termServiceName  = PersistenceToolsHelper.getNextTerm(s, "servicename", "Service Name", sCommand);

                    termVerbose = Terms.newTerm("verbose",
                      AtomicTerm.createString(s.advanceWhenMatching("verbose") ? "true" : "false"));

                    return Terms.newTerm(FUNCTOR, termSnapshotName, termServiceName, termVerbose, termType);
                    }
                }
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
    public static final String FUNCTOR = "sqlValidateSnapshot";
    }
