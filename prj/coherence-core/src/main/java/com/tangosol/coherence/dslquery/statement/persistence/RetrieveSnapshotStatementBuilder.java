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
 * that parses a CohQL term tree to produce an instance of a {@link RetrieveSnapshotStatement}.
 *
 * @author tam  2014.08.27
 * @since Coherence 12.2.1
 */
public class RetrieveSnapshotStatementBuilder
        extends AbstractStatementBuilder<RetrieveSnapshotStatementBuilder.RetrieveSnapshotStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public RetrieveSnapshotStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String  sServiceName  = atomicStringValueOf(term.findAttribute("service"));
        String  sSnapshotName = atomicStringValueOf(term.findAttribute("snapshotname"));
        boolean fVerbose      = "true".equals(atomicStringValueOf(term.findAttribute("overwrite")));

        if (sSnapshotName == null || sSnapshotName.isEmpty())
            {
            throw new CohQLException("Snapshot name required for retrieve snapshot");
            }

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for retrieve snapshot");
            }

        return new RetrieveSnapshotStatement(sSnapshotName, sServiceName, fVerbose);
        }

    @Override
    public String getSyntax()
        {
        return "RETRIEVE ARCHIVED SNAPSHOT 'snapshot-name' 'service' [OVERWIRTE]";
        }

    @Override
    public String getDescription()
        {
        return "Retrieve an archived snapshot for an individual service from a centralized\n"
               + "location using the archiver configured for the service. If a local snapshot\n"
               + "exists with the same name an error is thrown unless OVERWRITE option is used";
        }

    /**
     * Implementation of the CohQL "RETRIEVE SNAPSHOT" command.
     */
    public static class RetrieveSnapshotStatement
            extends AbstractSnapshotStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new RetrieveSnapshotStatement for the snapshot and service name.
         *
         * @param sSnapshotName  snapshot name to create statement for
         * @param sServiceName   service name to create statement for
         * @param fOverwrite     indicates if the snapshot should be overwritten
         */
        public RetrieveSnapshotStatement(String sSnapshotName, String sServiceName, boolean fOverwrite)
            {
            super(replaceDateMacros(sSnapshotName), sServiceName);
            f_fOverwrite = fOverwrite;
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return getConfirmationMessage("retrieve");
            }

         @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);
            PrintWriter            out    = ctx.getWriter();

            try
                {
                validateServiceExists(helper);
                validateArchivedSnapshotExistsForService(helper);
                helper.ensureReady(ctx, f_sServiceName);

                if (helper.snapshotExists(f_sServiceName, f_sSnapshotName))
                    {
                    if (f_fOverwrite)
                        {
                        out.println("Removing existing local snapshot '" + f_sSnapshotName + "' for service '"
                                + f_sServiceName + "' because OVERWRITE option was specified.");
                        out.flush();
                        helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, f_sSnapshotName, f_sServiceName);
                        out.println("Local snapshot removed");
                        out.flush();
                        }
                    else
                        {
                        throw new CohQLException("A snapshot named '" + f_sSnapshotName + "' already exists for "
                            + "service '" + f_sServiceName + "'.\n"
                            + "Please remove it if you want to retrieve the snapshot or use the OVERWRITE option.");
                        }
                    }

                out.println("Retrieving snapshot '" + f_sSnapshotName + "' for service '" + f_sServiceName + "'");
                out.flush();
                helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, f_sSnapshotName,
                                               f_sServiceName);
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in RETRIEVE SNAPSHOT");
                }

            return new DefaultStatementResult(SUCCESS, true);
            }

        // ----- data members -------------------------------------------

        /**
         * Indicates if we should overwrite a local snapshot if it exists.
         */
        private final boolean f_fOverwrite;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link RemoveSnapshotStatementBuilder}.
     */
    public static final RetrieveSnapshotStatementBuilder INSTANCE = new RetrieveSnapshotStatementBuilder();
    }
