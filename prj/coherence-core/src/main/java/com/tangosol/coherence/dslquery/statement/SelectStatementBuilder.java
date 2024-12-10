/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dslquery.internal.SelectListMaker;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import java.io.PrintWriter;

import java.util.List;

import java.util.concurrent.CompletableFuture;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link SelectStatement}.
 *
 * @author jk  2013.12.17
 * @since Coherence 12.2.1
 */
public class SelectStatementBuilder
        extends AbstractStatementBuilder<SelectStatementBuilder.SelectStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public SelectStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                   ParameterResolver namedBindVars)
        {
        SelectListMaker transformer = createSelectListMaker(listBindVars, namedBindVars,
                ctx.getCoherenceQueryLanguage());

        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for select query");
            }

        boolean  isDistinct = getIsDistinct(term);
        String   alias      = getAlias(term);
        NodeTerm fields     = getFields(term);
        NodeTerm whereTerm  = getWhere(term);
        NodeTerm groupBy    = getGroupBy(term);

        if (groupBy != null)
            {
            if (fields == null)
                {
                throw new CohQLException("must have fields for group by to make sense");
                }
            else if (!headsMatch(fields, groupBy))
                {
                throw new CohQLException("group by fields must match head of select list");
                }
            }

        InvocableMap.EntryAggregator aggregator = createAggregator(sCacheName, fields, alias, isDistinct, transformer);
        Filter                       filter     = ensureFilter(whereTerm, sCacheName, alias, listBindVars,
                                                      namedBindVars, ctx);
        boolean                      fReduction = !transformer.hasCalls() &&!isDistinct && aggregator != null;

        return new SelectStatement(sCacheName, filter, aggregator, fReduction);
        }

    @Override
    public String getSyntax()
        {
        return "SELECT (properties* aggregators* | * | alias) FROM 'cache-name' [[AS] alias]\n"
               + "        [WHERE conditional-expression] [GROUP [BY] properties+]";
        }

    @Override
    public String getDescription()
        {
        return "Select an ordered list of properties from the cache named 'cache-name' filtered\n"
               + "by the conditional-expression. If '*' is used then fetch the entire object.\n"
               + "If no conditional-expression is given all elements are selected, so this is not\n"
               + "suggested for large data sets!\n\n"
               + "SELECT aggregators FROM 'cache-name' [[AS] alias] [WHERE conditional-expression]\n\n"
               + "Select an ordered list of aggregators from the cache named 'cache-name' selected\n"
               + "by the conditional-expression.\n"
               + "The aggregators may be MAX, MIN, AVG, SUM, COUNT, LONG_MAX, LONG_MIN, LONG_SUM.\n"
               + "If no conditional-expression is given all elements are selected.\n" + "\n"
               + "SELECT (properties then aggregators) FROM 'cache-name' [[AS] alias]\n"
               + "        [WHERE conditional-expression ] GROUP [BY] properties\n\n"
               + "Select an ordered list of properties aggregators from the cache named\n"
               + "'cache-name' selected by the conditional-expression and grouped by the\n"
               + "set of properties that precedes the aggregators. For example:\n"
               + "    SELECT supplier, SUM(amount), AVG(price) FROM 'orders' GROUP BY supplier\n"
               + "As usual, if no conditional-expression is given all elements are selected.";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the {@link InvocableMap.EntryAggregator} that will aggregate the results of this
     * select query.
     *
     * @param cacheName    the cache being queried
     * @param fields       the fields being selected
     * @param alias        the alias of the cache name
     * @param fDistinct    a flag indicating whether this is a distinct query
     * @param transformer  the transformer to use to transform the field list to extractors
     *
     * @return an InvocableMap.EntryAggregator to use to aggregate the query results.
     */
    protected InvocableMap.EntryAggregator createAggregator(String cacheName, NodeTerm fields, String alias,
            boolean fDistinct, SelectListMaker transformer)
        {
        InvocableMap.EntryAggregator aggregator = null;

        if (!isSelectStarQuery(alias, fields))
            {
            transformer.setAlias(alias);

            transformer.makeSelectsForCache(cacheName, fields);

            if (!transformer.hasCalls())
                {
                if (fDistinct)
                    {
                    aggregator = transformer.getDistinctValues();
                    }
                else
                    {
                    aggregator = transformer.getResultsAsReduction();
                    }
                }
            else
                {
                aggregator = transformer.getResultsAsEntryAggregator();
                }
            }

        return aggregator;
        }

    /**
     * Create an instance of a {@link SelectListMaker}.
     *
     * @param listBindVars   the indexed bind variables that the SelectListMaker should use
     * @param namedBindVars  the named bind variables that the SelectListMaker should use
     * @param language       the CohQL language instance that the SelectListMaker should use
     *
     * @return a SelectListMaker
     */
    protected SelectListMaker createSelectListMaker(List listBindVars, ParameterResolver namedBindVars,
            CoherenceQueryLanguage language)
        {
        return new SelectListMaker(listBindVars, namedBindVars, language);
        }

    /**
     * Return true if this query is of the form "SELECT * FROM cache-name".
     *
     * @param sAlias      the alias for the cache name
     * @param termFields  the field list for the query
     *
     * @return true if this is a "SELECT * FROM cache-name" query
     */
    protected boolean isSelectStarQuery(String sAlias, NodeTerm termFields)
        {
        return termFields.termEqual(Terms.newTerm("fieldList", AtomicTerm.createString("*")))
               || (sAlias != null && termFields.termEqual(Terms.newTerm("fieldList", AtomicTerm.createString(sAlias))));
        }

    // ----- inner class: AsyncSelectStatement ------------------------------

    /**
     * Async implementation of the CohQL "SELECT" command.
     */
    public static class AsyncSelectStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a AsyncSelectStatement that will query the specified cache.
         *
         * @param sCache      the cache to query
         * @param filter      the {@link Filter} to use to query tha cache
         * @param aggregator  the {@link InvocableMap.EntryAggregator} to run against the cache entries
         * @param fReduction  a flag indicating whether this query is a sub-set of entry fields
         */
        AsyncSelectStatement(String sCache, Filter filter,
                             InvocableMap.EntryAggregator aggregator, boolean fReduction)
            {
            f_sCache     = sCache;
            f_filter     = filter;
            f_aggregator = aggregator;
            f_fReduction = fReduction;
            }

        // ----- Statement interface ----------------------------------------

        public CompletableFuture<StatementResult> execute(ExecutionContext ctx)
            {
            NamedCache cache = ctx.getSession().getCache(f_sCache, withoutTypeChecking());
            CompletableFuture<StatementResult> future;

            if (f_aggregator == null)
                {
                future = cache.async().entrySet(f_filter);
                }
            else
                {
                future = cache.async().aggregate(f_filter, f_aggregator);
                }

            return future.thenApply(oResult -> new DefaultStatementResult(oResult, !f_fReduction));
            }

        // ----- accessor methods -------------------------------------------

        /**
         * Return the {@link Filter} to use to execute this query.
         *
         * @return the {@link Filter} to use to execute this query
         */
        public Filter getFilter()
            {
            return f_filter;
            }

        /**
         * Return the {@link InvocableMap.EntryAggregator} to use to
         * execute this query.
         *
         * @return the InvocableMap.EntryAggregator to use to execute
         *         this query
         */
        public InvocableMap.EntryAggregator getAggregator()
            {
            return f_aggregator;
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to query.
         */
        protected final String f_sCache;

        /**
         * The {@link Filter} to use in the query.
         */
        protected final Filter f_filter;

        /**
         * The {@link InvocableMap.EntryAggregator} to use in the query.
         */
        protected final InvocableMap.EntryAggregator f_aggregator;

        /**
         * Flag to denote whether this query is an aggregation to select specific
         * fields from the values of a cache; e.g. select x, y, z from foo.
         */
        protected final boolean f_fReduction;
        }

    // ----- inner class: SelectStatement -----------------------------------

    /**
     * Implementation of the CohQL "SELECT" command.
     */
    public static class SelectStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SelectStatement that will query the specified cache.
         *
         * @param sCache      the cache to query
         * @param filter      the {@link Filter} to use to query tha cache
         * @param aggregator  the {@link InvocableMap.EntryAggregator} to run against the cache entries
         * @param fReduction  a flag indicating whether this query is a sub-set of entry fields
         */
        public SelectStatement(String sCache, Filter filter,
                               InvocableMap.EntryAggregator aggregator, boolean fReduction)
            {
            f_sCache     = sCache;
            f_filter     = filter;
            f_aggregator = aggregator;
            f_fReduction = fReduction;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            NamedCache cache = ctx.getSession().getCache(f_sCache, withoutTypeChecking());
            Object     oResult;

            if (f_aggregator == null)
                {
                oResult = cache.entrySet(f_filter);
                }
            else
                {
                oResult = cache.aggregate(f_filter, f_aggregator);
                }

            return new DefaultStatementResult(oResult, !f_fReduction);
            }

        @Override
        public CompletableFuture<StatementResult> executeAsync(ExecutionContext ctx)
            {
            NamedCache cache = ctx.getSession().getCache(f_sCache, withoutTypeChecking());
            CompletableFuture future;

            if (f_aggregator == null)
                {
                future = cache.async().entrySet(f_filter);
                }
            else
                {
                future = cache.async().aggregate(f_filter, f_aggregator);
                }

            future = future.thenApply(oResult -> new DefaultStatementResult(oResult, !f_fReduction));

            return future;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            if (f_aggregator == null)
                {
                out.printf("CacheFactory.getCache(\"%s\").entrySet(%s)",
                           f_sCache, f_filter);
                }
            else
                {
                out.printf("CacheFactory.getCache(\"%s\").aggregate(%s, %s)",
                           f_sCache, f_filter, f_aggregator);
                }
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCache, ctx);
            }

        // ----- accessor methods -------------------------------------------

        /**
         * Return the {@link Filter} to use to execute this query.
         *
         * @return the {@link Filter} to use to execute this query
         */
        public Filter getFilter()
            {
            return f_filter;
            }

        /**
         * Return the {@link InvocableMap.EntryAggregator} to use to
         * execute this query.
         *
         * @return the InvocableMap.EntryAggregator to use to execute
         *         this query
         */
        public InvocableMap.EntryAggregator getAggregator()
            {
            return f_aggregator;
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to query.
         */
        protected final String f_sCache;

        /**
         * The {@link Filter} to use in the query.
         */
        protected final Filter f_filter;

        /**
         * The {@link InvocableMap.EntryAggregator} to use in the query.
         */
        protected final InvocableMap.EntryAggregator f_aggregator;

        /**
         * Flag to denote whether this query is an aggregation to select specific
         * fields from the values of a cache; e.g. select x, y, z from foo.
         */
        protected final boolean f_fReduction;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of a SelectStatementBuilder.
     */
    public static final SelectStatementBuilder INSTANCE = new SelectStatementBuilder();
    }
