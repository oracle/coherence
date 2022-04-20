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
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.XorFilter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author jk  2013.12.31
 */
public class XorOperatorTest
        extends BaseOperatorTest
    {
    @Test
    public void shouldHaveCorrectSymbol()
            throws Exception
        {
        XorOperator op = new XorOperator();

        assertThat(op.getSymbol(), is("^^"));
        }

    @Test
    public void shouldHaveCorrectAliases()
            throws Exception
        {
        XorOperator op = new XorOperator();

        assertThat(op.getAliases(), is(new String[] {"xor"}));
        }

    @Test
    public void shouldAddToTokenTable()
            throws Exception
        {
        TokenTable  tokens = new TokenTable();
        XorOperator op     = new XorOperator();

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
        XorOperator op       = new XorOperator();
        Filter      left     = new EqualsFilter("getFoo", 1);
        Filter      right    = new EqualsFilter("getBar", 2);
        Filter      result   = op.makeFilter(left, right);
        Filter      expected = new XorFilter(left, right);

        assertThat(result, is(expected));
        }

    @Test
    public void shouldBuildFilterFromTerms()
            throws Exception
        {
        Term        term     = parse("foo == 100 ^^ bar == 200");
        XorOperator op       = new XorOperator();
        Filter      result   = op.makeFilter(term.termAt(2), term.termAt(3), new FilterBuilder());
        Filter      expected = new XorFilter(new EqualsFilter("getFoo", 100), new EqualsFilter("getBar", 200));

        assertThat(result, is(expected));
        }
    }
