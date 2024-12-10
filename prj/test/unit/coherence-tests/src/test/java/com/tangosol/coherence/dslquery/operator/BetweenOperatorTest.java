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
import com.tangosol.util.filter.BetweenFilter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author jk  2013.12.31
 */
public class BetweenOperatorTest
        extends BaseOperatorTest
    {
    @Test
    public void shouldHaveCorrectSymbol()
            throws Exception
        {
        BetweenOperator op = new BetweenOperator();

        assertThat(op.getSymbol(), is("between"));
        }

    @Test
    public void shouldHaveCorrectAliases()
            throws Exception
        {
        BetweenOperator op = new BetweenOperator();

        assertThat(op.getAliases().length, is(0));
        }

    @Test
    public void shouldAddToTokenTable()
            throws Exception
        {
        TokenTable      tokens = new TokenTable();
        BetweenOperator op     = new BetweenOperator();

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
        BetweenOperator op       = new BetweenOperator();
        ValueExtractor  left     = new ReflectionExtractor("getFoo");
        Object[]        right    = new Object[] {100, 200};
        Filter          result   = op.makeFilter(left, right);
        Filter          expected = new BetweenFilter(left, 100, 200);

        assertThat(result, is(expected));
        }

    @Test
    public void shouldBuildFilterFromTerms()
            throws Exception
        {
        Term            term     = parse("foo between 100 and 200");
        BetweenOperator op       = new BetweenOperator();
        Filter          result   = op.makeFilter(term.termAt(2), term.termAt(3), new FilterBuilder());
        Filter          expected = new BetweenFilter("getFoo", 100, 200);

        assertThat(result, is(expected));
        }
    }
