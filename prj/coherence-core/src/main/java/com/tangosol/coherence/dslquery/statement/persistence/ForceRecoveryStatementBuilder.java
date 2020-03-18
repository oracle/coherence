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
 * that parses a CohQL term tree to produce an instance of a {@link ForceRecoveryStatement}.
 *
 * @author tam  2014.09.01
 * @since Coherence 12.2.1.1
 */
public class ForceRecoveryStatementBuilder
          extends AbstractStatementBuilder<ForceRecoveryStatementBuilder.ForceRecoveryStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ForceRecoveryStatement realize(ExecutionContext ctx, NodeTerm
            term, List listBindVars,
            ParameterResolver namedBindVars)
        {
        String sServiceName = atomicStringValueOf(term.findAttribute("service"));

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for force recovery");
            }

        return new ForceRecoveryStatement(sServiceName == null || sServiceName.isEmpty() ? null : sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "FORCE RECOVERY 'service'";
        }

    @Override
    public String getDescription()
        {
        return "Proceed with recovery despite the dynamic quorum policy objections.\n" +
               "This may lead to the partial or full data loss at the corresponding\n" +
               "cache service.";
        }

    /**
     * Implementation of the CohQL "FORCE RECOVERY" command.
     */
    public static class ForceRecoveryStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a ForceRecoveryStatement that will force recovery.
         *
         * @param sServiceName the service name to resume on
         */
        public ForceRecoveryStatement(String sServiceName)
            {
            f_sServiceName = sServiceName;
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return "This operation may result in partial or full data loss. \n" +
                   "Are you sure you want to force recovery of this service '" + f_sServiceName + "'? (y/n): ";
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

                out.println("Forcing Recovery '" + f_sServiceName + "'");
                out.flush();
                helper.invokeOperation(PersistenceToolsHelper.FORCE_RECOVERY, f_sServiceName, new Object[0], new String[0]);
                out.println("Recovery forced");
                out.flush();
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in FORCE RECOVERY");
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
     * An instance of {@link ForceRecoveryStatementBuilder}.
     */
    public static final ForceRecoveryStatementBuilder INSTANCE = new ForceRecoveryStatementBuilder();
    }