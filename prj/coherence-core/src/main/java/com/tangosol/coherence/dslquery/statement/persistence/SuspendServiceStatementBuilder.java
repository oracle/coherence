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
import com.tangosol.coherence.dslquery.statement.AbstractStatement;
import com.tangosol.coherence.dslquery.statement.AbstractStatementBuilder;
import com.tangosol.coherence.dslquery.statement.DefaultStatementResult;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.io.PrintWriter;

import java.util.List;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link SuspendServiceStatement}.
 *
 * @author tam  2014.09.01
 * @since Coherence 12.2.1
 */
public class SuspendServiceStatementBuilder
        extends AbstractStatementBuilder<SuspendServiceStatementBuilder.SuspendServiceStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public SuspendServiceStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String sServiceName = atomicStringValueOf(term.findAttribute("service"));

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for suspend service");
            }

        return new SuspendServiceStatement(sServiceName == null || sServiceName.isEmpty() ? null : sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "SUSPEND SERVICE 'service'";
        }

    @Override
    public String getDescription()
        {
        return "Suspend a service in preparation for Persistence operations.";
        }

    /**
     * Implementation of the CohQL "SUSPEND SERVICE" command.
     */
    public static class SuspendServiceStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a SuspendServiceStatement that will suspend a service.
         *
         * @param sServiceName the service name to suspend
         */
        public SuspendServiceStatement(String sServiceName)
            {
            f_sServiceName = sServiceName;
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return "Are you sure you want to suspend the service '" + f_sServiceName + "'? (y/n): ";
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);
            PrintWriter            out    = ctx.getWriter();

            try
                {
                if (!helper.serviceExists(f_sServiceName))
                    {
                    throw new CohQLException("Service '" + f_sServiceName + "' does not exist");
                    }

                out.println("Suspending service '" + f_sServiceName + "'");
                out.flush();
                helper.suspendService(f_sServiceName);
                out.println("Service suspend");
                out.flush();
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in SUSPEND SERVICE");
                }

            return new DefaultStatementResult(AbstractSnapshotStatement.SUCCESS, true);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * The service name to resume.
         */
        private final String f_sServiceName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link SuspendServiceStatementBuilder}.
     */
    public static final SuspendServiceStatementBuilder INSTANCE = new SuspendServiceStatementBuilder();
    }