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
import com.tangosol.coherence.dslquery.statement.FormattedMapStatementResult;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.io.PrintWriter;

import java.util.List;
import java.util.Map;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link ListServicesStatement}.
 *
 * @author tam  2014.08.01
 * @since Coherence 12.2.1
 */
public class ListServicesStatementBuilder
        extends AbstractStatementBuilder<ListServicesStatementBuilder.ListServicesStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ListServicesStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        return new ListServicesStatement("true".equals(atomicStringValueOf(term.findAttribute("environment"))));
        }

    @Override
    public String getSyntax()
        {
        return "LIST SERVICES [ENVIRONMENT]";
        }

    @Override
    public String getDescription()
        {
        return "List services and their persistence mode, quorum status and current\n"
               + "operation status. If you specify ENVIRONMENT then the persistence environment\n"
               + "details are displayed.";
        }

    /**
     * Implementation of the CohQL "LIST SERVICES" command.
     */
    public static class ListServicesStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a ListServicesStatement that will list services and their
         * persistence mode.
         *
         * @param fEnvironment  true if the environment option was used
         */
        public ListServicesStatement(boolean fEnvironment)
            {
            f_fEnvironment = fEnvironment;
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Map<String, String[]>  mapServices;
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);
            DefaultStatementResult result;

            try
                {
                if (f_fEnvironment)
                    {
                    result = new DefaultStatementResult(helper.listServicesEnvironment());
                    }
                else
                    {
                    mapServices = helper.listServices();
                    result      = new FormattedMapStatementResult(mapServices);

                    ((FormattedMapStatementResult) result).setColumnHeaders(new String[] {"Service Name", "Mode",
                        "Quorum Policy", "Current Operation"});
                    }
                }
            catch (Exception e)
                {
                throw new CohQLException("Error in LIST SERVICES: " + e.getMessage());
                }

            return result;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * Indicates if environment option was specified.
         */
        private final boolean f_fEnvironment;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link ListServicesStatementBuilder}.
     */
    public static final ListServicesStatementBuilder INSTANCE = new ListServicesStatementBuilder();
    }
