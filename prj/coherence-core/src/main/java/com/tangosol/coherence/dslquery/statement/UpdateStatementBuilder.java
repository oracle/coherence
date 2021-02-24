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

import com.tangosol.coherence.dslquery.internal.UpdateSetListMaker;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import java.io.PrintWriter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link UpdateStatement}.
 *
 * @author jk  2013.12.17
 * @since Coherence 12.2.1
 */
public class UpdateStatementBuilder
        extends AbstractStatementBuilder<UpdateStatementBuilder.UpdateStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public UpdateStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                   ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for update command");
            }

        String             sAlias      = getAlias(term);
        Term               termSetList = getSetList(term);
        NodeTerm           termWhere   = getWhere(term);
        Filter             filter      = ensureFilter(termWhere, sCacheName, sAlias, listBindVars, namedBindVars, ctx);

        UpdateSetListMaker transformer = createUpdateSetListMaker(ctx, listBindVars, namedBindVars);

        transformer.setAlias(sAlias);

        try
            {
            InvocableMap.EntryProcessor processor = transformer.makeSetList((NodeTerm) termSetList);

            return new UpdateStatement(sCacheName, filter, processor);
            }
        catch (Exception e)
            {
            throw new CohQLException("Error creating update processor", e);
            }
        }

    @Override
    public String getSyntax()
        {
        return "UPDATE 'cache-name' [[AS] alias] SET update-statement {, update-statement}*\n" +
               "        [WHERE conditional-expression]";
        }

    @Override
    public String getDescription()
        {
        return "Update the cache named 'cache-name that are selected by the given conditional\n" +
               "expression. If no conditional-expression is given all entries will be updated!\n" +
               "Use with Care! Assignment of both simple values and java constructors and\n" +
               "static methods are supported. Simple addition and multiplication is supported as\n" +
               "well. E.G. update 'employees' set salary = 1000, vacation = 200 where grade > 7";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create an instance of an {@link UpdateSetListMaker}.
     *
     * @param context        the {@link ExecutionContext} to use to configure the
     *                       UpdateSetListMaker
     * @param listBindVars   the indexed bind variables that the SelectListMaker
     *                       should use
     * @param namedBindVars  the named bind variables that the SelectListMaker
     *                       should use
     *
     * @return an UpdateSetListMaker
     */
    protected UpdateSetListMaker createUpdateSetListMaker(ExecutionContext context, List listBindVars,
            ParameterResolver namedBindVars)
        {
        UpdateSetListMaker transformer = new UpdateSetListMaker(listBindVars, namedBindVars,
                context.getCoherenceQueryLanguage());

        transformer.setExtendedLanguage(context.isExtendedLanguageEnabled());

        return transformer;
        }

    // ----- inner class: UpdateStatement -----------------------------------

    /**
     * Implementation of the CohQL "UPDATE" command.
     */
    public static class UpdateStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a UpdateStatement that will update the specified cache.
         *
         * @param sCache     the name of the cache to update
         * @param filter     the {@link Filter} to select the cache entries
         *                   to update
         * @param processor  the {@link InvocableMap.EntryProcessor} that
         *                   will perform the update
         */
        public UpdateStatement(String sCache, Filter filter,
                               InvocableMap.EntryProcessor processor)
            {
            f_sCache = sCache;
            f_filter = filter;
            f_processor = processor;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Map<?, ?> mapResult = ctx.getSession().getCache(f_sCache, withoutTypeChecking())
                    .invokeAll(f_filter, f_processor);

            return new DefaultStatementResult(mapResult);
            }

        @SuppressWarnings("unchecked")
        @Override
        public CompletableFuture<StatementResult> executeAsync(ExecutionContext ctx)
            {
            return ctx.getSession()
                    .getCache(f_sCache, withoutTypeChecking())
                    .async()
                    .invokeAll(f_filter, f_processor)
                    .thenApply(DefaultStatementResult::new);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\").invokeAll(%s, %s)",
                       f_sCache, f_filter, f_processor);
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCache, ctx);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to be updated.
         */
        protected final String f_sCache;

        /**
         * The {@link Filter} that will be used to select entries to be
         * updated.
         */
        protected final Filter f_filter;

        /**
         * The {@link InvocableMap.EntryProcessor} that will perform the
         * "update" command.
         */
        protected final InvocableMap.EntryProcessor f_processor;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of a UpdateStatementBuilder.
     */
    public static final UpdateStatementBuilder INSTANCE = new UpdateStatementBuilder();
    }
