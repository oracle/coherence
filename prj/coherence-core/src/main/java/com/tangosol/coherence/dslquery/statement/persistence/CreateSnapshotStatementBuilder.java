/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement.persistence;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

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
 * that parses a CohQL term tree to produce an instance of a {@link CreateSnapshotStatement}.
 *
 * @author tam  2014.08.04
 * @since Coherence 12.2.1
 */
public class CreateSnapshotStatementBuilder
        extends AbstractStatementBuilder<CreateSnapshotStatementBuilder.CreateSnapshotStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public CreateSnapshotStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String sServiceName  = atomicStringValueOf(term.findAttribute("service"));
        String sSnapshotName = atomicStringValueOf(term.findAttribute("snapshotname"));

        if (sSnapshotName == null || sSnapshotName.isEmpty())
            {
            throw new CohQLException("Snapshot name required for create snapshot");
            }

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for create snapshot");
            }

        return new CreateSnapshotStatement(sSnapshotName, sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "CREATE SNAPSHOT 'snapshot-name' 'service'";
        }

    @Override
    public String getDescription()
        {
        return "Create a snapshot of cache contents for an individual service.\n"
               + "If you do not specify a service, then all services configured for either\n"
               + "on-demand or active mode will be included in snapshot. You can supply the\n"
               + "following to timestamp the snapshot:\n"
               + "%y - Year, %m - Month, %d - Day, %hh - Hour, %mm - Minute, %w - weekday,\n"
               + "%M - Month name";
        }

    /**
     * Implementation of the CohQL "CREATE SNAPSHOT" command.
     */
    public static class CreateSnapshotStatement
            extends AbstractSnapshotStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new CreateSnapshotStatement for the snapshot and service name.
         *
         * @param sSnapshotName  snapshot name to create statement for
         * @param sServiceName   service name to create statement for
         */
        public CreateSnapshotStatement(String sSnapshotName, String sServiceName)
            {
            super(replaceDateMacros(sSnapshotName), sServiceName);
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return getConfirmationMessage("create");
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);
            PrintWriter            out    = ctx.getWriter();

            try
                {
                validateSnapshotName(f_sSnapshotName);
                validateServiceExists(helper);

                // Temporary Debugging only
                String sErrorMsg = null;
                for (int i = 25; i > 0 && helper.snapshotExists(f_sServiceName, f_sSnapshotName); --i)
                    {
                    if (sErrorMsg == null)
                        {
                        sErrorMsg = "A snapshot named '" + f_sSnapshotName + "' already exists for " +
                                    "service '" + f_sServiceName + "'";
                        }
                    Logger.warn(sErrorMsg + ": " + helper.listSnapshots());
                    Blocking.sleep(250L);
                    }

                if (sErrorMsg != null)
                    {
                    throw new CohQLException(sErrorMsg);
                    }

                helper.ensureReady(ctx, f_sServiceName);
                out.println("Creating snapshot '" + f_sSnapshotName + "' for service '" + f_sServiceName + "'");
                out.flush();
                helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, f_sSnapshotName, f_sServiceName);
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in CREATE SNAPSHOT");
                }

            return new DefaultStatementResult(SUCCESS, true);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link CreateSnapshotStatementBuilder}.
     */
    public static final CreateSnapshotStatementBuilder INSTANCE = new CreateSnapshotStatementBuilder();
    }
