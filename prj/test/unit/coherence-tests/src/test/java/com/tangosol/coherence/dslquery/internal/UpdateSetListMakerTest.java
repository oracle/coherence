/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;
import com.tangosol.util.Extractors;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.CompositeUpdater;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.ReflectionUpdater;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.extractor.UniversalUpdater;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.NumberMultiplier;
import com.tangosol.util.processor.UpdaterProcessor;
import data.pof.Address;
import data.pof.Person;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static com.oracle.coherence.testing.util.CollectionUtils.mapWith;
import static com.oracle.coherence.testing.util.CollectionUtils.pair;

/**
 * @author jk  2013.12.04
 */
public class UpdateSetListMakerTest
    {
    @Test
    public void shouldAcceptNullIdentifier()
            throws Exception
        {
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptIdentifier("NuLl");
        assertThat(updateSetListMaker.getResult(), is(nullValue()));
        }

    @Test
    public void shouldAcceptBooleanTrueIdentifier()
            throws Exception
        {
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptIdentifier("TrUe");
        assertThat(updateSetListMaker.getResult(), is((Object) true));
        }

    @Test
    public void shouldAcceptBooleanFalseIdentifier()
            throws Exception
        {
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptIdentifier("FaLsE");
        assertThat(updateSetListMaker.getResult(), is((Object) false));
        }

    @Test
    public void shouldAcceptIdentifier()
            throws Exception
        {
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptIdentifier("test");
        assertThat(updateSetListMaker.getResult(), is((Object) "test"));
        }

    @Test
    public void shouldAcceptList()
            throws Exception
        {
        String             query              = "(1,2,3,4)";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = new Object[] {1, 2, 3, 4};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptList(term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptEmptyList()
            throws Exception
        {
        String             query              = "()";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = new Object[0];

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptList(term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptPath()
            throws Exception
        {
        String             query              = "a.b.c";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = new Object[] {"a", "b", "c"};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptPath(term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptEmptyPath()
            throws Exception
        {
        String             query              = "()";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = new Object[0];

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(new Object());

        updateSetListMaker.acceptPath(term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptKeyCallWithNoArgs()
            throws Exception
        {
        String             query              = "key()";
        NodeTerm           term               = (NodeTerm) parse(query).termAt(1);
        Object             expected           = new KeyExtractor(IdentityExtractor.INSTANCE);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptKeyCallWithWithArgs()
            throws Exception
        {
        String             query              = "key(foo)";
        NodeTerm           term               = (NodeTerm) parse(query).termAt(1);
        Object             expected           = new UniversalExtractor("key()", new Object[] {"foo"});

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptValueCallWithNoArgs()
            throws Exception
        {
        String             query              = "value()";
        NodeTerm           term               = (NodeTerm) parse(query).termAt(1);
        Object             expected           = IdentityExtractor.INSTANCE;

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptExtendedSquareBracketListCall()
            throws Exception
        {
        String             query              = "[1,2,3,4]";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = Arrays.asList(1, 2, 3, 4);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptExtendedMapCall()
            throws Exception
        {
        String             query              =
            "Map(listNode(literal('A'),literal(1)),listNode(literal('B'),literal(2)))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);
        Object             expected           = mapWith(pair("A", 1), pair("B", 2));

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptExtendedSetCall()
            throws Exception
        {
        String             query              = "Set(literal('A'),literal('B'))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), instanceOf(Collection.class));
        assertThat((Collection<String>) updateSetListMaker.getResult(), contains("A", "B"));
        }

    @Test
    public void shouldAcceptExtendedListCall()
            throws Exception
        {
        String             query              = "List(literal('A'),literal('B'))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);
        Object             expected           = Arrays.asList("A", "B");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptExtendedPairCall()
            throws Exception
        {
        String             query              = "1:2";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = new Object[] {1, 2};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptExtendedBagSetCall()
            throws Exception
        {
        String             query              = "{1,2,3}";
        NodeTerm           term               = (NodeTerm) parse(query);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), instanceOf(Collection.class));
        assertThat((Collection<Integer>) updateSetListMaker.getResult(), contains(1, 2, 3));
        }

    @Test
    public void shouldAcceptExtendedBagMapCall()
            throws Exception
        {
        String             query              = "{'A':1,'B':2}";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = mapWith(pair("A", 1), pair("B", 2));

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptExtendedExtractorCall()
            throws Exception
        {
        String             query              = "foo()";
        NodeTerm           term               = (NodeTerm) parse(query).termAt(1);
        Object             expected           = new ReflectionExtractor("foo");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptCall(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldFailIfAcceptNotUnaryOperator()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Unknown unary operator: !");

        NodeTerm           term               = (NodeTerm) parse("!(foo == 123)");
        Object             expected           = new NotFilter(new EqualsFilter("getFoo", 123));

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptUnaryOperator("!", term.termAt(2));
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptNegateUnaryOperator()
            throws Exception
        {
        AtomicTerm         term               = AtomicTerm.createInteger("100");
        Object             expected           = -100;

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptUnaryOperator("-", term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptPlusUnaryOperator()
            throws Exception
        {
        AtomicTerm         term               = AtomicTerm.createInteger("100");
        Object             expected           = 100;

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptUnaryOperator("+", term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptNewUnaryOperator()
            throws Exception
        {
        NodeTerm           term               = (NodeTerm) parse("new java.util.Date(12345L)");
        Object             expected           = new Date(12345);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptUnaryOperator("new", term.termAt(2));
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptNodeThatIsList()
            throws Exception
        {
        String             query              = "[1,2,3,4]";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = Arrays.asList(1, 2, 3, 4);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptNode(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptNodeThatIsPair()
            throws Exception
        {
        String             query              = "1:2";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = new Object[] {1, 2};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptNode(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldAcceptNodeThatIsBagSet()
            throws Exception
        {
        String             query              = "{1,2,3}";
        NodeTerm           term               = (NodeTerm) parse(query);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptNode(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), instanceOf(Collection.class));
        assertThat((Collection<Integer>) updateSetListMaker.getResult(), contains(1, 2, 3));
        }

    @Test
    public void shouldAcceptNodeThatIsBagMap()
            throws Exception
        {
        String             query              = "{'A':1,'B':2}";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = mapWith(pair("A", 1), pair("B", 2));

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptNode(term.getFunctor(), term);
        assertThat(updateSetListMaker.getResult(), is(expected));
        }

    @Test
    public void shouldGetResultAsUpdateProcessor()
            throws Exception
        {
        String             query              = "setList(binaryOperatorNode('==', identifier(foo), literal('bar')))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);
        Object             expected           = new UpdaterProcessor(new UniversalUpdater("foo"), "bar");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.makeSetList(term);

        InvocableMap.EntryProcessor result = updateSetListMaker.getResultAsEntryProcessor();

        assertThat(result, is(expected));
        }

    @Test
    public void shouldGetResultAsCompositeProcessor()
            throws Exception
        {
        String query = "setList(binaryOperatorNode('==', identifier(foo), literal('bar')),"
                       + "binaryOperatorNode('==', identifier(foo2), literal('bar2')))";
        NodeTerm term     = (NodeTerm) Terms.create(query);
        Object   expected = new CompositeProcessor(new InvocableMap.EntryProcessor[] {
                                new UpdaterProcessor(new UniversalUpdater("foo"), "bar"),
                                new UpdaterProcessor(new UniversalUpdater("foo2"), "bar2")});

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.makeSetList(term);

        InvocableMap.EntryProcessor result = updateSetListMaker.getResultAsEntryProcessor();

        assertThat(result, is(expected));
        }

    @Test
    public void shouldGetResults()
            throws Exception
        {
        String query = "setList(binaryOperatorNode('==', identifier(foo), literal('bar')),"
                       + "binaryOperatorNode('==', identifier(foo2), literal('bar2')))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);
        Object             expected           = new Object[] {new UpdaterProcessor(new UniversalUpdater("foo"), "bar"),
                new UpdaterProcessor(new UniversalUpdater("setFoo2()"), "bar2")};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.makeSetList(term);

        Object result = updateSetListMaker.getResults();

        assertThat(result, is(expected));
        }

    @Test
    public void shouldAcceptBinaryEqualsOperator()
            throws Exception
        {
        String             query              = "binaryOperatorNode('==', identifier(foo), literal('bar'))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);
        Object             expected           = new UpdaterProcessor(new UniversalUpdater("foo"), "bar");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptBinaryOperator("==", term.termAt(2), term.termAt(3));
        assertThat(updateSetListMaker.getResult(), is(instanceOf(InvocableMap.EntryProcessor.class)));

        InvocableMap.EntryProcessor result = (InvocableMap.EntryProcessor) updateSetListMaker.getResult();

        assertThat(result, is(expected));
        }

    @Test
    public void shouldAcceptBinaryAdditionOperator()
            throws Exception
        {
        String             query              = "binaryOperatorNode('+', identifier(foo), literal(2000))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);
        Object             expected           = new NumberIncrementor("foo", 2000, false);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptBinaryOperator("+", term.termAt(2), term.termAt(3));
        assertThat(updateSetListMaker.getResult(), is(instanceOf(NumberIncrementor.class)));

        NumberIncrementor result = (NumberIncrementor) updateSetListMaker.getResult();

        assertThat(result.toString(), is(expected.toString()));
        }

    @Test
    public void shouldAcceptBinaryMultiplicationOperator()
            throws Exception
        {
        String             query              = "binaryOperatorNode('*', identifier(foo), literal(1000))";
        NodeTerm           term               = (NodeTerm) Terms.create(query);
        Object             expected           = new NumberMultiplier("foo", 1000, false);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setResult(null);

        updateSetListMaker.acceptBinaryOperator("*", term.termAt(2), term.termAt(3));
        assertThat(updateSetListMaker.getResult(), is(instanceOf(NumberMultiplier.class)));

        NumberMultiplier result = (NumberMultiplier) updateSetListMaker.getResult();

        assertThat(result.toString(), is(expected.toString()));
        }

    @Test
    public void shouldMakeListLiteralFromEmptyArray()
            throws Exception
        {
        Object[]           array              = new Object[0];
        Object             expected           = Collections.emptyList();

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeListLiteral(array), is(expected));
        }

    @Test
    public void shouldMakeListLiteralFromArray()
            throws Exception
        {
        Object[]           array              = new Object[] {"A", 1, "B", 2};
        Object             expected           = Arrays.asList(array);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeListLiteral(array), is(expected));
        }

    @Test
    public void shouldMakeMapLiteralFromEmptyArray()
            throws Exception
        {
        Object[]           array              = new Object[0];
        Object             expected           = Collections.emptyMap();

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeMapLiteral(array), is(expected));
        }

    @Test
    public void shouldMakeMapLiteralFromArray()
            throws Exception
        {
        Object[]           array              = new Object[] {new Object[] {"A", 1}, new Object[] {"B", 2}};
        Object             expected           = mapWith(pair("A", 1), pair("B", 2));

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeMapLiteral(array), is(expected));
        }

    @Test
    public void shouldFailToMakeMapLiteralFromTooBigArray()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Incorrect for argument to literal Map :[B, 2, Oops!]");

        Object[]           array              = new Object[] {new Object[] {"A", 1}, new Object[] {"B", 2, "Oops!"}};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makeMapLiteral(array);
        }

    @Test
    public void shouldFailToMakeMapLiteralFromTooSmallArray()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Incorrect for argument to literal Map :[Oops!]");

        Object[]           array              = new Object[] {new Object[] {"A", 1}, new Object[] {"Oops!"}};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makeMapLiteral(array);
        }

    @Test
    public void shouldFailToMakeMapLiteralFromNullArray()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Incorrect for argument to literal Map :null");

        Object[]           array              = new Object[] {new Object[] {"A", 1}, null};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makeMapLiteral(array);
        }

    @Test
    public void shouldMakeObject()
            throws Exception
        {
        String             query              = "{'A':1,'B':2}";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = mapWith(pair("A", 1), pair("B", 2));

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObject(term), is(expected));
        }

    @Test
    public void shouldMakeObjectFromStaticCall()
            throws Exception
        {
        String             query              = "java.util.Calendar.getInstance()";
        NodeTerm           term               = (NodeTerm) parse(query);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObject(term), is(instanceOf(Calendar.class)));
        }

    @Test
    public void shouldMakeObjectForKeyUsingLiteral()
            throws Exception
        {
        String             query              = "9876L";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = 9876L;

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObjectForKey(term, null), is(expected));
        }

    @Test
    public void shouldMakeObjectForKeyUsingConstructor()
            throws Exception
        {
        String             query              = "new java.util.Date(12345L)";
        NodeTerm           term               = (NodeTerm) parse(query);
        Object             expected           = new Date(12345L);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObjectForKey(term, null), is(expected));
        }

    @Test
    public void shouldMakeObjectForKeyUsingMethodCall()
            throws Exception
        {
        String             query              = "getName()";
        NodeTerm           term               = (NodeTerm) parse(query);
        Person             person             = new Person("JK", new Date());
        Object             expected           = person.getName();

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObjectForKey(term, person), is(expected));
        }

    @Test
    public void shouldMakeObjectForKeyUsingNestedMethodCall()
            throws Exception
        {
        String   query    = "getAddress().getCity()";
        NodeTerm term     = (NodeTerm) parse(query);
        Person   person   = new Person("JK", new Date());
        Address  address  = new Address("7 Coral Apts", "London", "", "E16 1AQ");
        Object   expected = address.getCity();

        person.setAddress(address);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObjectForKey(term, person), is(expected));
        }

    @Test
    public void shouldMakeObjectForKeyUsingIdentifier()
            throws Exception
        {
        String             query              = "name";
        NodeTerm           term               = (NodeTerm) parse(query);
        Person             person             = new Person("JK", new Date());
        Object             expected           = person.getName();

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObjectForKey(term, person), is(expected));
        }

    @Test
    public void shouldMakeObjectForKeyUsingNestedIdentifier()
            throws Exception
        {
        String   query    = "address.city";
        NodeTerm term     = (NodeTerm) parse(query);
        Person   person   = new Person("JK", new Date());
        Address  address  = new Address("7 Coral Apts", "London", "", "E16 1AQ");
        Object   expected = address.getCity();

        person.setAddress(address);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObjectForKey(term, person), is(expected));
        }

    @Test
    public void shouldMakeObjectForKeyUsingMixedMethodAndNestedIdentifier()
            throws Exception
        {
        String   query    = "getAddress().city";
        NodeTerm term     = (NodeTerm) parse(query);
        Person   person   = new Person("JK", new Date());
        Address  address  = new Address("7 Coral Apts", "London", "", "E16 1AQ");
        Object   expected = address.getCity();

        person.setAddress(address);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.setExtendedLanguage(true);
        updateSetListMaker.setResult(null);

        assertThat(updateSetListMaker.makeObjectForKey(term, person), is(expected));
        }

    @Test
    public void shouldMakePairLiteral()
            throws Exception
        {
        Object[]           input              = new Object[] {"A", "B"};
        Object             expected           = new Object[] {"A", "B"};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makePairLiteral(input), is(expected));
        }

    @Test
    public void shouldFailToMakePairLiteralForNullInput()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Pairs must be length 2 instead of length 0");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makePairLiteral(null);
        }

    @Test
    public void shouldFailToMakePairLiteralForZeroLengthInput()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Pairs must be length 2 instead of length 0");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makePairLiteral(new Object[0]);
        }

    @Test
    public void shouldFailToMakePairLiteralForTooSmallInput()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Pairs must be length 2 instead of length 1");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makePairLiteral(new Object[] {"Oops!"});
        }

    @Test
    public void shouldFailToMakePairLiteralForTooBigInput()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Pairs must be length 2 instead of length 3");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makePairLiteral(new Object[] {"A", "B", "Oops!"});
        }

    @Test
    public void shouldMakePathStringFromUnrecognisedObject()
            throws Exception
        {
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makePathString(new Object()), is(nullValue()));
        }

    @Test
    public void shouldMakePathStringFromIdentityExtractor()
            throws Exception
        {
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makePathString(IdentityExtractor.INSTANCE), is(nullValue()));
        }

    @Test
    public void shouldMakePathStringFromString()
            throws Exception
        {
        Object             input              = "test";
        String             expected           = "test";
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makePathString(input), is(expected));
        }

    @Test
    public void shouldMakePathStringFromUniversalExtractor()
            throws Exception
        {
        Object             input              = new UniversalExtractor("foo");
        String             expected           = "getFoo";
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makePathString(input), is(expected));
        }

    @Test
    public void shouldMakePathStringFromArray()
            throws Exception
        {
        Object             input              = new Object[] {"foo", new UniversalExtractor("bar()"),
                IdentityExtractor.INSTANCE};
        String             expected           = "foo.bar.null";
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makePathString(input), is(expected));
        }

    @Test
    public void shouldMakeValueExtractorFromString()
            throws Exception
        {
        Object             input              = "foo";
        Object             expected           = new ReflectionExtractor("getFoo");
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeValueExtractor(input), is(expected));
        }

    @Test
    public void shouldMakeValueExtractorFromValueExtractor()
            throws Exception
        {
        Object             input              = new PofExtractor();
        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeValueExtractor(input), is(sameInstance(input)));
        }

    @Test
    public void shouldMakeValueExtractorFromArray()
            throws Exception
        {
        Object input    = new Object[] {"foo", new PofExtractor(null, 1)};
        Object expected = new ChainedExtractor(new ValueExtractor[] {new ReflectionExtractor("getFoo"),
                new PofExtractor(null, 1)});

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeValueExtractor(input), is(expected));
        }

    @Test
    public void shouldFailToMakeValueExtractorForUnknownValue()
            throws Exception
        {
        Object input = new Object();

        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Unable to determine extractor for: " + input);

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        updateSetListMaker.makeValueExtractor(input);
        }

    @Test
    public void shouldMakeValueUpdaterFromIdentityExtractor()
            throws Exception
        {
        Object             input              = IdentityExtractor.INSTANCE;
        Object             expected           = null;

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeValueUpdater(input), is(expected));
        }

    @Test
    public void shouldMakeValueUpdaterFromString()
            throws Exception
        {
        Object             input              = "foo";
        Object             expected           = new UniversalUpdater("foo");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeValueUpdater(input), is(expected));
        }

    @Test
    public void shouldMakeValueUpdaterFromUniversalExtractor()
            throws Exception
        {
        Object             input              = new UniversalExtractor("foo()", new Object[] {1});
        Object             expected           = new UniversalUpdater("foo");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeValueUpdater(input), is(expected));
        }

    @Test
    public void shouldMakeValueUpdaterFromValueUpdater()
            throws Exception
        {
        Object             input              = new UniversalUpdater("foo");

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);

        assertThat(updateSetListMaker.makeValueUpdater(input), is(sameInstance(input)));
        }

    @Test
    public void shouldMakeValueUpdaterFromArrayEndingInString()
            throws Exception
        {
        Object             input              = new Object[] {"bar", new UniversalExtractor("ram"), "ewe"};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);
        ValueUpdater       actual             = updateSetListMaker.makeValueUpdater(input);

        assertThat(actual, is(instanceOf(CompositeUpdater.class)));

        CompositeUpdater cu = (CompositeUpdater) actual;

        assertThat(cu.getExtractor(), is((Object) Extractors.chained("bar.ram")));
        assertThat(cu.getUpdater(), is((Object) new UniversalUpdater("ewe")));
        }

    @Test
    public void shouldMakeValueUpdaterFromArrayEndingInUniversalExtractor()
            throws Exception
        {
        Object             input              = new Object[] {"bar", new UniversalExtractor("ram"),
                new UniversalExtractor("ewe()")};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);
        ValueUpdater       actual             = updateSetListMaker.makeValueUpdater(input);

        assertThat(actual, is(instanceOf(CompositeUpdater.class)));

        CompositeUpdater cu = (CompositeUpdater) actual;

        assertThat(cu.getExtractor(), is((Object) Extractors.chained("bar.ram")));
        assertThat(cu.getUpdater(), is((Object) new UniversalUpdater("ewe")));
        }

    @Test
    public void shouldMakeValueUpdaterFromArrayEndingInValueUpdater()
            throws Exception
        {
        Object             input              = new Object[] {"bar", new ReflectionExtractor("ram"),
                new ReflectionUpdater("ewe")};

        UpdateSetListMaker updateSetListMaker = new UpdateSetListMaker(null, null, m_language);
        ValueUpdater       actual             = updateSetListMaker.makeValueUpdater(input);

        assertThat(actual, is(instanceOf(CompositeUpdater.class)));

        CompositeUpdater cu = (CompositeUpdater) actual;

        assertThat(cu.getExtractor(), is((Object) new ChainedExtractor("getBar.ram")));
        assertThat(cu.getUpdater(), is((Object) new ReflectionUpdater("ewe")));
        }

    private Term parse(String query)
        {
        OPParser parser = new OPParser(query, m_language.extendedSqlTokenTable(), m_language.getOperators());

        return parser.parse();
        }

    @Rule
    public ExpectedException       expectedEx = ExpectedException.none();
    private CoherenceQueryLanguage m_language = new CoherenceQueryLanguage();
    }
