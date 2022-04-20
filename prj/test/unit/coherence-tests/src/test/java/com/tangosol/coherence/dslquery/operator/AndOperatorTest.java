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

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;

/**
 * @author jk  2013.12.31
 */
public class AndOperatorTest
        extends BaseOperatorTest
    {
    @Test
    public void shouldHaveCorrectSymbol()
            throws Exception
        {
        AndOperator op = new AndOperator();

        assertThat(op.getSymbol(), is("&&"));
        }

    @Test
    public void shouldHaveCorrectAliases()
            throws Exception
        {
        AndOperator op = new AndOperator();

        assertThat(op.getAliases(), is(new String[] {"and"}));
        }

    @Test
    public void shouldAddToTokenTable()
            throws Exception
        {
        TokenTable  tokens = new TokenTable();
        AndOperator op     = new AndOperator();

        op.addToTokenTable(tokens);
        assertThat(tokens.lookup(op.getSymbol()), is(notNullValue()));

        for (String alias : op.getAliases())
            {
            assertThat("Token missing for alias " + alias, tokens.lookup(alias), is(notNullValue()));
            }
        }

    @Test
    public void shouldCreateFilter()
            throws Exception
        {
        AndOperator op       = new AndOperator();
        Filter      left     = new EqualsFilter("getFoo", 1234);
        Filter      right    = new EqualsFilter("getBar", 9876);
        Filter      result   = op.makeFilter(left, right);
        Filter      expected = new AllFilter(new Filter[] {left, right});

        assertThat(result, is(expected));
        }

    @Test
    public void shouldCreateFilterWhenLeftIsAllFilter()
            throws Exception
        {
        AndOperator op       = new AndOperator();
        Filter      left1    = new EqualsFilter("getFoo1", 1234);
        Filter      left2    = new EqualsFilter("getFoo2", 1234);
        Filter      left     = new AndFilter(left1, left2);
        Filter      right    = new EqualsFilter("getBar", 9876);
        Filter      result   = op.makeFilter(left, right);
        Filter      expected = new AllFilter(new Filter[] {left1, left2, right});

        assertThat(result, is(expected));
        }

    @Test
    public void shouldCreateFilterWhenRightIsAllFilter()
            throws Exception
        {
        AndOperator op       = new AndOperator();
        Filter      left     = new EqualsFilter("getFoo", 1234);
        Filter      right1   = new EqualsFilter("getBar", 9876);
        Filter      right2   = new EqualsFilter("getBar", 9876);
        Filter      right    = new AndFilter(right1, right2);
        Filter      result   = op.makeFilter(left, right);
        Filter      expected = new AllFilter(new Filter[] {left, right1, right2});

        assertThat(result, is(expected));
        }

    @Test
    public void shouldCreateFilterWhenLeftAndRightAreAllFilters()
            throws Exception
        {
        AndOperator op       = new AndOperator();
        Filter      left1    = new EqualsFilter("getFoo1", 1234);
        Filter      left2    = new EqualsFilter("getFoo2", 1234);
        Filter      left     = new AndFilter(left1, left2);
        Filter      right1   = new EqualsFilter("getBar", 9876);
        Filter      right2   = new EqualsFilter("getBar", 9876);
        Filter      right    = new AndFilter(right1, right2);
        Filter      result   = op.makeFilter(left, right);
        Filter      expected = new AllFilter(new Filter[] {left1, left2, right1, right2});

        assertThat(result, is(expected));
        }

    @Test
    public void shouldBuildFilterFromTerms()
            throws Exception
        {
        Term        term     = parse("foo == 1 and bar == 2");
        AndOperator op       = new AndOperator();
        Filter      result   = op.makeFilter(term.termAt(2), term.termAt(3), new FilterBuilder());
        Filter      expected = new AllFilter(new Filter[] {new EqualsFilter("getFoo", 1),
                                   new EqualsFilter("getBar", 2)});

        assertThat(result, is(expected));
        }
    }
