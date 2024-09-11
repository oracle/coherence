/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.dslquery.operator.BaseOperator;

import com.tangosol.coherence.dsltools.precedence.InfixOPToken;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.ContainsAllFilter;
import com.tangosol.util.filter.ContainsAnyFilter;
import com.tangosol.util.filter.ContainsFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.LikeFilter;
import com.tangosol.util.filter.NotEqualsFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.XorFilter;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.oracle.coherence.testing.util.CollectionUtils.setWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jk  2013.12.02
 */
public class FilterBuilderTest
    {
    @After
    public void cleanupCohQL()
        {
        m_language.clearCustomOperators();
        m_language.setExtractorBuilder(null);
        }

    @Test
    public void shouldAcceptNullIdentifier()
            throws Exception
        {
        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(new Object());

        filterBuilder.acceptIdentifier("NuLl");
        assertThat(filterBuilder.getResult(), is(nullValue()));
        }

    @Test
    public void shouldAcceptBooleanTrueIdentifier()
            throws Exception
        {
        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(new Object());

        filterBuilder.acceptIdentifier("TrUe");
        assertThat(filterBuilder.getResult(), is((Object) true));
        }

    @Test
    public void shouldAcceptBooleanFalseIdentifier()
            throws Exception
        {
        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(new Object());

        filterBuilder.acceptIdentifier("FaLsE");
        assertThat(filterBuilder.getResult(), is((Object) false));
        }

    @Test
    public void shouldAcceptIdentifierTargetingKey()
            throws Exception
        {
        ExtractorBuilder builder       = mock(ExtractorBuilder.class);
        ValueExtractor   extractor     = new PofExtractor(null, 1);
        FilterBuilder    filterBuilder = new FilterBuilder(m_language);

        when(builder.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(builder);

        filterBuilder.setResult(new Object());
        filterBuilder.m_sCacheName = "KeyType";

        filterBuilder.acceptIdentifier("test");
        assertThat(filterBuilder.getResult(), is((Object) extractor));
        verify(builder).realize("KeyType", AbstractExtractor.VALUE, "test");
        }

    @Test
    public void shouldAcceptIdentifierTargetingValue()
            throws Exception
        {
        ExtractorBuilder builder       = mock(ExtractorBuilder.class);
        ValueExtractor   extractor     = new PofExtractor(null, 1);
        FilterBuilder    filterBuilder = new FilterBuilder(m_language);

        when(builder.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(builder);

        filterBuilder.setResult(new Object());
        filterBuilder.m_sCacheName = "ValueType";

        filterBuilder.acceptIdentifier("test");
        assertThat(filterBuilder.getResult(), is((Object) extractor));
        verify(builder).realize("ValueType", AbstractExtractor.VALUE, "test");
        }

    @Test
    public void shouldAcceptList()
            throws Exception
        {
        NodeTerm term = new NodeTerm("test", new Term[] {AtomicTerm.createInteger("10"), AtomicTerm.createInteger("12"),
                AtomicTerm.createInteger("11"), AtomicTerm.createInteger("14"), });

        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(new Object());
        filterBuilder.acceptList(term);

        Object expected = new Object[] {10, 12, 11, 14};

        assertThat(filterBuilder.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptEmptyList()
            throws Exception
        {
        NodeTerm      term          = new NodeTerm("test");

        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(new Object());
        filterBuilder.acceptList(term);

        Object expected = new Object[0];

        assertThat(filterBuilder.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptPath()
            throws Exception
        {
        ExtractorBuilder builder       = mock(ExtractorBuilder.class);
        ValueExtractor   extractor     = new UniversalExtractor("getTest()");
        FilterBuilder    filterBuilder = new FilterBuilder(m_language);
        NodeTerm         term          = new NodeTerm("test",
                                             new Term[] {new NodeTerm("identifier", AtomicTerm.createString("foo")),
                new NodeTerm("identifier", AtomicTerm.createString("bar")), });

        when(builder.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(builder);

        filterBuilder.setResult(new Object());
        filterBuilder.m_sCacheName = "MyType";
        filterBuilder.acceptPath(term);

        assertThat(filterBuilder.getResult(), is((Object) extractor));
        verify(builder).realize("MyType", AbstractExtractor.VALUE, "foo.bar");
        }

    @Test
    public void shouldAcceptNotUnaryOperator()
            throws Exception
        {
        NodeTerm      term          = (NodeTerm) parse("!(foo == 123)");
        Object        expected      = new NotFilter(new EqualsFilter("getFoo", 123));

        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(null);

        filterBuilder.acceptUnaryOperator("!", term.termAt(2));
        assertThat(filterBuilder.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptNegateUnaryOperator()
            throws Exception
        {
        AtomicTerm    term          = AtomicTerm.createInteger("100");
        Object        expected      = -100;

        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(null);

        filterBuilder.acceptUnaryOperator("-", term);
        assertThat(filterBuilder.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptPlusUnaryOperator()
            throws Exception
        {
        AtomicTerm    term          = AtomicTerm.createInteger("100");
        Object        expected      = 100;

        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(null);

        filterBuilder.acceptUnaryOperator("+", term);
        assertThat(filterBuilder.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptNewUnaryOperator()
            throws Exception
        {
        NodeTerm      term          = (NodeTerm) parse("new java.util.Date(12345L)");
        Object        expected      = new Date(12345);

        FilterBuilder filterBuilder = new FilterBuilder(m_language);

        filterBuilder.setResult(null);

        filterBuilder.acceptUnaryOperator("new", term.termAt(2));
        assertThat(filterBuilder.getResult(), is(expected));
        }

    @Test
    public void shouldBuildCustomOperatorFilter()
            throws Exception
        {
        Filter   filter = mock(Filter.class, "MyFilter");
        CustomOp op     = new CustomOp(filter);

        m_language.addOperator(op);

        Filter result = new FilterBuilder(m_language).makeFilter(parse("foo $$ bar"));

        assertThat(result, is(filter));
        }

    @Test
    public void shouldBuildEqualsFilter()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo = 2"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildEqualsFilterWithNegativeLiteral()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", -2.0d);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo = -2.0"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildEqualsFilterWithNewObject()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", new Date(123456L));
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo = new java.util.Date(123456L)"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildEqualsFilterWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("2 = foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildBooleanEqualsFilter()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", true);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo = true"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildBooleanEqualsFilterWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", true);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("true = foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildEqualsFilterUsingEqualsEqualsSymbol()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo == 2"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildEqualsFilterUsingEqualsEqualsSymbolWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("2 == foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildEqualsFilterUsingIs()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo is 2"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildEqualsFilterUsingIsWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new EqualsFilter("getFoo", 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("2 is foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildGreaterThanFilter()
            throws Exception
        {
        Filter expected = new GreaterFilter("getFoo", 3);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo > 3"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildGreaterThanFilterWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new GreaterFilter("getFoo", 3);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("3 < foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildGreaterThanOrEqualFilter()
            throws Exception
        {
        Filter expected = new GreaterEqualsFilter("getFoo", 4);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo >= 4"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildGreaterThanOrEqualFilterWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new GreaterEqualsFilter("getFoo", 4);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("4 <= foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildLessThanFilter()
            throws Exception
        {
        Filter expected = new LessFilter("getFoo", 5);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo < 5"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildLessThanFilterWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new LessFilter("getFoo", 5);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("5 > foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildLessThanOrEqualsFilter()
            throws Exception
        {
        Filter expected = new LessEqualsFilter("getFoo", 6);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo <= 6"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildLessThanOrEqualsFilterWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new LessEqualsFilter("getFoo", 6);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("6 >= foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilter()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 7);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo != 7"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilterWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 7);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("7 != foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilterUsingLtGt()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 8);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo <> 8"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilterUsingLtGtWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 8);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("8 <> foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilterUsingIsNot()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 8);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo is not 8"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilterUsingIsNotWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 8);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("8 is not foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilterUsingIsExclamation()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 8);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo is !8"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildNotEqualFilterUsingIsExclamationWithLiteralOnTheLeft()
            throws Exception
        {
        Filter expected = new NotEqualsFilter("getFoo", 8);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("8 is !foo"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildAndFilter()
            throws Exception
        {
        Filter expected = new AllFilter(new Filter[] {new EqualsFilter("getFoo1", 10),
                new EqualsFilter("getFoo2", 11), });
        Filter filter = new FilterBuilder(m_language).makeFilter(parse("foo1 == 10 && foo2 == 11"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildMultipleAndFilter()
            throws Exception
        {
        Filter expected = new AllFilter(new Filter[] {new EqualsFilter("getFoo1", 12), new EqualsFilter("getFoo2", 13),
                new EqualsFilter("getFoo3", 14), new EqualsFilter("getFoo4", 15)});
        Filter filter =
            new FilterBuilder(m_language).makeFilter(parse("foo1 == 12 && foo2 == 13 && foo3 == 14 && foo4 == 15"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildAnyFilter()
            throws Exception
        {
        Filter expected = new AnyFilter(new Filter[] {new EqualsFilter("getFoo1", 16),
                new EqualsFilter("getFoo2", 17), });
        Filter filter = new FilterBuilder(m_language).makeFilter(parse("foo1 == 16 || foo2 == 17"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildMultipleOrFilter()
            throws Exception
        {
        Filter expected = new AnyFilter(new Filter[] {new EqualsFilter("getFoo1", 18), new EqualsFilter("getFoo2", 19),
                new EqualsFilter("getFoo3", 20), new EqualsFilter("getFoo4", 21)});
        Filter filter =
            new FilterBuilder(m_language).makeFilter(parse("foo1 == 18 || foo2 == 19 || foo3 == 20 || foo4 == 21"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildXorFilter()
            throws Exception
        {
        Filter expected = new XorFilter(new EqualsFilter("getFoo1", 22), new EqualsFilter("getFoo2", 23));

        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo1 == 22 ^^ foo2 == 23"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildComplexLogicalFilter()
            throws Exception
        {
        Filter expected = new AnyFilter(new Filter[] {new AllFilter(new Filter[] {new EqualsFilter("getFoo1", 24),
                new AnyFilter(new Filter[] {new EqualsFilter("getFoo2", 25), new EqualsFilter("getFoo3", 26), }), }),
                new EqualsFilter("getFoo4", 27)});

        Filter filter =
            new FilterBuilder(m_language).makeFilter(parse("foo1 == 24 && (foo2 == 25 || foo3 == 26) || foo4 == 27"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildInFilter()
            throws Exception
        {
        Filter expected = new InFilter("getFoo", setWith(1, 2, 3, 4));
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo in (1,2,3,4)"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildContainsFilter()
            throws Exception
        {
        Filter expected = new ContainsFilter("getFoo", 1);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo contains 1"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildContainsAllFilter()
            throws Exception
        {
        Filter expected = new ContainsAllFilter("getFoo", setWith(1, 2, 3, 4));
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo contains_all (1,2,3,4)"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildContainsAnyFilter()
            throws Exception
        {
        Filter expected = new ContainsAnyFilter("getFoo", setWith(1, 2, 3, 4));
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo contains_any (1,2,3,4)"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildLikeFilter()
            throws Exception
        {
        Filter expected = new LikeFilter("getFoo", "Test%");
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo like \"Test%\""));

        assertThat(filter, is(expected));
        assertThat(((LikeFilter) filter).getEscapeChar(), is((char) 0));
        assertThat(((LikeFilter) filter).isIgnoreCase(), is(false));
        }

    @Test
    public void shouldBuildLikeFilterWithEscapeChar()
            throws Exception
        {
        Filter expected = new LikeFilter("getFoo", "Test%", '\\', false);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo like (\"Test%\", '\\')"));

        assertThat(filter, is(expected));
        assertThat(((LikeFilter) filter).getEscapeChar(), is('\\'));
        assertThat(((LikeFilter) filter).isIgnoreCase(), is(false));
        }

    @Test
    public void shouldBuildBetweenFilter()
            throws Exception
        {
        Filter expected = new BetweenFilter("getFoo", 100, 200);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("foo between 100 and 200"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldWrapWithNotFilter()
            throws Exception
        {
        Filter expected = new NotFilter(new EqualsFilter("getFoo", 2));
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("!(foo = 2)"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildFilterOnKeyField()
            throws Exception
        {
        Filter expected = new EqualsFilter(new UniversalExtractor("foo", null, UniversalExtractor.KEY), 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("key(foo) = 2"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildFilterOnKeyInstance()
            throws Exception
        {
        Filter expected = new EqualsFilter(new KeyExtractor(IdentityExtractor.INSTANCE), 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("key() = 2"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildFilterOnValueInstance()
            throws Exception
        {
        Filter expected = new EqualsFilter(IdentityExtractor.INSTANCE, 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("value() = 2"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildFilterOnValueMethod()
            throws Exception
        {
        Filter expected = new EqualsFilter(new UniversalExtractor("test()"), 2);
        Filter filter   = new FilterBuilder(m_language).makeFilter(parse("test() = 2"));

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildFilterWithNumberedBindVariables()
            throws Exception
        {
        Object[] binds    = new Object[] {"v1", "v2"};
        Filter   expected = new AllFilter(new Filter[] {new EqualsFilter("getFoo1", "v2"),
                new EqualsFilter("getFoo2", "v1"), });
        Filter filter = new FilterBuilder(m_language).makeFilter(parse("foo1 == ?2 && foo2 == ?1"), binds, null);

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildFilterWithNamedBindVariables()
            throws Exception
        {
        Object[]            binds    = new Object[]
            {
            };
        Map<String, String> bindings = new HashMap<String, String>();

        bindings.put("a", "v1");
        bindings.put("b", "v2");

        Filter expected = new AllFilter(new Filter[] {new EqualsFilter("getFoo1", "v2"),
                new EqualsFilter("getFoo2", "v1"), });
        Filter filter = new FilterBuilder(m_language).makeFilter(parse("foo1 == :b && foo2 == :a"), binds, bindings);

        assertThat(filter, is(expected));
        }

    @Test
    public void shouldBuildValueExtractor()
            throws Exception
        {
        ValueExtractor expected = new UniversalExtractor("foo");
        ValueExtractor result   = new FilterBuilder(m_language).makeExtractor((NodeTerm) parse("foo"));

        assertThat(result, is(expected));
        }

    @Test
    public void shouldConvertMapToParameterList()
            throws Exception
        {
        Map map = new LinkedHashMap();

        map.put("Key-1", "Value-1");
        map.put("Key-2", "Value-2");

        ParameterList parameterList = FilterBuilder.newParameterListFromMap(map);

        assertThat(parameterList.size(), is(2));

        Iterator<Parameter> iterator   = parameterList.iterator();
        Parameter           parameter1 = iterator.next();

        assertThat(parameter1.getName(), is("Key-1"));
        assertThat(parameter1.evaluate(new NullParameterResolver()).get(), is((Object) "Value-1"));

        Parameter parameter2 = iterator.next();

        assertThat(parameter2.getName(), is("Key-2"));
        assertThat(parameter2.evaluate(new NullParameterResolver()).get(), is((Object) "Value-2"));
        }

    @Test
    public void shouldOptimizeUseOfValue()
        {
        {
            Filter expected = new GreaterEqualsFilter(new UniversalExtractor("age"), 20);
            Filter filter1  = new FilterBuilder(m_language).makeFilter(parse("value().age >= 20"));
            Filter filter2  = new FilterBuilder(m_language).makeFilter(parse("age >= 20"));

            assertThat(filter1, is(expected));
            assertThat(filter2, is(expected));
            assertThat(filter1, is(filter2));
        }
        {
            Filter expected = new AllFilter(new Filter[] {
                                   new NotEqualsFilter(IdentityExtractor.INSTANCE, null),
                                   new EqualsFilter(new UniversalExtractor("age"), 20)
                                 });
            Filter filter = new FilterBuilder(m_language).makeFilter(parse("value() is not null && value().age is 20"));
            assertThat(filter, is(expected));
        }
        {
            Filter expected = new EqualsFilter(Extractors.chained("address", "city"), "Boston");
            Filter filter   = new FilterBuilder(m_language).makeFilter(parse("value().address.city == 'Boston'"));
            assertThat(filter, is(expected));
        }
        
        }

    private Term parse(String query)
        {
        OPParser parser = new OPParser(query, m_language.filtersTokenTable(), m_language.getOperators());

        return parser.parse();
        }

    /**
     * Custom operator to use in tests
     */
    public class CustomOp
            extends BaseOperator
        {
        public CustomOp(Filter filter)
            {
            super("$$", true);
            m_filter = filter;
            }

        @Override
        public void addToTokenTable(TokenTable tokenTable)
            {
            tokenTable.addToken(new InfixOPToken("$$", 40, "binaryOperatorNode"));
            }

        @Override
        public Filter makeFilter(Object oLeft, Object oRight)
            {
            return m_filter;
            }

        protected Filter m_filter;
        }

    /**
     * The CoherenceQueryLanguage used by these tests
     */
    private final CoherenceQueryLanguage m_language = new CoherenceQueryLanguage();
    }
