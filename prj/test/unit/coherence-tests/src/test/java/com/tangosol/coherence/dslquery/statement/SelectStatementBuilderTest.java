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

import com.tangosol.coherence.dslquery.internal.SelectListMaker;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.DistinctValues;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tangosol.coherence.dslquery.TermMatcher.matchingTerm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.17
 */
public class SelectStatementBuilderTest
    {
    @Test
    public void shouldRealizeSelectStarQuery()
            throws Exception
        {
        String sql = "sqlSelectNode(" + "isDistinct('false'), " + "fieldList('*'), " + "from('foo'), " + "alias(), "
                     + "whereClause(), " + "groupBy())";

        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create(sql);
        SelectStatementBuilder builder = SelectStatementBuilder.INSTANCE;

        SelectStatementBuilder.SelectStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_aggregator, is(nullValue()));
        assertThat(query.f_filter, is((Filter) AlwaysFilter.INSTANCE));
        assertThat(query.f_sCache, is("foo"));
        assertThat(query.f_fReduction, is(false));
        }

    @Test
    public void shouldRealizeSelectStarQueryUsingAlias()
            throws Exception
        {
        String sql = "sqlSelectNode(" + "isDistinct('false'), " + "fieldList('f'), " + "from('foo'), " + "alias('f'), "
                     + "whereClause(), " + "groupBy())";

        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create(sql);
        SelectStatementBuilder builder = SelectStatementBuilder.INSTANCE;
        SelectStatementBuilder.SelectStatement query   = builder.realize(context, term, null, null);

        assertThat(query.f_aggregator, is(nullValue()));
        assertThat(query.f_filter, is((Filter) AlwaysFilter.INSTANCE));
        assertThat(query.f_sCache, is("foo"));
        assertThat(query.f_fReduction, is(false));
        }

    @Test
    public void shouldRealizeSelectDistinctQuery()
            throws Exception
        {
        final String sql = "sqlSelectNode(" + "isDistinct('true'), " + "fieldList(identifier(bar)), " + "from('foo'), "
                           + "alias('f'), " + "whereClause(), " + "groupBy())";

        final List              indexedBinds = new ArrayList<>();
        final ParameterResolver namedBinds   = mock(ParameterResolver.class);
        final ExecutionContext  ctx          = mock(ExecutionContext.class);
        final NodeTerm          term         = (NodeTerm) Terms.create(sql);
        final SelectListMaker   transformer  = mock(SelectListMaker.class);
        final DistinctValues    aggregator   = new DistinctValues();

        when(transformer.getDistinctValues()).thenReturn(aggregator);

        SelectStatementBuilder builder = new SelectStatementBuilder()
            {
            @Override
            protected SelectListMaker createSelectListMaker(List indexedBindVars, ParameterResolver namedBindVars,
                    CoherenceQueryLanguage language)
                {
                assertThat(indexedBindVars, is(sameInstance(indexedBinds)));
                assertThat(namedBindVars, is(sameInstance(namedBinds)));

                return transformer;
                }
            };

        SelectStatementBuilder.SelectStatement query
                = builder.realize(ctx, term, indexedBinds, namedBinds);

        assertThat(query.f_filter, is((Filter) AlwaysFilter.INSTANCE));
        assertThat(query.f_sCache, is("foo"));
        assertThat((DistinctValues) query.f_aggregator, is(sameInstance(aggregator)));
        assertThat(query.f_fReduction, is(false));

        ArgumentCaptor<NodeTerm> termCaptor = ArgumentCaptor.forClass(NodeTerm.class);
        InOrder                  inOrder    = inOrder(transformer);

        inOrder.verify(transformer).setAlias("f");
        inOrder.verify(transformer).makeSelectsForCache(eq("foo"), termCaptor.capture());
        inOrder.verify(transformer).getDistinctValues();
        assertThat(termCaptor.getValue(), is(matchingTerm(builder.getFields(term))));
        }

    @Test
    public void shouldRealizeSelectQueryAsReducerAggregation()
            throws Exception
        {
        String sql = "sqlSelectNode(" + "isDistinct('false'), " + "fieldList(identifier(bar1),identifier(bar2)), "
                     + "from('foo'), " + "alias('f'), " + "whereClause(), " + "groupBy())";

        final List                         indexedBinds = new ArrayList<>();
        final ParameterResolver            namedBinds   = mock(ParameterResolver.class);
        final ExecutionContext             context      = mock(ExecutionContext.class);
        final NodeTerm                     term         = (NodeTerm) Terms.create(sql);
        final SelectListMaker              transformer  = mock(SelectListMaker.class);
        final InvocableMap.EntryAggregator aggregator   = mock(InvocableMap.EntryAggregator.class);

        when(transformer.getResultsAsReduction()).thenReturn(aggregator);
        when(transformer.hasCalls()).thenReturn(false);

        SelectStatementBuilder builder = new SelectStatementBuilder()
            {
            @Override
            protected SelectListMaker createSelectListMaker(List indexedBindVars, ParameterResolver namedBindVars,
                    CoherenceQueryLanguage language)
                {
                assertThat(indexedBindVars, is(sameInstance(indexedBinds)));
                assertThat(namedBindVars, is(sameInstance(namedBinds)));

                return transformer;
                }
            };

        SelectStatementBuilder.SelectStatement query
                = builder.realize(context, term, indexedBinds, namedBinds);

        assertThat(query.f_filter, is((Filter) AlwaysFilter.INSTANCE));
        assertThat(query.f_sCache, is("foo"));
        assertThat(query.f_aggregator, is(sameInstance(aggregator)));
        assertThat(query.f_fReduction, is(true));

        ArgumentCaptor<NodeTerm> termCaptor = ArgumentCaptor.forClass(NodeTerm.class);
        InOrder                  inOrder    = inOrder(transformer);

        inOrder.verify(transformer).setAlias("f");
        inOrder.verify(transformer).makeSelectsForCache(eq("foo"), termCaptor.capture());
        inOrder.verify(transformer).getResultsAsReduction();
        assertThat(termCaptor.getValue(), is(matchingTerm(builder.getFields(term))));
        }

    @Test
    public void shouldRealizeSelectQueryAsAggregation()
            throws Exception
        {
        String sql = "sqlSelectNode(" + "isDistinct('false'), " + "fieldList(callNode(sum(identifier(a)))), "
                     + "from('foo'), " + "alias('f'), " + "whereClause(), " + "groupBy())";

        final List                         indexedBinds = new ArrayList<>();
        final ParameterResolver            namedBinds   = mock(ParameterResolver.class);
        final ExecutionContext             context      = mock(ExecutionContext.class);
        final NodeTerm                     term         = (NodeTerm) Terms.create(sql);
        final SelectListMaker              transformer  = mock(SelectListMaker.class);
        final InvocableMap.EntryAggregator aggregator   = mock(InvocableMap.EntryAggregator.class);

        when(transformer.getResultsAsEntryAggregator()).thenReturn(aggregator);
        when(transformer.hasCalls()).thenReturn(true);

        SelectStatementBuilder builder = new SelectStatementBuilder()
            {
            @Override
            protected SelectListMaker createSelectListMaker(List indexedBindVars, ParameterResolver namedBindVars,
                    CoherenceQueryLanguage language)
                {
                assertThat(indexedBindVars, is(sameInstance(indexedBinds)));
                assertThat(namedBindVars, is(sameInstance(namedBinds)));

                return transformer;
                }
            };

        SelectStatementBuilder.SelectStatement query
                = builder.realize(context, term, indexedBinds, namedBinds);

        assertThat(query.f_filter, is((Filter) AlwaysFilter.INSTANCE));
        assertThat(query.f_sCache, is("foo"));
        assertThat(query.f_aggregator, is(sameInstance(aggregator)));
        assertThat(query.f_fReduction, is(false));

        ArgumentCaptor<NodeTerm> termCaptor = ArgumentCaptor.forClass(NodeTerm.class);
        InOrder                  inOrder    = inOrder(transformer);

        inOrder.verify(transformer).setAlias("f");
        inOrder.verify(transformer).makeSelectsForCache(eq("foo"), termCaptor.capture());
        inOrder.verify(transformer).getResultsAsEntryAggregator();
        assertThat(termCaptor.getValue(), is(matchingTerm(builder.getFields(term))));
        }

    @Test
    public void shouldRealizeCorrectFilterFromWhereClause()
            throws Exception
        {
        String sql = "sqlSelectNode(" + "isDistinct('false'), " + "fieldList('*'), " + "from('foo'), " + "alias(), "
                     + "whereClause(binaryOperatorNode('==', identifier('bar'), literal('something'))), "
                     + "groupBy())";

        ExecutionContext       context  = mock(ExecutionContext.class);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();

        when(context.getCoherenceQueryLanguage()).thenReturn(language);

        NodeTerm               term     = (NodeTerm) Terms.create(sql);
        SelectStatementBuilder builder  = SelectStatementBuilder.INSTANCE;

        SelectStatementBuilder.SelectStatement query
                = builder.realize(context, term, null, null);

        Filter                 expected = new EqualsFilter(new ReflectionExtractor("getBar"), "something");

        assertThat(query.f_filter, is(expected));
        }

    @Test
    public void shouldRealizeSubQuery()
            throws Exception
        {
        String sql = "sqlSelectNode(" + "isDistinct('false'), " + "fieldList("
                     + "    derefNode(identifier(a), identifier(a)), "
                     + "    derefNode(identifier(a), identifier(b)), identifier(x)), "
                     + "from('dist-test'), alias('a'), " + "subQueries(subQuery(alias('x'), " + "    sqlSelectNode("
                     + "        isDistinct('false'), " + "        fieldList(identifier(j), identifier(k)), "
                     + "        from(derefNode(identifier(a), identifier(bar))), " + "        alias(), "
                     + "        subQueries(), " + "        whereClause(), groupBy()))), "
                     + "whereClause(), groupBy())\n";

        ExecutionContext       context  = mock(ExecutionContext.class);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();

        when(context.getCoherenceQueryLanguage()).thenReturn(language);

        NodeTerm               term    = (NodeTerm) Terms.create(sql);
        SelectStatementBuilder builder = SelectStatementBuilder.INSTANCE;

        SelectStatementBuilder.SelectStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_filter, is(notNullValue()));

        }

    @Test
    public void shouldThrowExceptionIfCacheIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for select query");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlSelectCacheNode(from(''))");

        SelectStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for select query");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlSelectCacheNode(from())");

        SelectStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfCacheIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("Cache name needed for select query");

        ExecutionContext context = mock(ExecutionContext.class);
        NodeTerm         term    = (NodeTerm) Terms.create("sqlSelectCacheNode()");

        SelectStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfGroupByExistsWithoutFields()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("must have fields for group by to make sense");

        ExecutionContext context = mock(ExecutionContext.class);
        String           sql     = "sqlSelectNode(" + "isDistinct('false'), " + "from('foo'), " + "alias('f'), "
                                   + "whereClause(), " + "groupBy(identifier(bar)))";
        NodeTerm term = (NodeTerm) Terms.create(sql);

        SelectStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfGroupByFieldsDoNotMatchSelectFields()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("group by fields must match head of select list");

        ExecutionContext context = mock(ExecutionContext.class);
        String           sql     = "sqlSelectNode(" + "isDistinct('false'), "
                                   + "fieldList(identifier(bar1),identifier(bar2)), " + "from('foo'), "
                                   + "alias('f'), " + "whereClause(), " + "groupBy(identifier(bar2)))";
        NodeTerm term = (NodeTerm) Terms.create(sql);

        SelectStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionLessThanTwoArgsPassedToConcat()
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("CONCAT requires at least two arguments");

        ExecutionContext       context  = mock(ExecutionContext.class);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();

        when(context.getCoherenceQueryLanguage()).thenReturn(language);

        NodeTerm term = (NodeTerm)
                Terms.create("sqlSelectNode(isDistinct(\"false\"), fieldList(identifier(book_)), from(\"book\"), alias(\"book_\"),"
                             + " subQueries(), whereClause(binaryOperatorNode(\"like\", derefNode(identifier(book_), "
                             + "identifier(title)), callNode(CONCAT(literal(\"%\"))))), groupBy())");
        SelectStatementBuilder.INSTANCE.realize(context, term, null, null);
        }

    @Test
    public void shouldAssertCacheExistsInSanityCheck()
            throws Exception
        {
        String                       cacheName  = "test";
        Filter                       filter     = mock(Filter.class);
        InvocableMap.EntryAggregator aggregator = mock(InvocableMap.EntryAggregator.class);
        ExecutionContext             context    = mock(ExecutionContext.class);

        SelectStatementBuilder.SelectStatement query
                = new SelectStatementBuilder.SelectStatement(cacheName, filter, aggregator, false)
                    {
                    @Override
                    protected void assertCacheName(String sName, ExecutionContext context)
                        {
                        }
                    };

        SelectStatementBuilder.SelectStatement spyQuery = spy(query);

        spyQuery.sanityCheck(context);

        verify(spyQuery).assertCacheName(cacheName, context);
        }

    @Test
    public void shouldPerformEntrySetQuery()
            throws Exception
        {
        String           cacheName      = "test";
        Filter           filter         = mock(Filter.class);
        Session          session        = mock(Session.class);
        NamedCache       cache          = mock(NamedCache.class);
        Set              expectedResult = new HashSet();
        ExecutionContext context        = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq(cacheName), any(TypeAssertion.class))).thenReturn(cache);
        when(cache.entrySet(any(Filter.class))).thenReturn(expectedResult);

        SelectStatementBuilder.SelectStatement statement
                = new SelectStatementBuilder.SelectStatement(cacheName, filter, null, false);

        StatementResult result    = statement.execute(context);

        assertThat((Set) result.getResult(), is(sameInstance(expectedResult)));
        verify(cache).entrySet(same(filter));
        }

    @Test
    public void shouldPerformAggregateQuery()
            throws Exception
        {
        String                       cacheName      = "test";
        Filter                       filter         = mock(Filter.class);
        InvocableMap.EntryAggregator aggregator     = mock(InvocableMap.EntryAggregator.class);
        Session                      session        = mock(Session.class);
        NamedCache                   cache          = mock(NamedCache.class);
        Object                       expectedResult = new Object();
        ExecutionContext             context        = mock(ExecutionContext.class);

        when(context.getSession()).thenReturn(session);
        when(session.getCache(eq(cacheName), any(TypeAssertion.class))).thenReturn(cache);
        when(cache.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenReturn(expectedResult);

        SelectStatementBuilder.SelectStatement statement
                = new SelectStatementBuilder.SelectStatement(cacheName, filter, aggregator, false);

        StatementResult result    = statement.execute(context);

        assertThat(result.getResult(), is(sameInstance(expectedResult)));
        verify(cache).aggregate(same(filter), same(aggregator));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
