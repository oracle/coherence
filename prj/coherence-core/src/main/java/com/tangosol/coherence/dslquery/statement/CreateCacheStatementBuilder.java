/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.io.PrintWriter;

import java.util.List;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link CreateCacheStatement}.
 *
 * @author jk  2013.12.11
 * @since Coherence 12.2.1
 */
public class CreateCacheStatementBuilder
        extends AbstractStatementBuilder<CreateCacheStatementBuilder.CreateCacheStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public CreateCacheStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
            ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for create cache");
            }

        return new CreateCacheStatement(sCacheName);
        }

    @Override
    public String getSyntax()
        {
        return "(ENSURE | CREATE) CACHE 'cache-name'";
        }

    @Override
    public String getDescription()
        {
        return "Make sure the NamedCache 'cache-name' exists.";
        }

    // ----- inner class: CreateCacheStatement ------------------------------

    /**
     * Implementation of the CohQL "CREATE CACHE" command.
     */
    public static class CreateCacheStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a CreateCacheStatement that will create a cache
         * with the specified name.
         *
         * @param sCache  the name of the cache to create
         */
        public CreateCacheStatement(String sCache)
            {
            f_sCache = sCache;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.getSession().getCache(f_sCache, withoutTypeChecking());

            return StatementResult.NULL_RESULT;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\"))", f_sCache);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to be created by this command.
         */
        protected final String f_sCache;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of CreateCacheStatementBuilder.
     */
    public static final CreateCacheStatementBuilder INSTANCE = new CreateCacheStatementBuilder();
    }
