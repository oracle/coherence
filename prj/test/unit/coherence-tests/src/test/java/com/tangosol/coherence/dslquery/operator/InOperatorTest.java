/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dslquery.FilterBuilder;
import com.tangosol.coherence.dsltools.precedence.TokenTable;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.InKeySetFilter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static com.oracle.coherence.testing.util.CollectionUtils.setWith;

/**
 * @author jk  2013.12.31
 */
public class InOperatorTest
        extends BaseOperatorTest
    {
    @Test
    public void shouldHaveCorrectSymbol()
            throws Exception
        {
        InOperator op = new InOperator();

        assertThat(op.getSymbol(), is("in"));
        }

    @Test
    public void shouldHaveCorrectAliases()
            throws Exception
        {
        InOperator op = new InOperator();

        assertThat(op.getAliases().length, is(0));
        }

    @Test
    public void shouldAddToTokenTable()
            throws Exception
        {
        TokenTable tokens = new TokenTable();
        InOperator op     = new InOperator();

        op.addToTokenTable(tokens);
        assertThat(tokens.lookup(op.getSymbol()), is(notNullValue()));

        for (String alias : op.getAliases())
            {
            assertThat("Token missing for alias " + alias, tokens.lookup(alias), is(notNullValue()));
            }
        }

    @Test
    public void shouldCreateFilterWhenRightTermIsArray()
            throws Exception
        {
        InOperator op       = new InOperator();
        ValueExtractor left     = new ReflectionExtractor("getFoo");
        Object[]       right    = new Object[] {100, 200, 300};
        Filter         result   = op.makeFilter(left, right);
        Filter         expected = new InFilter(left, setWith(100, 200, 300));

        assertThat(result, is(expected));
        }

    @Test
    public void shouldCreateFilterWhenRightTermIsSet()
            throws Exception
        {
        InOperator op       = new InOperator();
        ValueExtractor left     = new ReflectionExtractor("getFoo");
        Object         right    = setWith(100, 200, 300);
        Filter         result   = op.makeFilter(left, right);
        Filter         expected = new InFilter(left, setWith(100, 200, 300));

        assertThat(result, is(expected));
        }

    @Test
    public void shouldCreateFilterWhenRightTermIsObject()
            throws Exception
        {
        InOperator op       = new InOperator();
        ValueExtractor left     = new ReflectionExtractor("getFoo");
        Object         right    = new Object();
        Filter         result   = op.makeFilter(left, right);
        Filter         expected = new InFilter(left, setWith(right));

        assertThat(result, is(expected));
        }

    @Test
    public void shouldCreateInKeySetFilterWhenRightTermIsArray()
            throws Exception
        {
        InOperator op     = new InOperator();
        Filter     left   = new EqualsFilter("getFoo", 1);
        Object[]   right  = new Object[] {100, 200, 300};
        Filter     result = op.makeFilter(left, right);

        assertThat(result, is(instanceOf(InKeySetFilter.class)));
        assertThat(((InKeySetFilter) result).getFilter(), is(left));
        }

    @Test
    public void shouldCreateInKeySetFilterWhenRightTermIsSet()
            throws Exception
        {
        InOperator op     = new InOperator();
        Filter     left   = new EqualsFilter("getFoo", 1);
        Object     right  = setWith(100, 200, 300);
        Filter     result = op.makeFilter(left, right);

        assertThat(result, is(instanceOf(InKeySetFilter.class)));
        assertThat(((InKeySetFilter) result).getFilter(), is(left));
        }

    @Test
    public void shouldCreateInKeySetFilterWhenRightTermIsObject()
            throws Exception
        {
        InOperator op     = new InOperator();
        Filter     left   = new EqualsFilter("getFoo", 1);
        Object     right  = new Object();
        Filter     result = op.makeFilter(left, right);

        assertThat(result, is(instanceOf(InKeySetFilter.class)));
        assertThat(((InKeySetFilter) result).getFilter(), is(left));
        }

    @Test
    public void shouldBuildFilterFromTerms()
            throws Exception
        {
        Term       term     = parse("foo in (100,200,300)");
        InOperator op       = new InOperator();
        Filter     result   = op.makeFilter(term.termAt(2), term.termAt(3), new FilterBuilder());
        Filter     expected = new InFilter("getFoo", setWith(100, 200, 300));

        assertThat(result, is(expected));
        }
    }
