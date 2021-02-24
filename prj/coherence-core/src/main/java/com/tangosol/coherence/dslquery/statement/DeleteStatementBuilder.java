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

import com.tangosol.util.Filter;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.ConditionalRemove;

import java.io.PrintWriter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link DeleteStatement}.
 *
 * @author jk  2013.12.11
 * @since Coherence 12.2.1
 */
public class DeleteStatementBuilder
        extends AbstractStatementBuilder<DeleteStatementBuilder.DeleteStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public DeleteStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                   ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for delete query");
            }

        Filter filter = ensureFilter(getWhere(term), sCacheName, getAlias(term),
                listBindVars, namedBindVars, ctx);

        return new DeleteStatement(sCacheName, filter);
        }

    @Override
    public String getSyntax()
        {
        return "DELETE FROM 'cache-name'[[AS] alias] [WHERE conditional-expression]";
        }

    @Override
    public String getDescription()
        {
        return "Delete the entries from the cache 'cache-name' that match the conditional\n" +
               "expression. If no conditional-expression is given all entries will be deleted!\n" +
               "Use with Care!";
        }

    // ----- inner class: DeleteStatement -----------------------------------

    /**
     * Implementation of the CohQL "DELETE" query.
     */
    public static class DeleteStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create an instance of {@link DeleteStatement} that will delete
         * all entries from the specified cache that match the given
         * {@link Filter}.
         *
         * @param sCacheName  the name of the cache to remove entries from
         * @param filter      the Filter to use to determine the entries to
         *                    be removed
         */
        public DeleteStatement(String sCacheName, Filter filter)
            {
            f_sCache = sCacheName;
            f_filter = filter;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Map map = ctx.getSession().getCache(f_sCache, withoutTypeChecking())
                    .invokeAll(f_filter, new ConditionalRemove<>(AlwaysFilter.INSTANCE()));

            return new DefaultStatementResult(map.entrySet());
            }

        @Override
        public CompletableFuture<StatementResult> executeAsync(ExecutionContext ctx)
            {
            return ctx.getSession()
                    .getCache(f_sCache, withoutTypeChecking())
                    .async()
                    .invokeAll(f_filter, new ConditionalRemove<>(AlwaysFilter.INSTANCE()))
                    .thenApply(map -> new DefaultStatementResult(map.entrySet()));
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCache, ctx);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\")." +
                       "invokeAll(%s, new ConditionalRemove(AlwaysFilter.INSTANCE))",
                       f_sCache, f_filter);
            }

        // ----- data members ---------------------------------------------------

        /**
         * The cache name containing the entries to be deleted
         */
        protected final String f_sCache;

        /**
         * The {@link Filter} to be used in the CohQL "delete" command.
         */
        protected final Filter f_filter;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of DeleteStatementBuilder.
     */
    public static final DeleteStatementBuilder INSTANCE = new DeleteStatementBuilder();
    }
