/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement.persistence;

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
 * that parses a CohQL term tree to produce an instance of a {@link ArchiveSnapshotStatement}.
 *
 * @author tam  2014.08.27
 * @since Coherence 12.2.1
 */
public class ArchiveSnapshotStatementBuilder
        extends AbstractStatementBuilder<ArchiveSnapshotStatementBuilder.ArchiveSnapshotStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ArchiveSnapshotStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String sServiceName  = atomicStringValueOf(term.findAttribute("service"));
        String sSnapshotName = atomicStringValueOf(term.findAttribute("snapshotname"));

        if (sSnapshotName == null || sSnapshotName.isEmpty())
            {
            throw new CohQLException("Snapshot name required for archive snapshot");
            }

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for archive snapshot");
            }

        return new ArchiveSnapshotStatement(sSnapshotName, sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "ARCHIVE SNAPSHOT 'snapshot-name' 'service'";
        }

    @Override
    public String getDescription()
        {
        return "Archive a snapshot for an individual service to a centralized location using\n"
               + "the archiver configured for the service.";
        }

    /**
     * Implementation of the CohQL "ARCHIVE SNAPSHOT" command.
     */
    public static class ArchiveSnapshotStatement
            extends AbstractSnapshotStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new ArchiveSnapshotStatement for the snapshot and service name.
         *
         * @param sSnapshotName  snapshot name to create statement for
         * @param sServiceName   service name to create statement for
         */
        public ArchiveSnapshotStatement(String sSnapshotName, String sServiceName)
            {
            super(replaceDateMacros(sSnapshotName), sServiceName);
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return getConfirmationMessage("archive");
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);
            PrintWriter            out    = ctx.getWriter();

            try
                {
                validateServiceExists(helper);
                validateSnapshotExistsForService(helper);
                validateSnapshotName(f_sSnapshotName);

                // ensure that the archived snapshot does not exist for the service
                if (helper.archivedSnapshotExists(f_sServiceName, f_sSnapshotName))
                    {
                    throw new CohQLException("Archived snapshot '" + f_sSnapshotName + "' already exists for" +
                                             " service '" + f_sServiceName + "'");
                    }

                helper.ensureReady(ctx, f_sServiceName);

                out.println("Archiving snapshot '" + f_sSnapshotName + "' for service '" + f_sServiceName + "'");
                out.flush();
                helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, f_sSnapshotName, f_sServiceName);
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in ARCHIVE SNAPSHOT");
                }

            return new DefaultStatementResult(SUCCESS, true);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link ArchiveSnapshotStatementBuilder}.
     */
    public static final ArchiveSnapshotStatementBuilder INSTANCE = new ArchiveSnapshotStatementBuilder();
    }