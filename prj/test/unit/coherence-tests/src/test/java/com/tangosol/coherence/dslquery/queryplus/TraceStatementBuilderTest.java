/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.Statement;
import com.tangosol.coherence.dslquery.StatementBuilder;

import com.tangosol.coherence.dslquery.statement.QueryRecorderStatementBuilder;
import com.tangosol.coherence.dslquery.token.SQLTraceOPToken;

import com.tangosol.coherence.dsltools.precedence.IdentifierOPToken;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.net.ConfigurableCacheFactory;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringReader;

/**
 * @author jk  2014.01.06
 */
public class TraceStatementBuilderTest
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
    public void shouldTurnOnTrace()
            throws Exception
        {
        query("trace on");
        verify(m_executionContext).setTraceEnabled(true);
        }

    @Test
    public void shouldTurnOffTrace()
            throws Exception
        {
        query("trace off");
        verify(m_executionContext).setTraceEnabled(false);
        }

    @Test
    public void shouldDelegateToSQLQueryRecorderQueryBuilder()
            throws Exception
        {
        Statement statement = build("trace select * from foo where bar == 1");

        assertThat(statement, is(instanceOf(QueryRecorderStatementBuilder.QueryRecorderStatement.class)));
        }

    private void query(String query)
        {
        ConfigurableCacheFactory cacheFactory = mock(ConfigurableCacheFactory.class);
        PrintWriter              out          = new PrintWriter(System.out);

        when(m_executionContext.getCacheFactory()).thenReturn(cacheFactory);
        when(m_executionContext.isTraceEnabled()).thenReturn(true);
        when(m_executionContext.getWriter()).thenReturn(out);

        Statement statement = build(query);

        statement.execute(m_executionContext);
        }

    private Statement build(String query)
        {
        OPParser            parser           = new OPParser(new StringReader(query), m_tokens,
                                                   m_language.getOperators());

        NodeTerm            term             = parser.parse();

        StatementBuilder<?> statementBuilder = m_language.getStatementBuilder(term.getFunctor());

        return statementBuilder.realize(m_executionContext, term, null, null);
        }

    @Before
    public void setup()
        {
        m_executionContext = mock(ExecutionContext.class);
        m_delegate         = new SQLTraceOPToken("trace");
        m_language         = new CoherenceQueryLanguage();

        when(m_executionContext.getCoherenceQueryLanguage()).thenReturn(m_language);

        m_builder = new TraceStatementBuilder(m_delegate);

        m_language.addStatement(m_builder.instantiateOpToken().getFunctor(), m_builder);

        m_tokens = m_language.extendedSqlTokenTable();
        m_tokens.addToken(m_builder.instantiateOpToken());
        }

    private TraceStatementBuilder  m_builder;
    private ExecutionContext       m_executionContext;
    private IdentifierOPToken      m_delegate;
    private TokenTable             m_tokens;
    private CoherenceQueryLanguage m_language;
    }
