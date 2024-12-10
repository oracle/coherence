/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement.persistence;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
import com.tangosol.coherence.dslquery.statement.AbstractStatementBuilder;
import com.tangosol.coherence.dslquery.statement.DefaultStatementResult;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.io.PrintWriter;

import java.util.List;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link RemoveSnapshotStatement}.
 *
 * @author tam  2014.08.01
 * @since Coherence 12.2.1
 */
public class RemoveSnapshotStatementBuilder
        extends AbstractStatementBuilder<RemoveSnapshotStatementBuilder.RemoveSnapshotStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public RemoveSnapshotStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String  sServiceName  = atomicStringValueOf(term.findAttribute("service"));
        String  sSnapshotName = atomicStringValueOf(term.findAttribute("snapshotname"));
        boolean fArchived     = "true".equals(atomicStringValueOf(term.findAttribute("archived")));

        if (sSnapshotName == null || sSnapshotName.isEmpty())
            {
            throw new CohQLException("Snapshot name required for remove snapshot");
            }

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for remove snapshot");
            }

        return new RemoveSnapshotStatement(sSnapshotName, sServiceName, fArchived);
        }

    @Override
    public String getSyntax()
        {
        return "REMOVE [ARCHIVED] SNAPSHOT 'snapshot-name' 'service'";
        }

    @Override
    public String getDescription()
        {
        return "Remove a snapshot for and individual service. If the ARCHIVED keyword is used\n"
               + "then an archived snapshot of the name will be removed.";
        }

    /**
     * Implementation of the CohQL "REMOVE [ARCHIVED] SNAPSHOT" command.
     */
    public static class RemoveSnapshotStatement
            extends AbstractSnapshotStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new RemoveSnapshotStatement for the snapshot and service name.
         *
         * @param sSnapshotName  snapshot name to create statement for
         * @param sServiceName   service name to create statement for
         * @param fArchive       indicates if the snapshot is archived
         */
        public RemoveSnapshotStatement(String sSnapshotName, String sServiceName, boolean fArchive)
            {
            super(replaceDateMacros(sSnapshotName), sServiceName);
            f_fArchived = fArchive;
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return "Are you sure you want to remove" + (f_fArchived ? "archived " : "") + " snapshot called '"
                   + f_sSnapshotName + "' for service '" + f_sServiceName + "'? (y/n): ";
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);
            PrintWriter            out    = ctx.getWriter();

            try
                {
                if (f_fArchived)
                    {
                    validateServiceExists(helper);
                    validateArchivedSnapshotExistsForService(helper);
                    helper.ensureReady(ctx, f_sServiceName);

                    out.println("Removing archived snapshot '" + f_sSnapshotName + "' for service '" + f_sServiceName
                                + "'");
                    out.flush();
                    helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT, f_sSnapshotName, f_sServiceName);

                    // post condition to prevent return until archived snapshot no longer visible
                    while (helper.archivedSnapshotExists(f_sServiceName, f_sSnapshotName))
                            {
                            Blocking.sleep(SLEEP_TIME);
                            }
                    }
                else
                    {
                    validateServiceExists(helper);
                    validateSnapshotExistsForService(helper);
                    helper.ensureReady(ctx, f_sServiceName);

                    out.println("Removing snapshot '" + f_sSnapshotName + "' for service '" + f_sServiceName + "'");
                    out.flush();
                    helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, f_sSnapshotName, f_sServiceName);

                    // post condition to prevent return until snapshot no longer visible
                    while (helper.snapshotExists(f_sServiceName, f_sSnapshotName))
                        {
                        Blocking.sleep(SLEEP_TIME);
                        }
                    }

                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in REMOVE SNAPSHOT");
                }

            return new DefaultStatementResult(SUCCESS, true);
            }

        // ----- data members -------------------------------------------

        /**
         * Indicates if the snapshot to be removed is an archived snapshot.
         */
        final boolean f_fArchived;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link RemoveSnapshotStatementBuilder}.
     */
    public static final RemoveSnapshotStatementBuilder INSTANCE = new RemoveSnapshotStatementBuilder();
    }