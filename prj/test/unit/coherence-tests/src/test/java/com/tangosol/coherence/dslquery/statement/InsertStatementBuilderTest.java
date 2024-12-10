/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static com.tangosol.coherence.dslquery.TermMatcher.matchingTerm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
public class InsertStatementBuilderTest
    {
    @Test
    public void shouldRealizeInsertQuery()
            throws Exception
        {
        String                 sql     = "sqlInsertNode(" + "from('foo'), "
                                         + "key(literal('key-1')), "
                                         + "value(literal('value-1')))";

        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create(sql);
        InsertStatementBuilder builder = InsertStatementBuilder.INSTANCE;

        InsertStatementBuilder.InsertStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sCacheName, is("foo"));
        assertThat((String) query.f_oKey, is("key-1"));
        assertThat((String) query.f_oValue, is("value-1"));
        }

    @Test
    public void shouldRealizeInsertQueryWithNullValue()
            throws Exception
        {
        String                 sql     = "sqlInsertNode(" + "from('foo'), "
                                         + "key(literal('key-1')))";

        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create(sql);
        InsertStatementBuilder builder = InsertStatementBuilder.INSTANCE;

        InsertStatementBuilder.InsertStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sCacheName, is("foo"));
        assertThat((String) query.f_oKey, is("key-1"));
        assertThat(query.f_oValue, is(nullValue()));
        }

    @Test
    public void shouldRealizeInsertQueryWithKeyAndValueFromUpdateSetListMaker()
            throws Exception
        {
        String                   sql         = "sqlInsertNode(" + "from('foo'), " + "key(literal('key-1')), "
                                               + "value(literal('value-1')))";

        final List               indexedVars = new ArrayList<>();
        final ParameterResolver  namedVars   = mock(ParameterResolver.class);
        final ExecutionContext   context     = mock(ExecutionContext.class);
        final NodeTerm           term        = (NodeTerm) Terms.create(sql);
        final UpdateSetListMaker transformer = mock(UpdateSetListMaker.class);
        final Object             key         = new Object();
        final Object             value       = new Object();

        when(transformer.makeObject(any(NodeTerm.class))).thenReturn(value);
        when(transformer.makeObjectForKey(any(NodeTerm.class), same(value))).thenReturn(key);

        InsertStatementBuilder builder = new InsertStatementBuilder()
            {
            @Override
            protected UpdateSetListMaker createUpdateSetListMaker(ExecutionContext ctx, List indexedBindVars,
                    ParameterResolver namedBindVars)
                {
                assertThat(ctx, is(sameInstance(context)));
                assertThat(indexedBindVars, is(sameInstance(indexedVars)));
                assertThat(namedBindVars, is(sameInstance(namedVars)));

                return transformer;
                }
            };

        InsertStatementBuilder.InsertStatement query
                = builder.realize(context, term, indexedVars, namedVars);

        assertThat(query.f_sCacheName, is("foo"));
        assertThat(query.f_oKey, is(sameInstance(key)));
        assertThat(query.f_oValue, is(sameInstance(value)));

        ArgumentCaptor<NodeTerm> valueTermCaptor = ArgumentCaptor.forClass(NodeTerm.class);

        verify(transformer).makeObject(valueTermCaptor.capture());
        assertThat(valueTermCaptor.getValue(), is(matchingTerm(builder.getInsertValue(term))));

        ArgumentCaptor<NodeTerm> keyTermCaptor = ArgumentCaptor.forClass(NodeTerm.class);

        verify(transformer).makeObjectForKey(keyTermCaptor.capture(), same(value));
        assertThat(keyTermCaptor.getValue(), is(matchingTerm(builder.getInsertKey(term))));
        }

    @Test
    public void shouldRealizeInsertQueryWithKeyFromValueGetKeyMethod()
            throws Exception
        {
        String                   sql            = "sqlInsertNode(" + "from('foo'), " + "value(literal('value-1')))";
        final List               indexedVars    = new ArrayList<>();
        final ParameterResolver  environmentMap = mock(ParameterResolver.class);
        final ExecutionContext   ctx            = mock(ExecutionContext.class);
        final NodeTerm           term           = (NodeTerm) Terms.create(sql);
        final UpdateSetListMaker transformer    = mock(UpdateSetListMaker.class);
        final String             key            = "Key-1";
        final Object             value          = new DummyValueWithKey(key, "Test");

        when(transformer.makeObject(any(NodeTerm.class))).thenReturn(value);

        InsertStatementBuilder builder = new InsertStatementBuilder()
            {
            @Override
            protected UpdateSetListMaker createUpdateSetListMaker(ExecutionContext ctx, List indexedBindVars,
                    ParameterResolver namedBindVars)
                {
                assertThat(ctx, is(sameInstance(ctx)));
                assertThat(indexedBindVars, is(sameInstance(indexedVars)));
                assertThat(namedBindVars, is(sameInstance(environmentMap)));

                return transformer;
                }
            };

        InsertStatementBuilder.InsertStatement query
                = builder.realize(ctx, term, indexedVars, environmentMap);

        assertThat(query.f_sCacheName, is("foo"));
        assertThat((String) query.f_oKey, is(sameInstance(key)));
        assertThat(query.f_oValue, is(sameInstance(value)));

        ArgumentCaptor<NodeTerm> valueTermCaptor = ArgumentCaptor.forClass(NodeTerm.class);

        verify(transformer).makeObject(valueTermCaptor.capture());
        assertThat(valueTermCaptor.getValue(), is(matchingTerm(builder.getInsertValue(term))));
        }

    @Test
    public void shouldThrowExceptionWhenKeyNotSpecifiedAndValueHasNoGetKeyMethod()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("No key specified and missing or inaccessible method: "
                                 + DummyValue.class.getCanonicalName() + ".getKey()");

        String                   sql         = "sqlInsertNode(" + "from('foo'), " + "value(literal('value-1')))";

        final List               indexedVars = new ArrayList<>();
        final ParameterResolver  namedVars   = mock(ParameterResolver.class);
        final ExecutionContext   ctx         = mock(ExecutionContext.class);
        final NodeTerm           term        = (NodeTerm) Terms.create(sql);
        final UpdateSetListMaker transformer = mock(UpdateSetListMaker.class);
        final Object             value       = new DummyValue("Id", "Test");

        when(transformer.makeObject(any(NodeTerm.class))).thenReturn(value);

        InsertStatementBuilder builder = new InsertStatementBuilder()
            {
            @Override
            protected UpdateSetListMaker createUpdateSetListMaker(ExecutionContext ctx, List indexedBindVars,
                    ParameterResolver namedBindVars)
                {
                assertThat(ctx, is(sameInstance(ctx)));
                assertThat(indexedBindVars, is(sameInstance(indexedVars)));
                assertThat(namedBindVars, is(sameInstance(namedVars)));

                return transformer;
                }
            };

        builder.realize(ctx, term, indexedVars, namedVars);
        }

    @Test
    public void shouldThrowExceptionWhenKeyNotAndValueIsNull()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("No key specified for insert");

        String            sql            = "sqlInsertNode(from('foo'))";

        List<Object>      environment    = new ArrayList<>();
        ParameterResolver environmentMap = mock(ParameterResolver.class);
        ExecutionContext  context        = mock(ExecutionContext.class);
        NodeTerm          term           = (NodeTerm) Terms.create(sql);

        InsertStatementBuilder.INSTANCE.realize(context, term, environment, environmentMap);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for insert command");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlInsertCacheNode(from(''))");

        InsertStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for insert command");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlInsertCacheNode(from())");

        InsertStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for insert command");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlInsertCacheNode()");

        InsertStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        String                       cacheName  = "test";
        Filter                       filter     = mock(Filter.class);
        InvocableMap.EntryAggregator aggregator = mock(InvocableMap.EntryAggregator.class);
        ExecutionContext             context    = mock(ExecutionContext.class);

        InsertStatementBuilder.InsertStatement query
                = new InsertStatementBuilder.InsertStatement(cacheName, filter, aggregator)
                    {
                    @Override
                    protected void assertCacheName(String sName, ExecutionContext context)
                        {
                        }
                    };

        InsertStatementBuilder.InsertStatement spyQuery = spy(query);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldPerformInsert()
            throws Exception
        {
        String           cacheName      = "test";
        Session          session        = mock(Session.class);
        NamedCache       cache          = mock(NamedCache.class);
        Object           key            = new Object();
        Object           value          = new Object();
        Object           expectedResult = new Object();
        ExecutionContext context        = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq(cacheName), any(TypeAssertion.class))).thenReturn(cache);
        when(cache.put(any(), any())).thenReturn(expectedResult);

        InsertStatementBuilder.InsertStatement statement
                = new InsertStatementBuilder.InsertStatement(cacheName, key, value);

        StatementResult result    = statement.execute(context);

        assertThat(result.getResult(), is(sameInstance(expectedResult)));
        verify(cache).put(same(key), same(value));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
