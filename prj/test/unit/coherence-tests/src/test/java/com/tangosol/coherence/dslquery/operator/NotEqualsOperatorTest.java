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
import com.tangosol.util.filter.NotEqualsFilter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author jk  2013.12.31
 */
public class NotEqualsOperatorTest
        extends BaseOperatorTest
    {
    @Test
    public void shouldHaveCorrectSymbol()
            throws Exception
        {
        NotEqualsOperator op = new NotEqualsOperator();

        assertThat(op.getSymbol(), is("!="));
        }

    @Test
    public void shouldHaveCorrectAliases()
            throws Exception
        {
        NotEqualsOperator op = new NotEqualsOperator();

        assertThat(op.getAliases(), is(new String[] {"<>"}));
        }

    @Test
    public void shouldAddToTokenTable()
            throws Exception
        {
        TokenTable        tokens = new TokenTable();
        NotEqualsOperator op     = new NotEqualsOperator();

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
        NotEqualsOperator op       = new NotEqualsOperator();
        ValueExtractor    left     = new ReflectionExtractor("getFoo");
        Object            right    = new Object();
        Filter            result   = op.makeFilter(left, right);
        Filter            expected = new NotEqualsFilter(left, right);

        assertThat(result, is(expected));
        }

    @Test
    public void shouldBuildFilterFromTerms()
            throws Exception
        {
        Term              term     = parse("foo != 100");
        NotEqualsOperator op       = new NotEqualsOperator();
        Filter            result   = op.makeFilter(term.termAt(2), term.termAt(3), new FilterBuilder());
        Filter            expected = new NotEqualsFilter("getFoo", 100);

        assertThat(result, is(expected));
        }

    @Test
    public void shouldReturnSelfWhenFlipped()
            throws Exception
        {
        NotEqualsOperator op = new NotEqualsOperator();

        assertThat(op.flip(), is(sameInstance((BaseOperator) op)));
        }
    }
