/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement.persistence;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementBuilder;
import com.tangosol.coherence.dslquery.StatementResult;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
import com.tangosol.coherence.dslquery.statement.AbstractStatementBuilder;
import com.tangosol.coherence.dslquery.statement.DefaultStatementResult;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.io.PrintWriter;

import java.util.List;

/**
 * An implementation of a {@link StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link RecoverSnapshotStatement}.
 *
 * @author tam  2014.08.04
 * @since Coherence 12.2.1
 */
public class RecoverSnapshotStatementBuilder
        extends AbstractStatementBuilder<RecoverSnapshotStatementBuilder.RecoverSnapshotStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public RecoverSnapshotStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String sServiceName  = atomicStringValueOf(term.findAttribute("service"));
        String sSnapshotName = atomicStringValueOf(term.findAttribute("snapshotname"));

        if (sSnapshotName == null || sSnapshotName.isEmpty())
            {
            throw new CohQLException("Snapshot name required for recover snapshot");
            }

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for recover snapshot");
            }

        return new RecoverSnapshotStatement(sSnapshotName,
            sServiceName == null || sServiceName.isEmpty() ? null : sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "RECOVER SNAPSHOT 'snapshot-name' 'service'";
        }

    @Override
    public String getDescription()
        {
        return "Recover a snapshot for an individual service.";
        }

    /**
     * Implementation of the CohQL "RECOVER SNAPSHOT" command.
     */
    public static class RecoverSnapshotStatement
            extends AbstractSnapshotStatement
        {
        // ----- constructors -----------------------------------------------

        public RecoverSnapshotStatement(String sSnapshotName, String sServiceName)
            {
            super(replaceDateMacros(sSnapshotName), sServiceName);
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return getConfirmationMessage("recover");
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
                helper.ensureReady(ctx, f_sServiceName);

                out.println("Recovering snapshot '" + f_sSnapshotName + "' for service '" + f_sServiceName + "'");
                out.flush();
                helper.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT, f_sSnapshotName, f_sServiceName);
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in RECOVER SNAPSHOT");
                }

            return new DefaultStatementResult(SUCCESS, true);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link RecoverSnapshotStatementBuilder}.
     */
    public static final RecoverSnapshotStatementBuilder INSTANCE = new RecoverSnapshotStatementBuilder();
    }