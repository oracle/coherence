/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExtractorBuilder;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.pof.reflect.SimplePofPath;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.AbstractAggregator;
import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.DoubleMax;
import com.tangosol.util.aggregator.DoubleMin;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ExtractorProcessor;
import org.junit.After;
import org.junit.Test;

import java.io.StringReader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.02
 */
public class SelectListMakerTest
    {
    @After
    public void resetCoherenceQueryLanguage()
        {
        m_language.clearCustomFunctions();
        m_language.setExtractorBuilder(null);
        }

    @Test
    public void shouldAcceptNullIdentifier()
            throws Exception
        {
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.setResult(new Object());

        selectListMaker.walk(parse("NuLl"));
        assertThat(selectListMaker.getResult(), is(nullValue()));
        }

    @Test
    public void shouldAcceptBooleanTrueIdentifier()
            throws Exception
        {
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.setResult(new Object());

        selectListMaker.walk(parse("TrUe"));
        assertThat(selectListMaker.getResult(), is((Object) true));
        }

    @Test
    public void shouldAcceptBooleanFalseIdentifier()
            throws Exception
        {
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.setResult(new Object());

        selectListMaker.walk(parse("FaLsE"));
        assertThat(selectListMaker.getResult(), is((Object) false));
        }

    @Test
    public void shouldAcceptIdentifierTargetingValue()
            throws Exception
        {
        ExtractorBuilder mapper          = mock(ExtractorBuilder.class);
        ValueExtractor   extractor       = new ReflectionExtractor("getTest");
        SelectListMaker  selectListMaker = new SelectListMaker(null, null, m_language);

        when(mapper.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(mapper);
        selectListMaker.setResult(new Object());
        selectListMaker.m_sCacheName = "TestType";

        selectListMaker.walk(parse("test"));
        assertThat(selectListMaker.getResult(), is((Object) extractor));
        verify(mapper).realize("TestType", AbstractExtractor.VALUE, "test");
        }

    @Test
    public void shouldAcceptIdentifierTargetingKey()
            throws Exception
        {
        ExtractorBuilder mapper          = mock(ExtractorBuilder.class);
        ValueExtractor   extractor       = new ReflectionExtractor("getTest");
        SelectListMaker  selectListMaker = new SelectListMaker(null, null, m_language);

        when(mapper.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(mapper);
        selectListMaker.setResult(new Object());
        selectListMaker.m_sCacheName = "TestType";

        selectListMaker.walk(parse("test"));
        assertThat(selectListMaker.getResult(), is((Object) extractor));
        verify(mapper).realize("TestType", AbstractExtractor.VALUE, "test");
        }

    @Test
    public void shouldAcceptPath()
            throws Exception
        {
        ExtractorBuilder mapper          = mock(ExtractorBuilder.class);
        ValueExtractor   extractor       = new ReflectionExtractor("getTest");
        NodeTerm         term            = (NodeTerm) parse("foo.bar");
        SelectListMaker  selectListMaker = new SelectListMaker(null, null, m_language);

        when(mapper.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(mapper);
        selectListMaker.setResult(new Object());
        selectListMaker.m_sCacheName = "MyType";
        selectListMaker.acceptPath(term);

        assertThat(selectListMaker.getResult(), is((Object) extractor));
        verify(mapper).realize("MyType", AbstractExtractor.VALUE, "foo.bar");
        }

    @Test
    public void shouldAcceptPathBeginningWithKeyFunction()
            throws Exception
        {
        ExtractorBuilder mapper          = mock(ExtractorBuilder.class);
        ValueExtractor   extractor       = new PofExtractor(null, new SimplePofPath(1), PofExtractor.KEY);
        NodeTerm         term            = (NodeTerm) parse("key().foo.bar");
        SelectListMaker  selectListMaker = new SelectListMaker(null, null, m_language);

        when(mapper.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(mapper);
        selectListMaker.setResult(new Object());
        selectListMaker.m_sCacheName = "MyKeyType";
        selectListMaker.acceptPath(term);

        assertThat(selectListMaker.getResult(), is((Object) extractor));
        verify(mapper).realize("MyKeyType", AbstractExtractor.KEY, "foo.bar");
        }

    @Test
    public void shouldAcceptPathInsideKeyFunction()
            throws Exception
        {
        ExtractorBuilder mapper          = mock(ExtractorBuilder.class);
        ValueExtractor   extractor       = new PofExtractor(null, new SimplePofPath(1), PofExtractor.KEY);
        NodeTerm         term            = (NodeTerm) parse("key(foo.bar)");
        SelectListMaker  selectListMaker = new SelectListMaker(null, null, m_language);

        when(mapper.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(mapper);
        selectListMaker.setResult(new Object());
        selectListMaker.m_sCacheName = "MyKeyType";
        selectListMaker.walk(term);

        assertThat(selectListMaker.getResult(), is((Object) extractor));
        verify(mapper).realize("MyKeyType", AbstractExtractor.VALUE, "foo.bar");
        }

    @Test
    public void shouldAcceptNestedCallWithParameters()
            throws Exception
        {
        Term            term            = parse("foo.test(1,2,3)");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);
        ValueExtractor  expected        = new ChainedExtractor(new ReflectionExtractor("getFoo"),
                                              new ReflectionExtractor("test", new Object[] {1,
                2, 3}));

        selectListMaker.m_sCacheName = "dist-test";
        selectListMaker.acceptPath((NodeTerm) term);
        assertThat(selectListMaker.getResult(), is((Object) expected));
        }

    @Test
    public void shouldAcceptMappedNestedCallWithParameters()
            throws Exception
        {
        ExtractorBuilder mapper          = mock(ExtractorBuilder.class);
        ValueExtractor   extractor       = new ReflectionExtractor("getTest");
        Term             term            = parse("foo.test(1,2,3)");
        SelectListMaker  selectListMaker = new SelectListMaker(null, null, m_language);
        ValueExtractor   expected        = new ChainedExtractor(extractor,
                                               new ReflectionExtractor("test", new Object[] {1,
                2, 3}));

        when(mapper.realize(anyString(), anyInt(), anyString())).thenReturn(extractor);

        m_language.setExtractorBuilder(mapper);

        selectListMaker.m_sCacheName = "MyType";
        selectListMaker.acceptPath((NodeTerm) term);
        assertThat(selectListMaker.getResult(), is((Object) expected));
        verify(mapper).realize("MyType", AbstractExtractor.VALUE, "foo");
        }

    @Test
    public void shouldAcceptNestedCallWithParametersInsidePath()
            throws Exception
        {
        Term            term            = parse("foo.test(1,2,3).bar");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);
        ValueExtractor  expected        = new ChainedExtractor(new ValueExtractor[] {new ReflectionExtractor("getFoo"),
                new ReflectionExtractor("test", new Object[] {1, 2, 3}), new ReflectionExtractor("getBar")});

        selectListMaker.walk(term);
        assertThat(selectListMaker.getResult(), is((Object) expected));
        }

    @Test
    public void shouldAcceptMappedNestedCallWithParametersInsidePath()
            throws Exception
        {
        ExtractorBuilder mapper          = mock(ExtractorBuilder.class);
        ValueExtractor   extractor1      = new ReflectionExtractor("mapped1");
        ValueExtractor   extractor2      = new ReflectionExtractor("mapped2");
        Term             term            = parse("foo1.foo2.test(1,2,3).bar");
        SelectListMaker  selectListMaker = new SelectListMaker(null, null, m_language);
        ValueExtractor   expected        = new ChainedExtractor(new ValueExtractor[] {extractor1,
                new ReflectionExtractor("test", new Object[] {1, 2, 3}), extractor2});

        when(mapper.realize(nullable(String.class), anyInt(), eq("foo1.foo2"))).thenReturn(extractor1);
        when(mapper.realize(nullable(String.class), anyInt(), eq("bar"))).thenReturn(extractor2);

        m_language.setExtractorBuilder(mapper);

        selectListMaker.walk(term);
        verify(mapper, atMost(1)).realize("dist-test", AbstractExtractor.VALUE, "foo1.foo2");
        verify(mapper, atMost(1)).realize("dist-test", AbstractExtractor.VALUE, "bar");
        assertThat(selectListMaker.getResult(), is((Object) expected));
        }

    @Test
    public void shouldAcceptNestedCallNoParameters()
            throws Exception
        {
        Term            term            = parse("foo.test()");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);
        ValueExtractor  expected        = new ChainedExtractor(new ReflectionExtractor("getFoo"),
                                              new ReflectionExtractor("test"));

        selectListMaker.walk(term);
        assertThat(selectListMaker.getResult(), is((Object) expected));
        }

    @Test
    public void shouldParseMaxFunction()
            throws Exception
        {
        assertAggregator("max(f2)", DoubleMax.class, "getF2");
        }

    @Test
    public void shouldParseMinFunction()
            throws Exception
        {
        assertAggregator("min(f2)", DoubleMin.class, "getF2");
        }

    @Test
    public void shouldParseSumFunction()
            throws Exception
        {
        assertAggregator("sum(f2)", DoubleSum.class, "getF2");
        }

    @Test
    public void shouldParseAverageFunction()
            throws Exception
        {
        assertAggregator("avg(f2)", DoubleAverage.class, "getF2");
        }

    @Test
    public void shouldParseBigDecimalMaxFunction()
            throws Exception
        {
        assertAggregator("bd_max(f2)", BigDecimalMax.class, "getF2");
        }

    @Test
    public void shouldParseBigDecimalMinFunction()
            throws Exception
        {
        assertAggregator("bd_min(f2)", BigDecimalMin.class, "getF2");
        }

    @Test
    public void shouldParseBigDecimalSumFunction()
            throws Exception
        {
        assertAggregator("bd_sum(f2)", BigDecimalSum.class, "getF2");
        }

    @Test
    public void shouldParseBigDecimalAverageFunction()
            throws Exception
        {
        assertAggregator("bd_avg(f2)", BigDecimalAverage.class, "getF2");
        }

    @Test
    public void shouldParseLongMaxFunction()
            throws Exception
        {
        assertAggregator("long_max(f2)", LongMax.class, "getF2");
        }

    @Test
    public void shouldParseLongMinFunction()
            throws Exception
        {
        assertAggregator("long_min(f2)", LongMin.class, "getF2");
        }

    @Test
    public void shouldParseLongSumFunction()
            throws Exception
        {
        assertAggregator("long_sum(f2)", LongSum.class, "getF2");
        }

    @Test
    public void shouldCountFunction()
            throws Exception
        {
        assertAggregator("count()", Count.class, null);
        }

    @Test
    public void shouldParseValueFunction()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("value()");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(), is(instanceOf(IdentityExtractor.class)));
        }

    @Test
    public void shouldParseKeyFunction()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("key()");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(), is((Object) new KeyExtractor(IdentityExtractor.INSTANCE)));
        }

    @Test
    public void shouldParseKeyFieldFunction()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("key(foo)");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(),
                   is((Object) new UniversalExtractor("getFoo()", null, ReflectionExtractor.KEY)));
        }

    @Test
    public void shouldParseKeyChainedFieldFunction()
            throws Exception
        {
        ChainedExtractor expected = new ChainedExtractor(new ValueExtractor[] {
                                        new UniversalExtractor("f1", null, UniversalExtractor.KEY),
                                        new UniversalExtractor("f2")});
        NodeTerm        node            = (NodeTerm) parse("key(f1.f2)");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(), is((Object) expected));
        }

    @Test
    public void shouldParseValueMethodCall()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("myMethod()");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(), is((Object) new ReflectionExtractor("myMethod")));
        }

    @Test
    public void shouldParseCustomFunctionWithNoArgs()
            throws Exception
        {
        InvocableMap.EntryAggregator agg     = mock(InvocableMap.EntryAggregator.class, "MyAgg");
        ParameterizedBuilder         builder = mock(ParameterizedBuilder.class);

        when(builder.realize(any(ParameterResolver.class), nullable(ClassLoader.class),
                             any(ParameterList.class))).thenReturn(agg);

        m_language.addFunction("myFunction", builder);

        NodeTerm        node            = (NodeTerm) parse("myFunction()");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(), is((Object) agg));
        }

    @Test
    public void shouldParseCustomFunctionWithArgs()
            throws Exception
        {
        InvocableMap.EntryAggregator agg     = mock(InvocableMap.EntryAggregator.class, "MyAgg");
        ParameterizedBuilder         builder = mock(ParameterizedBuilder.class);

        when(builder.realize(any(ParameterResolver.class), nullable(ClassLoader.class),
                             any(ParameterList.class))).thenReturn(agg);

        m_language.addFunction("myFunction", builder);

        NodeTerm        node            = (NodeTerm) parse("myFunction(foo)");
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(), is((Object) agg));
        }

    @Test
    public void shouldReturnDistinctValues()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("select foo, bar from myCache");
        NodeTerm        fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);
        ValueExtractor  expected        = new MultiExtractor("getFoo,getBar");

        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);
        selectListMaker.getResultsAsEntryAggregator();

        DistinctValues result = selectListMaker.getDistinctValues();

        assertThat(result, is(notNullValue()));
        assertThat(result.getValueExtractor(), is(expected));
        }

    @Test
    public void shouldReturnDistinctValuesWithKeyFunction()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("select key(foo) from myCache");
        NodeTerm        fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);
        ValueExtractor  expected        = new UniversalExtractor("getFoo()", null, ReflectionExtractor.KEY);

        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);
        selectListMaker.getResultsAsEntryAggregator();

        DistinctValues result = selectListMaker.getDistinctValues();

        assertThat(result, is(notNullValue()));
        assertEquals(result.getValueExtractor(), expected);
        }

    @Test
    public void shouldNotReturnDistinctValuesWhenSelectContainsAggregatorFunction()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("select foo, bar, count() from myCache");
        NodeTerm        fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);

        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);

        DistinctValues result = selectListMaker.getDistinctValues();

        assertThat(result, is(nullValue()));
        }

    @Test
    public void shouldBeAnAggregation()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("select max(foo) from myCache");
        NodeTerm        fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);

        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);
        assertThat(selectListMaker.isAggregation(), is(true));
        }

    @Test
    public void shouldNotBeAnAggregation()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("select foo, max(bar) from myCache group by foo");
        NodeTerm        fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);

        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);
        assertThat(selectListMaker.isAggregation(), is(false));
        }

    @Test
    public void shouldGetAggregator()
            throws Exception
        {
        NodeTerm                     node            = (NodeTerm) parse("select max(foo) from myCache");
        NodeTerm                     fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);
        InvocableMap.EntryAggregator expected        = new DoubleMax("getFoo");

        SelectListMaker              selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);

        InvocableMap.EntryAggregator aggregator = selectListMaker.getResultsAsEntryAggregator();

        assertThat(aggregator, is(expected));
        }

    @Test
    public void shouldGetReductionAggregator()
            throws Exception
        {
        NodeTerm                     node            = (NodeTerm) parse("select foo, bar from myCache");
        NodeTerm                     fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);
        InvocableMap.EntryAggregator expected        = new ReducerAggregator(new MultiExtractor("getFoo,getBar"));

        SelectListMaker              selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);

        InvocableMap.EntryAggregator aggregator = selectListMaker.getResultsAsReduction();

        assertThat(aggregator, is(expected));
        }

    @Test
    public void shouldGetValueExtractor()
            throws Exception
        {
        NodeTerm        node            = (NodeTerm) parse("select foo, bar from myCache");
        NodeTerm        fields          = (NodeTerm) node.findChild(OPToken.FIELD_LIST);
        ValueExtractor  expected        = new MultiExtractor("getFoo,getBar");

        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);

        ValueExtractor result = selectListMaker.getResultsAsValueExtractor();

        assertThat(result, is(expected));
        }

    @Test
    public void shouldGetEntryProcessor()
            throws Exception
        {
        NodeTerm                    node     = (NodeTerm) parse("select foo, bar from myCache");
        NodeTerm                    fields   = (NodeTerm) node.findChild(OPToken.FIELD_LIST);
        InvocableMap.EntryProcessor expected = new CompositeProcessor(new InvocableMap.EntryProcessor[] {
                                                   new ExtractorProcessor("getFoo"),
                new ExtractorProcessor("getBar")});

        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.makeSelectsForCache("myCache", fields);

        InvocableMap.EntryProcessor result = selectListMaker.getResultsAsEntryProcessor();

        assertThat(result, is(expected));
        }

    private void assertAggregator(String query, Class expectedType, String expectedExtractorMethod)
        {
        NodeTerm        node            = (NodeTerm) parse(query);
        SelectListMaker selectListMaker = new SelectListMaker(null, null, m_language);

        selectListMaker.acceptCall(node.termAt(1).getFunctor(), (NodeTerm) node.termAt(1));
        assertThat(selectListMaker.getResult(), is(instanceOf(expectedType)));

        if (expectedExtractorMethod != null)
            {
            AbstractAggregator aggregator = (AbstractAggregator) selectListMaker.getResult();
            ReflectionExtractor expectedExtractor = new ReflectionExtractor(expectedExtractorMethod);

            assertThat(aggregator.getValueExtractor(), is((Object) expectedExtractor));
            }
        }

    private Term parse(String query)
        {
        OPParser p = new OPParser(new StringReader(query), m_language.sqlTokenTable(), m_language.getOperators());

        return p.parse();
        }

    /**
     * The CoherenceQueryLanguage used by these tests
     */
    private final CoherenceQueryLanguage m_language = new CoherenceQueryLanguage();
    }
