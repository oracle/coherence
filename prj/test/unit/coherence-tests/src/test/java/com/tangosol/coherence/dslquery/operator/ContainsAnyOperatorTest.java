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
import com.tangosol.util.filter.ContainsAnyFilter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static com.oracle.coherence.testing.util.CollectionUtils.setWith;

/**
 * @author jk  2013.12.31
 */
public class ContainsAnyOperatorTest
        extends BaseOperatorTest
    {
    @Test
    public void shouldHaveCorrectSymbol()
            throws Exception
        {
        ContainsAnyOperator op = new ContainsAnyOperator();

        assertThat(op.getSymbol(), is("contains_any"));
        }

    @Test
    public void shouldHaveCorrectAliases()
            throws Exception
        {
        ContainsAnyOperator op = new ContainsAnyOperator();

        assertThat(op.getAliases().length, is(0));
        }

    @Test
    public void shouldAddToTokenTable()
            throws Exception
        {
        TokenTable          tokens = new TokenTable();
        ContainsAnyOperator op     = new ContainsAnyOperator();

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
        ContainsAnyOperator op       = new ContainsAnyOperator();
        ValueExtractor      left     = new ReflectionExtractor("getFoo");
        Object[]            right    = new Object[] {100, 200, 300};
        Filter              result   = op.makeFilter(left, right);
        Filter              expected = new ContainsAnyFilter(left, setWith(100, 200, 300));

        assertThat(result, is(expected));
        }

    @Test
    public void shouldBuildFilterFromTerms()
            throws Exception
        {
        Term                term     = parse("foo contains_any (1,2,3,4)");
        ContainsAnyOperator op       = new ContainsAnyOperator();
        Filter              result   = op.makeFilter(term.termAt(2), term.termAt(3), new FilterBuilder());
        Filter              expected = new ContainsAnyFilter("getFoo", setWith(1, 2, 3, 4));

        assertThat(result, is(expected));
        }
    }
