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

import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.11
 */
public class CreateCacheStatementBuilderTest
    {
    @Test
    public void shouldRealizeQuery()
            throws Exception
        {
        ExecutionContext            context = mock(ExecutionContext.class);
        NodeTerm                    term    = (NodeTerm) Terms.create("sqlCreateCacheNode(from('test'))");
        CreateCacheStatementBuilder builder = CreateCacheStatementBuilder.INSTANCE;

        CreateCacheStatementBuilder.CreateCacheStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sCache, is("test"));
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for create cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateCacheNode(from(''))");

        CreateCacheStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for create cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateCacheNode(from())");

        CreateCacheStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for create cache");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateCacheNode()");

        CreateCacheStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldCreateCache()
            throws Exception
        {
        Session          session = mock(Session.class);
        ExecutionContext context = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);

        CreateCacheStatementBuilder.CreateCacheStatement statement
                = new CreateCacheStatementBuilder.CreateCacheStatement("test");

        statement.execute(context);

        verify(session).getCache(eq("test"), any(TypeAssertion.class));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
