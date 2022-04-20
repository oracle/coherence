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

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ReflectionExtractor;

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
public class CreateIndexStatementBuilderTest
    {
    @Test
    public void shouldRealizeQuery()
            throws Exception
        {
        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateIndexNode(from('test'),"
                                       + "extractor(derefNode(identifier(foo))))");
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();

        when(context.getCoherenceQueryLanguage()).thenReturn(language);

        CreateIndexStatementBuilder builder = CreateIndexStatementBuilder.INSTANCE;

        CreateIndexStatementBuilder.CreateIndexStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sCache, is("test"));
        assertThat(query.f_extractor, is((Object) new ReflectionExtractor("getFoo")));
        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for create index");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateIndexNode(from(''),"
                                       + "extractor(derefNode(identifier(foo))))");

        CreateIndexStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for create index");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateIndexNode(from(),"
                                       + "extractor(derefNode(identifier(foo))))");

        CreateIndexStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for create index");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateIndexNode("
                                       + "extractor(derefNode(identifier(foo))))");

        CreateIndexStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfExtractorIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("ValueExtractor(s) needed for create index");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateIndexNode(from('test'),extractor())");

        CreateIndexStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfExtractorIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("ValueExtractor(s) needed for create index");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlCreateIndexNode(from('test'))");

        CreateIndexStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        String           cacheName = "test";
        ValueExtractor   extractor = new ReflectionExtractor("getFoo");
        ExecutionContext context   = mock(ExecutionContext.class);

        CreateIndexStatementBuilder.CreateIndexStatement query
                = new CreateIndexStatementBuilder.CreateIndexStatement("test", extractor)
                    {
                    @Override
                    protected void assertCacheName(String sName, ExecutionContext context)
                        {
                        }
                    };

        CreateIndexStatementBuilder.CreateIndexStatement spyQuery = spy(query);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldCreateIndex()
            throws Exception
        {
        ValueExtractor   extractor = new ReflectionExtractor("getFoo");
        Session          session   = mock(Session.class);
        NamedCache       cache     = mock(NamedCache.class);
        ExecutionContext context   = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(anyString(), any(TypeAssertion.class))).thenReturn(cache);

        CreateIndexStatementBuilder.CreateIndexStatement statement
                = new CreateIndexStatementBuilder.CreateIndexStatement("test", extractor);

        statement.execute(context);

        InOrder inOrder = inOrder(session, cache);

        inOrder.verify(session).getCache(eq("test"), any(TypeAssertion.class));
        inOrder.verify(cache).addIndex(new ReflectionExtractor("getFoo"), true, null);
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
