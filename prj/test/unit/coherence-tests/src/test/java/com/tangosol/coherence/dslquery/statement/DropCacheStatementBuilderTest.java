/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.InOrder;

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
public class DropCacheStatementBuilderTest
    {
    @Test
    public void shouldRealizeQuery()
            throws Exception
        {
        ExecutionContext          context = mock(ExecutionContext.class);
        NodeTerm                  term    = (NodeTerm) Terms.create("sqlDropCacheNode(from('test'))");

        DropCacheStatementBuilder builder = DropCacheStatementBuilder.INSTANCE;

        DropCacheStatementBuilder.DropCacheStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sCacheName, is("test"));
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for drop cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlDropCacheNode(from(''))");

        DropCacheStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for drop cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlDropCacheNode(from())");

        DropCacheStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for drop cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlDropCacheNode()");

        DropCacheStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        String           cacheName = "test";
        ExecutionContext context   = mock(ExecutionContext.class);

        DropCacheStatementBuilder.DropCacheStatement query
                = new DropCacheStatementBuilder.DropCacheStatement("test")
                {
                @Override
                protected void assertCacheName(String sName, ExecutionContext context)
                    {
                    }
                };

        DropCacheStatementBuilder.DropCacheStatement spyQuery = spy(query);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldDropCache()
            throws Exception
        {
        Session          session = mock(Session.class);
        NamedCache       cache   = mock(NamedCache.class);
        ExecutionContext context = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(anyString(), any(TypeAssertion.class))).thenReturn(cache);

        DropCacheStatementBuilder.DropCacheStatement statement
                = new DropCacheStatementBuilder.DropCacheStatement("test");

        statement.execute(context);

        InOrder inOrder = inOrder(session, cache);

        inOrder.verify(session).getCache(eq("test"), any(TypeAssertion.class));
        inOrder.verify(cache).destroy();
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
