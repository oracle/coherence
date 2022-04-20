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

import com.tangosol.coherence.dslquery.internal.UpdateSetListMaker;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.GreaterFilter;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tangosol.coherence.dslquery.TermMockitoMatcher.termEquals;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.17
 */
public class UpdateStatementBuilderTest
    {
    @Test
    public void shouldRealizeUpdateQuery()
            throws Exception
        {
        final String sql = "sqlUpdateNode(" + "from('foo'), "
                           + "setList(binaryOperatorNode('==', identifier(bar), literal('new-value'))),"
                           + "whereClause(binaryOperatorNode('>', identifier(barney), literal(10))))";

        final NodeTerm                    term           = (NodeTerm) Terms.create(sql);
        final NodeTerm                    setListTerm    = (NodeTerm) term.findChild("setList");
        final List                        indexedVars    = new ArrayList<>();
        final ParameterResolver           namedVars      = mock(ParameterResolver.class);
        final ExecutionContext            ctx            = mock(ExecutionContext.class);
        final UpdateSetListMaker          transformer    = mock(UpdateSetListMaker.class);
        final InvocableMap.EntryProcessor processor      = mock(InvocableMap.EntryProcessor.class);
        final Filter                      expectedFilter = new GreaterFilter("getBarney", 10);
        final CoherenceQueryLanguage      language       = new CoherenceQueryLanguage();

        when(transformer.makeSetList(any(NodeTerm.class))).thenReturn(processor);
        when(ctx.getCoherenceQueryLanguage()).thenReturn(language);

        UpdateStatementBuilder builder = new UpdateStatementBuilder()
            {
            @Override
            protected UpdateSetListMaker createUpdateSetListMaker(ExecutionContext context, List listBindVars,
                    ParameterResolver namedBindvars)
                {
                assertThat(context, is(sameInstance(ctx)));
                assertThat(listBindVars, is(sameInstance(indexedVars)));
                assertThat(namedBindvars, is(sameInstance(namedVars)));

                return transformer;
                }
            };

        UpdateStatementBuilder.UpdateStatement query
                = builder.realize(ctx, term, indexedVars, namedVars);

        assertThat(query.f_sCache, is("foo"));
        assertThat(query.f_filter, is(expectedFilter));
        assertThat(query.f_processor, is(sameInstance(processor)));
        verify(transformer).makeSetList(termEquals(setListTerm));
        }

    @Test
    public void shouldRealizeUpdateQueryWithNoWhereClause()
            throws Exception
        {
        String sql = "sqlUpdateNode(" + "from('foo'), "
                     + "setList(binaryOperatorNode('==', identifier(bar), literal('new-value'))))";

        ExecutionContext       context        = mock(ExecutionContext.class);
        NodeTerm               term           = (NodeTerm) Terms.create(sql);
        UpdateStatementBuilder builder        = UpdateStatementBuilder.INSTANCE;
        Filter                 expectedFilter = AlwaysFilter.INSTANCE;

        UpdateStatementBuilder.UpdateStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sCache, is("foo"));
        assertThat(query.f_filter, is(expectedFilter));
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for update command");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlUpdateCacheNode(from(''))");

        UpdateStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for update command");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlUpdateCacheNode(from())");

        UpdateStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for update command");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlUpdateCacheNode()");

        UpdateStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        String                      cacheName = "test";
        Filter                      filter    = mock(Filter.class);
        InvocableMap.EntryProcessor processor = mock(InvocableMap.EntryProcessor.class);
        ExecutionContext            context   = mock(ExecutionContext.class);

        UpdateStatementBuilder.UpdateStatement statement
                = new UpdateStatementBuilder.UpdateStatement(cacheName, filter, processor)
                    {
                    @Override
                    protected void assertCacheName(String sName, ExecutionContext context)
                        {
                        }
                    };

        UpdateStatementBuilder.UpdateStatement spyQuery = spy(statement);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldPerformUpdate()
            throws Exception
        {
        String                      cacheName      = "test";
        Session                     session        = mock(Session.class);
        NamedCache                  cache          = mock(NamedCache.class);
        Filter                      filter         = mock(Filter.class);
        InvocableMap.EntryProcessor processor      = mock(InvocableMap.EntryProcessor.class);
        Map                         expectedResult = new HashMap();
        ExecutionContext            context        = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);

        when(session.getCache(eq(cacheName), any(TypeAssertion.class))).thenReturn(cache);
        when(cache.invokeAll(any(Filter.class), any(InvocableMap.EntryProcessor.class))).thenReturn(expectedResult);

        UpdateStatementBuilder.UpdateStatement statement
                = new UpdateStatementBuilder.UpdateStatement(cacheName, filter, processor);

        StatementResult result    = statement.execute(context);

        assertThat((Map) result.getResult(), is(sameInstance(expectedResult)));
        verify(cache).invokeAll(same(filter), same(processor));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
