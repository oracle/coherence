/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token.persistence;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.Term;

/**
 * SQLCreateSnapshotOPToken is used for parsing and specifying the AST
 * used for creating snapshots.
 * <p>
 * Syntax:
 * <p>
 * CREATE SNAPSHOT 'snapshot-name' 'service'
 *
 * @author tam  2014.08.04
 * @since 12.2.1
 */
public class SQLCreateSnapshotOPToken
        extends AbstractSQLSnapshotOPToken
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a new SQLCreateSnapshotOPToken.
     *
     */
    public SQLCreateSnapshotOPToken()
        {
        super("snapshot");
        }

    // ----- Operator Precedence API ----------------------------------------

    @Override
    public Term nud(OPParser p)
        {
        return process(p.getScanner(), "create", FUNCTOR);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlCreateSnapshot";
    }
