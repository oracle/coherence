/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.Filter;

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import com.tangosol.util.processor.ConditionalRemove;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.InOrder;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.11
 */
public class DeleteStatementBuilderTest
    {
    @Test
    public void shouldRealizeQueryWithAlwaysFilterIfNoWhereClauseTerm()
            throws Exception
        {
        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create("sqlDeleteNode(from('test'))");

        DeleteStatementBuilder builder = DeleteStatementBuilder.INSTANCE;

        DeleteStatementBuilder.DeleteStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sCache, is("test"));
        assertThat(query.f_filter, is((Filter) AlwaysFilter.INSTANCE));
        }

    @Test
    public void shouldRealizeQueryWithFilter()
            throws Exception
        {
        String ast = "sqlDeleteNode(from('test')," + "whereClause(binaryOperatorNode('&&', "
                     + "            binaryOperatorNode('==', identifier(bar1), bindingNode('?', literal(1))), "
                     + "            binaryOperatorNode('==', identifier(bar2), bindingNode(':', identifier(A))))))";

        List<Object>      indexedVars     = Arrays.asList((Object) 19);
        ParameterList     namedParameters = new SimpleParameterList(new Parameter("A", 20));
        ParameterResolver namedVars       = new ResolvableParameterList(namedParameters);
        Filter            expectedFilter  = new AllFilter(new Filter[] {new EqualsFilter("getBar1", 19),
                new EqualsFilter("getBar2", 20)});

        ExecutionContext       context  = mock(ExecutionContext.class);
        NodeTerm               term     = (NodeTerm) Terms.create(ast);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();

        when(context.getCoherenceQueryLanguage()).thenReturn(language);

        DeleteStatementBuilder builder = DeleteStatementBuilder.INSTANCE;

        DeleteStatementBuilder.DeleteStatement query
                = builder.realize(context, term, indexedVars, namedVars);

        assertThat(query.f_sCache, is("test"));
        assertThat(query.f_filter, is(expectedFilter));
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for delete query");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlDeleteNode(from(''))");

        DeleteStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for delete query");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlDeleteNode(from())");

        DeleteStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for delete query");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlDeleteNode()");

        DeleteStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        String           cacheName = "test";
        Filter           filter    = new EqualsFilter("getFoo", 19L);
        ExecutionContext context   = mock(ExecutionContext.class);

        DeleteStatementBuilder.DeleteStatement query
                = new DeleteStatementBuilder.DeleteStatement("test", filter)
                {
                @Override
                protected void assertCacheName(String sName, ExecutionContext context)
                    {
                    }
                };

        DeleteStatementBuilder.DeleteStatement spyQuery = spy(query);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldDelete()
            throws Exception
        {
        Session          session = mock(Session.class);
        NamedCache       cache   = mock(NamedCache.class);
        Filter           filter  = new EqualsFilter("getFoo", 19L);
        ExecutionContext context = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(anyString(), any(TypeAssertion.class))).thenReturn(cache);

        DeleteStatementBuilder.DeleteStatement statement
                = new DeleteStatementBuilder.DeleteStatement("test", filter);

        statement.execute(context);

        InOrder inOrder = inOrder(session, cache);

        inOrder.verify(session).getCache(eq("test"), any(TypeAssertion.class));
        inOrder.verify(cache).invokeAll(filter, new ConditionalRemove(AlwaysFilter.INSTANCE));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
