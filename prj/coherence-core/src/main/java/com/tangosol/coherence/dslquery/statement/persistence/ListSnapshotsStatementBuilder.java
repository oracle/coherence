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
import java.util.Map;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link ListSnapshotsStatement}.
 *
 * @author tam  2014.08.01
 * @since Coherence 12.2.1
 */
public class ListSnapshotsStatementBuilder
        extends AbstractStatementBuilder<ListSnapshotsStatementBuilder.ListSnapshotsStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ListSnapshotsStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String  sService  = atomicStringValueOf(term.findAttribute("service"));
        boolean fArchived = "true".equals(atomicStringValueOf(term.findAttribute("archived")));

        return new ListSnapshotsStatement(sService, fArchived);

        }

    // ----- StatementBuilder interface -------------------------------------

    @Override
    public String getSyntax()
        {
        return "LIST [ARCHIVED] SNAPSHOTS ['service']";
        }

    @Override
    public String getDescription()
        {
        return "List snapshots for an individual service or all services. If ARCHIVED keyword\n"
               + "is included then the archived snapshots for the service is listed. If there is\n"
               + "no archiver defined for the service, then an exception will be raised.";

        }

    /**
     * Implementation of the CohQL "LIST SNAPSHOTS" command.
     */
    public static class ListSnapshotsStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a ListSnapshotsStatement that will list snapshots for services.
         *
         * @param sService   the service to list snapshots for
         * @param fArchived  if true we want to list archived snapshots
         */
        public ListSnapshotsStatement(String sService, boolean fArchived)
            {
            f_sService  = sService;
            f_fArchived = fArchived;
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Object       oResult = null;
            PersistenceToolsHelper helper = PersistenceToolsHelper.ensurePersistenceToolsHelper(ctx);

            try
                {
                if (f_sService == null)
                    {
                    oResult = f_fArchived ? helper.listArchivedSnapshots() : helper.listSnapshots();
                    }
                else
                    {
                    // retrieve the list of services
                    Map<String, String[]> mapServices = helper.listServices();

                    if (!mapServices.containsKey(f_sService))
                        {
                        throw new CohQLException("Service '" + f_sService + "' does not exist");
                        }

                    oResult = f_fArchived ? helper.listArchivedSnapshots(f_sService) : helper.listSnapshots(f_sService);
                    }
                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in LIST SNAPSHOTS");
                }

            return new DefaultStatementResult(oResult, true);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * Service name to list services for.
         */
        private final String f_sService;

        /**
         * Indicates if we with to list archived snapshots.
         */
        private final boolean f_fArchived;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link ListSnapshotsStatementBuilder}.
     */
    public static final ListSnapshotsStatementBuilder INSTANCE = new ListSnapshotsStatementBuilder();
    }