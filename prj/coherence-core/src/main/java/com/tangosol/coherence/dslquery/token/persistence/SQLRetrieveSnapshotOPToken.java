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
 * SQLRetrieveSnapshotOPToken is used for parsing and specifying the AST
 * used for retrieving snapshots.
 * <p>
 * Syntax:
 * <p>
 * RETRIEVE ARCHIVED SNAPSHOT 'snapshot-name' 'service' [OVERWRITE]
 *
 * @author tam  2014.08.04
 * @since 12.2.1
 */
public class SQLRetrieveSnapshotOPToken
        extends SQLOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLRetrieveSnapshotOPToken.
     *
     * @param id the string representing the command
     */
    public SQLRetrieveSnapshotOPToken(String id)
        {
        super(id, IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        OPScanner    s        = p.getScanner();
        final String sCommand = "RETRIEVE ARCHIVED SNAPSHOT";

        if (s.isEndOfStatement())
            {
            throw new OPException("Please include ARCHIVED keyword for " + sCommand);
            }

        if (s.advanceWhenMatching("archived"))
            {
            if (s.advanceWhenMatching("snapshot"))
                {
                Term termSnapshotName = PersistenceToolsHelper.getNextTerm(s, "snapshotname", "Snapshot Name",
                                            sCommand);
                Term termServiceName = PersistenceToolsHelper.getNextTerm(s, "service", "Service Name", sCommand);
                Term termOverwrite = Terms.newTerm("overwrite",
                                                   AtomicTerm.createString(s.advanceWhenMatching("overwrite")
                                                       ? "true" : "false"));

                return Terms.newTerm(FUNCTOR, termSnapshotName, termServiceName, termOverwrite);
                }
            else
                {
                throw new OPException("Expected SNAPSHOT but found " + s.getCurrentAsString());
                }
            }
        else
            {
            return super.nud(p);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlRetrieveSnapshot";
    }
