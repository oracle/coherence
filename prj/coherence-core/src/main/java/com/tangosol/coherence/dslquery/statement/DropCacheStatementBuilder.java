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
 * that parses a CohQL term tree to produce an instance of a {@link DropCacheStatement}.
 *
 * @author jk  2013.12.11
 * @since Coherence 12.2.1
 */
public class DropCacheStatementBuilder
        extends AbstractStatementBuilder<DropCacheStatementBuilder.DropCacheStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public DropCacheStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                      ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for drop cache");
            }

        return new DropCacheStatement(sCacheName);
        }

    @Override
    public String getSyntax()
        {
        return "DROP CACHE 'cache-name'";
        }

    @Override
    public String getDescription()
        {
        return "Remove the cache 'cache-name' from the cluster.";
        }

    // ----- inner class: DropCacheStatement --------------------------------

    /**
     * Implementation of the CohQL "DROP CACHE" command.
     */
    public static class DropCacheStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a DropCacheStatement.
         *
         * @param sCacheName  the name of the cache to destroy
         */
        public DropCacheStatement(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.getSession().getCache(f_sCacheName, withoutTypeChecking()).destroy();

            return StatementResult.NULL_RESULT;
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCacheName, ctx);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\")).destroy()", f_sCacheName);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to be destroyed.
         */
        protected final String f_sCacheName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of DropCacheStatementBuilder.
     */
    public static final DropCacheStatementBuilder INSTANCE = new DropCacheStatementBuilder();
    }
