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

import com.tangosol.util.aggregator.QueryRecorder;

import java.io.PrintWriter;

import java.util.List;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link QueryRecorderStatement}.
 *
 * @author jk  2013.12.17
 * @since Coherence 12.2.1
 */
public class QueryRecorderStatementBuilder
        extends AbstractStatementBuilder<QueryRecorderStatementBuilder.QueryRecorderStatement>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a QueryRecorderStatementBuilder of the specified type.
     *
     * @param recordType  the type of query recorder to build
     */
    protected QueryRecorderStatementBuilder(QueryRecorder.RecordType recordType)
        {
        m_recordType = recordType;
        }

    // ----- StatementBuilder interface -------------------------------------

    @Override
    public QueryRecorderStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
            ParameterResolver namedBindVars)
        {
        NodeTerm termStmt   = (NodeTerm) term.termAt(1);
        String   sCacheName = getCacheName(termStmt);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for query plan");
            }

        String   sAlias    = getAlias(termStmt);
        NodeTerm termWhere = getWhere(termStmt);
        Filter   filter    = ensureFilter(termWhere, sCacheName, sAlias, listBindVars, namedBindVars, ctx);

        return new QueryRecorderStatement(sCacheName, filter, m_recordType);
        }

    @Override
    public String getSyntax()
        {
        if (m_recordType == QueryRecorder.RecordType.EXPLAIN)
            {
            return "SHOW PLAN 'CohQL command' | EXPLAIN PLAN for 'CohQL command'";
            }

        return "TRACE 'CohQL command'";
        }

    @Override
    public String getDescription()
        {
        if (m_recordType == QueryRecorder.RecordType.EXPLAIN)
            {
            return "Shows what the CohQL command would do rather than executing it.";
            }

        return "Shows what the CohQL command would do rather than executing it.";
        }

    // ----- inner class:  QueryRecorderStatement ---------------------------

    /**
     * Implementation of the CohQL "QueryRecorder" command.
     */
    public static class QueryRecorderStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a QueryRecorderStatement that produces a plan or trace
         * of the specified filter query against the specified cache.
         *
         * @param sCacheName  the cache to be queried
         * @param filter      the {@link Filter} to show the plan or trace for
         * @param type        the type of query recorder - explain plan or trace
         */
        public QueryRecorderStatement(String sCacheName, Filter filter,
                                      QueryRecorder.RecordType type)
            {
            f_sCacheName = sCacheName;
            f_filter     = filter;
            f_aggregator = new QueryRecorder<>(type);
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Object oResult = ctx.getSession()
                    .getCache(f_sCacheName, withoutTypeChecking())
                    .aggregate(f_filter, f_aggregator);

            return new DefaultStatementResult(oResult);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\").aggregate(%s, %s)",
                       f_sCacheName, f_filter, f_aggregator);
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCacheName, ctx);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to query.
         */
        protected final String f_sCacheName;

        /**
         * The {@link Filter} to be explained or traced.
         */
        protected final Filter f_filter;

        /**
         * The type of query recorder to run.
         */
        protected final QueryRecorder<Object,Object> f_aggregator;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of a QueryRecorderStatementBuilder that builds EXPLAIN PLAN queries.
     */
    public static final QueryRecorderStatementBuilder EXPLAIN_INSTANCE =
        new QueryRecorderStatementBuilder(QueryRecorder.RecordType.EXPLAIN);

    /**
     * An instance of a QueryRecorderStatementBuilder that builds TRACE queries.
     */
    public static final QueryRecorderStatementBuilder TRACE_INSTANCE =
        new QueryRecorderStatementBuilder(QueryRecorder.RecordType.TRACE);

    // ----- data members ---------------------------------------------------

    /**
     * The type of query recorder that this builder builds.
     */
    protected QueryRecorder.RecordType m_recordType;
    }
