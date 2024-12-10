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
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

/**
 * An abstract implementation of a snapshot operation which can
 * be extended to support different persistence operations.
 *
 * @author tam  2014.01.09
 * @since 12.2.1
 */
public abstract class AbstractSQLSnapshotOPToken
        extends SQLOPToken
    {
    // ---- constructors -----------------------------------------------------

    /**
     * Construct a new AbstractSQLSnapshotOPToken.
     *
     * @param id  the string representing the command
     */
    public AbstractSQLSnapshotOPToken(String id)
        {
        super(id);
        }

    /**
     * Construct a new AbstractSQLSnapshotOPToken.
     *
     * @param id           the string representing the command
     * @param sNudASTName  the ast name to use for constructing an ast
     */
    public AbstractSQLSnapshotOPToken(String id, String sNudASTName)
        {
        super(id, sNudASTName);
        }

    // ----- helpers --------------------------------------------------

    /**
     * Process the commands for the given operation and return a
     * valid {@link Term} for the command.
     *
     * @param s           {@link OPScanner} to read commands from
     * @param sOperation  the operation being called
     * @param sFunctor    the current functor
     *
     * @return the processed {@link Term}
     */
    protected Term process(OPScanner s, String sOperation, String sFunctor)
        {
        if (s.isEndOfStatement())
            {
            throw new CohQLException("Snapshot name required for " + sOperation + " snapshot");
            }

        Term termSnapshot = Terms.newTerm("snapshotname", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));

        if (s.isEndOfStatement())
            {
            throw new OPException("Service name required for " + sOperation + " snapshot");
            }

        Term termService = Terms.newTerm("service", AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));

        return Terms.newTerm(sFunctor, termSnapshot, termService);
        }
    }
