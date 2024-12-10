/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.QueryRecorder;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.EqualsFilter;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.17
 */
public class QueryRecorderStatementBuilderTest
    {
    @Test
    public void shouldRealizeExplainQuery()
            throws Exception
        {
        String sql = "sqlExplainNode(sqlSelectNode(" + "fieldList('*'), " + "from('foo'), " + "whereClause(), "
                     + "groupBy()))";

        ExecutionContext              context = mock(ExecutionContext.class);
        NodeTerm                      term    = (NodeTerm) Terms.create(sql);
        QueryRecorderStatementBuilder builder = QueryRecorderStatementBuilder.EXPLAIN_INSTANCE;

        QueryRecorderStatementBuilder.QueryRecorderStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_aggregator.getType(), is(QueryRecorder.RecordType.EXPLAIN));
        }

    @Test
    public void shouldRealizeTraceQuery()
            throws Exception
        {
        String sql = "sqlTraceNode(sqlSelectNode(" + "fieldList('*'), " + "from('foo'), " + "whereClause(), "
                     + "groupBy()))";

        ExecutionContext              context = mock(ExecutionContext.class);
        NodeTerm                      term    = (NodeTerm) Terms.create(sql);
        QueryRecorderStatementBuilder builder = QueryRecorderStatementBuilder.TRACE_INSTANCE;

        QueryRecorderStatementBuilder.QueryRecorderStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_aggregator.getType(), is(QueryRecorder.RecordType.TRACE));
        }

    @Test
    public void shouldRealizeCorrectFilterFromWhereClause()
            throws Exception
        {
        String sql = "sqlExplainNode(sqlSelectNode(" + "from('foo'), " + "alias(), "
                     + "whereClause(binaryOperatorNode('==', identifier('bar'), literal('something'))), "
                     + "groupBy()))";

        ExecutionContext       context  = mock(ExecutionContext.class);
        NodeTerm               term     = (NodeTerm) Terms.create(sql);
        Filter                 expected = new EqualsFilter(new ReflectionExtractor("getBar"), "something");
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();

        when(context.getCoherenceQueryLanguage()).thenReturn(language);

        QueryRecorderStatementBuilder.QueryRecorderStatement query
                = QueryRecorderStatementBuilder.EXPLAIN_INSTANCE.realize(context, term, null,
                                           null);

        assertThat(query.f_filter, is(expected));
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for query plan");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlExplainNode(sqlSelectCacheNode(from('')))");

        QueryRecorderStatementBuilder.EXPLAIN_INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for query plan");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlExplainNode(sqlSelectCacheNode(from()))");

        QueryRecorderStatementBuilder.EXPLAIN_INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for query plan");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlExplainNode(sqlSelectCacheNode())");

        QueryRecorderStatementBuilder.EXPLAIN_INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        String           cacheName = "test";
        Filter           filter    = mock(Filter.class);
        ExecutionContext context   = mock(ExecutionContext.class);

        QueryRecorderStatementBuilder.QueryRecorderStatement query
                = new QueryRecorderStatementBuilder.QueryRecorderStatement(cacheName, filter, QueryRecorder.RecordType.EXPLAIN)
                {
                @Override
                protected void assertCacheName(String sName, ExecutionContext context)
                    {
                    }
                };

        QueryRecorderStatementBuilder.QueryRecorderStatement spyQuery = spy(query);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldPerformExplainQuery()
            throws Exception
        {
        String                   cacheName      = "test";
        Filter                   filter         = mock(Filter.class);
        Session                  session        = mock(Session.class);
        NamedCache               cache          = mock(NamedCache.class);
        Object                   expectedResult = new Object();
        ExecutionContext         context        = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq(cacheName), any(TypeAssertion.class))).thenReturn(cache);
        when(cache.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenReturn(expectedResult);

        QueryRecorderStatementBuilder.QueryRecorderStatement statement
                = new QueryRecorderStatementBuilder.QueryRecorderStatement(cacheName,
                        filter, QueryRecorder.RecordType.EXPLAIN);

        StatementResult result = statement.execute(context);

        assertThat(result.getResult(), is(sameInstance(expectedResult)));

        ArgumentCaptor<QueryRecorder> argumentCaptor = ArgumentCaptor.forClass(QueryRecorder.class);

        verify(cache).aggregate(eq(filter), argumentCaptor.capture());

        QueryRecorder recorder = argumentCaptor.getValue();

        assertThat(recorder.getType(), is(QueryRecorder.RecordType.EXPLAIN));
        }

    @Test
    public void shouldPerformTraceQuery()
            throws Exception
        {
        String           cacheName      = "test";
        Filter           filter         = mock(Filter.class);
        Session          session        = mock(Session.class);
        NamedCache       cache          = mock(NamedCache.class);
        Object           expectedResult = new Object();
        ExecutionContext context        = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq(cacheName), any(TypeAssertion.class))).thenReturn(cache);
        when(cache.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenReturn(expectedResult);

        QueryRecorderStatementBuilder.QueryRecorderStatement statement
                = new QueryRecorderStatementBuilder.QueryRecorderStatement(cacheName, filter,
                        QueryRecorder.RecordType.TRACE);

        StatementResult result = statement.execute(context);

        assertThat(result.getResult(), is(sameInstance(expectedResult)));

        ArgumentCaptor<QueryRecorder> argumentCaptor = ArgumentCaptor.forClass(QueryRecorder.class);

        verify(cache).aggregate(eq(filter), argumentCaptor.capture());

        QueryRecorder recorder = argumentCaptor.getValue();

        assertThat(recorder.getType(), is(QueryRecorder.RecordType.TRACE));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
