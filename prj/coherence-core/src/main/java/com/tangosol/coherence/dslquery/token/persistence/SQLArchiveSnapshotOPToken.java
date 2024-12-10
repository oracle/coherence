/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token.persistence;

import com.tangosol.coherence.dsltools.precedence.OPException;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.termtrees.Term;

/**
 * SQLArchiveSnapshotOPToken is used for parsing and specifying the AST
 * used for archiving snapshots.
 * <p>
 * Syntax:
 * <p>
 * ARCHIVE SNAPSHOT 'snapshot-name' 'service'
 *
 * @author tam  2014.08.04
 * @since 12.2.1
 */
public class SQLArchiveSnapshotOPToken
        extends AbstractSQLSnapshotOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLArchiveSnapshotOPToken.
     *
     * @param id the string representing the command
     */
    public SQLArchiveSnapshotOPToken(String id)
        {
        super(id, IDENTIFIER_NODE);
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        OPScanner s = p.getScanner();

        if (s.advanceWhenMatching("snapshot"))
            {
            return process(s, "archive", FUNCTOR);
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
    public static final String FUNCTOR = "sqlArchiveSnapshot";
    }
