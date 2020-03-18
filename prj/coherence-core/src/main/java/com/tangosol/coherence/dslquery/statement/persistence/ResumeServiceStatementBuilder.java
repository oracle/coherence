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
 * that parses a CohQL term tree to produce an instance of a {@link ResumeServiceStatement}.
 *
 * @author tam  2014.08.05
 * @since Coherence 12.2.1
 */
public class ResumeServiceStatementBuilder
        extends AbstractStatementBuilder<ResumeServiceStatementBuilder.ResumeServiceStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ResumeServiceStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String sServiceName = atomicStringValueOf(term.findAttribute("service"));

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for resume service");
            }

        return new ResumeServiceStatement(sServiceName == null || sServiceName.isEmpty() ? null : sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "RESUME SERVICE 'service'";
        }

    @Override
    public String getDescription()
        {
        return "Resume a previously suspended service. \n" + "Note: If the service is not suspended, this is a No-op.";
        }

    /**
     * Implementation of the CohQL "RESUME SERVICE" command.
     */
    public static class ResumeServiceStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a ResumeServiceStatement that will resume a suspended service.
         *
         * @param sServiceName the service name to resume
         */
        public ResumeServiceStatement(String sServiceName)
            {
            f_sServiceName = sServiceName;
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return "Are you sure you want to resume the service '" + f_sServiceName + "'? (y/n): ";
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

                out.println("Resuming service '" + f_sServiceName + "'");
                out.flush();
                helper.resumeService(f_sServiceName);
                out.println("Service resumed");
                out.flush();
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in RESUME SERVICE");
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
     * An instance of {@link ResumeServiceStatementBuilder}.
     */
    public static final ResumeServiceStatementBuilder INSTANCE = new ResumeServiceStatementBuilder();
    }