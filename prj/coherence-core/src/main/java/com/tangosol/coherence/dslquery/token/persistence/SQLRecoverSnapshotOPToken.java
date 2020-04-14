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
 * SQLRecoverSnapshotOPToken is used for parsing and specifying the AST
 * used for creating snapshots.
 * <p>
 * Syntax:
 * <p>
 * RECOVER SNAPSHOT 'snapshot-name' 'service'
 *
 * @author tam  2014.08.04
 * @since 12.2.1
 */
public class SQLRecoverSnapshotOPToken
        extends AbstractSQLSnapshotOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLRecoverSnapshotOPToken.
     *
     * @param id the string representing the command
     */
    public SQLRecoverSnapshotOPToken(String id)
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
            return process(s, "recover", FUNCTOR);
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
    public static final String FUNCTOR = "sqlRecoverSnapshot";
    }
