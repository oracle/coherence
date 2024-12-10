/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.Statement;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2014.01.06
 */
public class ExtendedLanguageStatementBuilderTest
    {
    @Test
    public void shouldHaveDescription()
            throws Exception
        {
        assertThat(m_builder.getDescription(), is(notNullValue()));
        }

    @Test
    public void shouldHaveSyntaxText()
            throws Exception
        {
        assertThat(m_builder.getSyntax(), is(notNullValue()));
        }

    @Test
    public void shouldTurnOnExtendedLanguage()
            throws Exception
        {
        NodeTerm  term      = (NodeTerm) Terms.create("extendedLanguageCommand('on')");
        Statement statement = m_builder.realize(m_context, term, null, null);

        statement.execute(m_context);

        verify(m_context).setExtendedLanguage(true);
        }

    @Test
    public void shouldTurnOffExtendedLanguage()
            throws Exception
        {
        NodeTerm  term      = (NodeTerm) Terms.create("extendedLanguageCommand('off')");
        Statement statement = m_builder.realize(m_context, term, null, null);

        statement.execute(m_context);

        verify(m_context).setExtendedLanguage(false);
        }

    @Test(expected = CohQLException.class)
    public void shouldThrowExceptionIfSyntaxIsWrong()
            throws Exception
        {
        NodeTerm term = (NodeTerm) Terms.create("extendedLanguageCommand('foo')");

        m_builder.realize(m_context, term, null, null);
        }

    @Before
    public void setup()
        {
        m_context = mock(ExecutionContext.class);

        when(m_context.getWriter()).thenReturn(new PrintWriter(System.out));

        m_builder = new ExtendedLanguageStatementBuilder();
        }

    private ExecutionContext                 m_context;
    private ExtendedLanguageStatementBuilder m_builder;
    }
