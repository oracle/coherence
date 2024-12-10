/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.TokenTable;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.TermWalker;
import com.tangosol.coherence.dsltools.termtrees.Terms;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.EqualsFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.31
 */
public class BaseOperatorTest
    {
    @Test
    public void shouldHaveCorrectSymbol()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("?");

        assertThat(operator.getSymbol(), is("?"));
        }

    @Test
    public void shouldHaveNoAliases()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("?");

        assertThat(operator.getAliases(), is(new String[0]));
        }

    @Test
    public void shouldHaveCorrectAliases()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("?", "1", "2");

        assertThat(operator.getAliases(), is(new String[] {"1", "2"}));
        }

    @Test
    public void shouldunmodifiableSet()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("!");
        Set<String>  result   = operator.unmodifiableSet("Test");

        assertThat(result, contains("Test"));
        }

    @Test
    public void shouldunmodifiableSetFromArray()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("!");
        Set<String>  result   = operator.unmodifiableSet(new String[] {"1", "2"});

        assertThat(result, containsInAnyOrder("1", "2"));
        }

    @Test
    public void shouldunmodifiableSetFromCollection()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("!");
        Set<String>  result   = operator.unmodifiableSet(Arrays.asList("3", "4"));

        assertThat(result, containsInAnyOrder("3", "4"));
        }

    @Test
    public void shouldunmodifiableSetFromSet()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("!");
        Set<String>  original = new TreeSet<String>();
        Set<String>  result   = operator.unmodifiableSet(original);

        assertThat(result, is(sameInstance(original)));
        }

    @Test
    public void shouldCreateConditionalBaseOperator()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("Test", true);

        assertThat(operator.isConditional(), is(true));
        }

    @Test
    public void shouldCreateNonConditionalBaseOperator()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("Test", false);

        assertThat(operator.isConditional(), is(false));
        }

    @Test
    public void shouldThrowExceptionCallingMakeFilter()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("Test", true);

        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage(String.format("Unsupported binary operator (%s)", operator.getSymbol()));

        operator.makeFilter(null, null);
        }

    @Test
    public void shouldThrowExceptionCallingMakeExtractor()
            throws Exception
        {
        BaseOperator operator = new BaseOperatorStubOne("Test", true);

        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage(String.format("Unsupported binary operator (%s)", operator.getSymbol()));

        operator.makeExtractor(null, null);
        }

    @Test
    public void shouldMakeExtractor()
            throws Exception
        {
        Term                leftTerm  = Terms.newTerm("identifier", AtomicTerm.createString("foo"));
        Term                rightTerm = AtomicTerm.createString("Test");
        Object              left      = new Object();
        Object              right     = new Object();
        TermWalker          walker    = mock(TermWalker.class);
        ValueExtractor      expected  = new ReflectionExtractor("getFoo");
        BaseOperatorStubTwo operator  = new BaseOperatorStubTwo(expected);

        when(walker.walk(leftTerm)).thenReturn(left);
        when(walker.walk(rightTerm)).thenReturn(right);

        ValueExtractor result = operator.makeExtractor(leftTerm, rightTerm, walker);

        assertThat(result, is(sameInstance(expected)));
        assertThat(operator.m_left, is(sameInstance(left)));
        assertThat(operator.m_right, is(sameInstance(right)));
        }

    @Test
    public void shouldMakeFilter()
            throws Exception
        {
        Term                leftTerm  = Terms.newTerm("identifier", AtomicTerm.createString("foo"));
        Term                rightTerm = AtomicTerm.createString("Test");
        Object              left      = new Object();
        Object              right     = new Object();
        TermWalker          walker    = mock(TermWalker.class);
        Filter              expected  = new EqualsFilter("getFoo", "Test");
        BaseOperatorStubTwo operator  = new BaseOperatorStubTwo(expected);

        when(walker.walk(leftTerm)).thenReturn(left);
        when(walker.walk(rightTerm)).thenReturn(right);

        Filter result = operator.makeFilter(leftTerm, rightTerm, walker);

        assertThat(result, is(sameInstance(expected)));
        assertThat(operator.m_left, is(sameInstance(left)));
        assertThat(operator.m_right, is(sameInstance(right)));
        }

    protected Term parse(String query)
        {
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();
        OPParser               parser   = new OPParser(query, language.filtersTokenTable(), language.getOperators());

        return parser.parse();
        }

    public static class BaseOperatorStubOne
            extends BaseOperator
        {
        public BaseOperatorStubOne(String sSymbol, String... aAliases)
            {
            super(sSymbol, true, aAliases);
            }

        public BaseOperatorStubOne(String sSymbol, boolean conditional, String... aAliases)
            {
            super(sSymbol, conditional, aAliases);
            }

        @Override
        public void addToTokenTable(TokenTable tokenTable)
            {
            }
        }

    public static class BaseOperatorStubTwo
            extends BaseOperator
        {
        public BaseOperatorStubTwo(Object result)
            {
            super("", false);
            m_result = result;
            }

        @Override
        public Filter makeFilter(Object oLeft, Object oRight)
            {
            m_left  = oLeft;
            m_right = oRight;

            return (Filter) m_result;
            }

        @Override
        public ValueExtractor makeExtractor(Object oLeft, Object oRight)
            {
            m_left  = oLeft;
            m_right = oRight;

            return (ValueExtractor) m_result;
            }

        @Override
        public void addToTokenTable(TokenTable tokenTable)
            {
            }

        protected Object m_left;
        protected Object m_right;
        protected Object m_result;
        }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
