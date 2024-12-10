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
 * that parses a CohQL term tree to produce an instance of a {@link TruncateCacheStatement}.
 *
 * @author bbc  2015.09.01
 * @since Coherence 12.2.1.1
 */
public class TruncateCacheStatementBuilder
        extends AbstractStatementBuilder<TruncateCacheStatementBuilder.TruncateCacheStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public TruncateCacheStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                      ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for truncate cache");
            }

        return new TruncateCacheStatement(sCacheName);
        }

    @Override
    public String getSyntax()
        {
        return "TRUNCATE CACHE 'cache-name'";
        }

    @Override
    public String getDescription()
        {
        return "Remove all entries from the cache 'cache-name'.";
        }

    // ----- inner class: TruncateCacheStatement --------------------------------

    /**
     * Implementation of the CohQL "TRUNCATE CACHE" command.
     */
    public static class TruncateCacheStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a TruncateCacheStatement.
         *
         * @param sCacheName  the name of the cache to truncate
         */
        public TruncateCacheStatement(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.getSession().getCache(f_sCacheName, withoutTypeChecking()).truncate();

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
            out.printf("CacheFactory.getCache(\"%s\")).truncate()", f_sCacheName);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to be truncated.
         */
        protected final String f_sCacheName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of TruncateCacheStatementBuilder.
     */
    public static final TruncateCacheStatementBuilder INSTANCE = new TruncateCacheStatementBuilder();
    }
