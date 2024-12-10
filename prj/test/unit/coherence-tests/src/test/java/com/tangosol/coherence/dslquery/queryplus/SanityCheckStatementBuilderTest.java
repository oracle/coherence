/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

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
public class SanityCheckStatementBuilderTest
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
    public void shouldTurnOnSanityCheck()
            throws Exception
        {
        NodeTerm  term      = (NodeTerm) Terms.create("sanityCheckCommand('on')");
        Statement statement = m_builder.realize(m_context, term, null, null);

        statement.execute(m_context);

        verify(m_context).setSanityCheckingEnabled(true);
        }

    @Test
    public void shouldTurnOffSanityCheck()
            throws Exception
        {
        NodeTerm  term      = (NodeTerm) Terms.create("sanityCheckCommand('off')");
        Statement statement = m_builder.realize(m_context, term, null, null);

        statement.execute(m_context);

        verify(m_context).setSanityCheckingEnabled(false);
        }

    @Before
    public void setup()
        {
        m_context = mock(ExecutionContext.class);

        when(m_context.getWriter()).thenReturn(new PrintWriter(System.out));

        m_builder = new SanityCheckStatementBuilder();
        }

    private SanityCheckStatementBuilder m_builder;
    private ExecutionContext            m_context;
    }
