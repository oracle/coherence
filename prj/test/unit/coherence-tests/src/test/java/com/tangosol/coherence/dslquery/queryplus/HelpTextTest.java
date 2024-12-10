/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.QueryPlus;
import com.tangosol.coherence.dslquery.StatementBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.03.16
 */
@RunWith(Parameterized.class)
public class HelpTextTest
    {

    public HelpTextTest(String sClassName, StatementBuilder statementBuilder)
        {
        m_sClassName = sClassName;
        m_statementBuilder = statementBuilder;
        }

    @Test
    public void shouldNotHaveSyntaxLongerThanEightyCharacters() throws Exception
        {
        assertLinesLessThanEightyCharacters(m_statementBuilder.getSyntax());
        }

    @Test
    public void shouldNotHaveDescriptionLongerThanEightyCharacters() throws Exception
        {
        assertLinesLessThanEightyCharacters(m_statementBuilder.getDescription());
        }

    protected void assertLinesLessThanEightyCharacters(String sText)
        {
        StringJoiner stringJoiner = new StringJoiner("\n");
        boolean      fFailed      = false;
        String[]     asParts      = sText.split("\n");

        stringJoiner.add("Lines are longer than 80 characters:");

        for (String sLine : asParts)
            {
            int nLen = sLine.length();
            fFailed = fFailed || nLen > 80;
            stringJoiner.add(nLen + "\t" + sLine);
            }

        assertThat(stringJoiner.toString(), fFailed, is(false));
        }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> getStatementBuilders()
        {
        PrintWriter            writer   = new PrintWriter(System.out);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();
        QueryPlus.Dependencies deps     = QueryPlus.DependenciesHelper.newInstance(writer, System.in,
                language, new String[0]);

        QueryPlus                       queryPlus   = new QueryPlus(deps);
        Collection<StatementBuilder<?>> colBuilders = language.getStatementBuilders().values();
        List<Object[]>                  aoParams    = new ArrayList<>();

        for (StatementBuilder statementBuilder : colBuilders)
            {
            aoParams.add(new Object[]{statementBuilder.getClass().getName(), statementBuilder});
            }

        return aoParams;
        }

    private final StatementBuilder m_statementBuilder;
    private final String m_sClassName;
    }
