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
 * that parses a CohQL term tree to produce an instance of a {@link  ListArchiverStatement}.
 *
 * @author tam  2014.08.04
 * @since Coherence 12.2.1
 */
public class ListArchiverStatementBuilder
        extends AbstractStatementBuilder<ListArchiverStatementBuilder.ListArchiverStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ListArchiverStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String sServiceName = atomicStringValueOf(term.findAttribute("service"));

        if (sServiceName == null || sServiceName.isEmpty())
            {
            throw new CohQLException("Service name required for LIST ARCHIVER");
            }

        return new ListArchiverStatement(sServiceName == null || sServiceName.isEmpty() ? null : sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "LIST ARCHIVER 'service'";
        }

    @Override
    public String getDescription()
        {
        return "List the archiver configured for a given service.";
        }

    /**
     * Implementation of the CohQL "LIST ARCHIVER" command.
     */
    public static class ListArchiverStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a ListArchiverStatement that will list the archiver for a service.
         *
         * @param sServiceName the service name to resume
         */
        public ListArchiverStatement(String sServiceName)
            {
            f_sServiceName = sServiceName;
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Object                 oResult;
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);

            try
                {
                if (!helper.serviceExists(f_sServiceName))
                    {
                    throw new CohQLException("Service '" + f_sServiceName + "' does not exist");
                    }

                oResult = helper.getArchiver(f_sServiceName);
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in LIST ARCHIVER");
                }

            return new DefaultStatementResult(oResult, true);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * The service name to list the archiver for.
         */
        private final String f_sServiceName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link ListArchiverStatementBuilder}.
     */
    public static final ListArchiverStatementBuilder INSTANCE = new ListArchiverStatementBuilder();
    }