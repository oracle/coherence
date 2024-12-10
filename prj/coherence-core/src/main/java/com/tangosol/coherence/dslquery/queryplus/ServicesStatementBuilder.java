/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dslquery.statement.DefaultStatementResult;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.precedence.OPToken;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A {@link com.tangosol.coherence.dslquery.StatementBuilder} that builds
 * the QueryPlus "SERVICES" command.
 *
 * @author jk  2014.01.06
 * @since Coherence 12.2.1
 */
public class ServicesStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ServicesQueryPlusStatement realize(ExecutionContext ctx, NodeTerm term,
            List listBindVars, ParameterResolver namedBindVars)
        {
        try
            {
            AtomicTerm atomicTerm = (AtomicTerm) term.termAt(1);
            String     sAction    = atomicTerm.getValue();

            return new ServicesQueryPlusStatement(sAction);
            }
        catch (IllegalArgumentException e)
            {
            throw new CohQLException("Invalid services command - valid syntax is: " + getSyntax());
            }
        }

    @Override
    public String getSyntax()
        {
        return "SERVICES INFO";
        }

    @Override
    public String getDescription()
        {
        return "Displays information about the Services on this member.";
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new ServicesCommandOPToken();
        }

    // ----- inner class: ServicesCommandOPToken ----------------------------

    /**
     * A CohQL OPToken representing the QueryPlus "services" command.
     */
    public class ServicesCommandOPToken
            extends AbstractOPToken
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a ServicesCommandOPToken.
         */
        public ServicesCommandOPToken()
            {
            super("services", OPToken.IDENTIFIER_NODE, "servicesCommand");
            }

        // ----- OPToken methods --------------------------------------------

        @Override
        public Term nud(OPParser parser)
            {
            OPScanner scanner = parser.getScanner();
            String    sAction = scanner.getCurrentAsString();

            if (sAction != null)
                {
                scanner.advance();

                return Terms.newTerm(getFunctor(), AtomicTerm.createString(sAction));
                }

            return super.nud(parser);
            }
        }

    // ----- inner class: ServicesCommandOPToken ----------------------------

    /**
     * A class representing the "SERVICES" QueryPlus command.
     *
     * @author jk 2014.03.17
     */
    public class ServicesQueryPlusStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a ServicesQueryPlusStatement that will execute the
         * specified service command action.
         *
         * @param sAction  the action this statement will perform
         */
        public ServicesQueryPlusStatement(String sAction)
            {
            f_sAction = sAction;
            }

        // ----- Statement methods ---------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            if (f_sAction.toLowerCase().equals("info"))
                {
                return dumpServiceInfo(ctx);
                }

            return StatementResult.NULL_RESULT;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return information about the current set of Coherence services.
         *
         * @param ctx  the {@link ExecutionContext} to use to obtain the
         *             current Coherence {@link Cluster}
         *
         * @return information about the current set of Coherence services
         */
        public StatementResult dumpServiceInfo(ExecutionContext ctx)
            {
            Cluster      cluster  = ctx.getCluster();
            List<String> listInfo = new ArrayList<>();

            for (Enumeration enumServiceNames = cluster.getServiceNames(); enumServiceNames.hasMoreElements(); )
                {
                String  serviceName = (String) enumServiceNames.nextElement();
                Service service     = cluster.getService(serviceName);

                if (service instanceof CacheService)
                    {
                    listInfo.add(serviceName);

                    CacheService cacheService = (CacheService) service;

                    for (Enumeration cacheNames = cacheService.getCacheNames(); cacheNames.hasMoreElements(); )
                        {
                        listInfo.add("\t" + String.valueOf(cacheNames.nextElement()));
                        }
                    }
                }

            return new DefaultStatementResult(listInfo);
            }

        // ----- data members -----------------------------------------------

        /**
         * Flag indicating whether this command turns sanity checking on or off.
         */
        protected final String f_sAction;
        }
    }
