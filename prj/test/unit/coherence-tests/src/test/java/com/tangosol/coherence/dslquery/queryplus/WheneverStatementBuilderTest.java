/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.QueryPlus;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import org.junit.Before;
import org.junit.Test;

import static com.tangosol.coherence.dslquery.TermMatcher.matchingTerm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.io.PrintWriter;

/**
 * @author jk 2014.08.05
 */
public class WheneverStatementBuilderTest
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
    public void shouldParseWheneverCohQLErrorThenExit()
            throws Exception
        {
        AbstractQueryPlusStatementBuilder.AbstractOPToken token    = m_builder.instantiateOpToken();

        CoherenceQueryLanguage                            language = new CoherenceQueryLanguage();

        language.addStatement(token.getFunctor(), m_builder);
        language.extendedSqlTokenTable().addToken(token);
        language.sqlTokenTable().addToken(token);

        String   query  = "whenever cohqlerror then exit";
        OPParser parser = new OPParser(query, language.sqlTokenTable(), language.getOperators());

        Term     term   = parser.parse();

        assertThat(term, is(matchingTerm("wheneverCommand('exit')")));
        }

    @Test
    public void shouldParseWheneverCohQLErrorThenContinue()
            throws Exception
        {
        AbstractQueryPlusStatementBuilder.AbstractOPToken token    = m_builder.instantiateOpToken();

        CoherenceQueryLanguage                            language = new CoherenceQueryLanguage();

        language.addStatement(token.getFunctor(), m_builder);
        language.extendedSqlTokenTable().addToken(token);
        language.sqlTokenTable().addToken(token);

        String   query  = "whenever cohqlerror then continue";
        OPParser parser = new OPParser(query, language.sqlTokenTable(), language.getOperators());

        Term     term   = parser.parse();

        assertThat(term, is(matchingTerm("wheneverCommand('continue')")));
        }

    @Test
    public void shouldBuildWheneverStatementWithContinueAction()
            throws Exception
        {
        ExecutionContext                                    ctx       = new ExecutionContext();
        NodeTerm                                            term      =
            (NodeTerm) Terms.create("wheneverCommand('continue')");

        WheneverStatementBuilder.WheneverQueryPlusStatement statement = m_builder.realize(ctx, term, null, null);

        assertThat(statement.m_fStopOnError, is(false));
        }

    @Test
    public void shouldBuildWheneverStatementWithExitAction()
            throws Exception
        {
        ExecutionContext                                    ctx       = new ExecutionContext();
        NodeTerm                                            term      =
            (NodeTerm) Terms.create("wheneverCommand('exit')");

        WheneverStatementBuilder.WheneverQueryPlusStatement statement = m_builder.realize(ctx, term, null, null);

        assertThat(statement.m_fStopOnError, is(true));
        }

    @Test(expected = CohQLException.class)
    public void shouldBuildWheneverStatementWithInvalidAction()
            throws Exception
        {
        ExecutionContext ctx  = new ExecutionContext();
        NodeTerm         term = (NodeTerm) Terms.create("wheneverCommand('foo')");

        m_builder.realize(ctx, term, null, null);
        }

    @Test
    public void shouldSetErrorActionToExit()
            throws Exception
        {
        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        String[]               asArgs       = new String[] {"-c", "-l", "whenever cohqlerror then exit"};
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setStopOnError(false);

        queryPlus.run();
        assertThat(ctx.isStopOnError(), is(true));
        }

    @Test
    public void shouldSetErrorActionToContinue()
            throws Exception
        {
        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        String[]               asArgs       = new String[] {"-c", "-l", "whenever cohqlerror then continue"};
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setStopOnError(true);

        queryPlus.run();
        assertThat(ctx.isStopOnError(), is(false));
        }

    @Before
    public void setup()
        {
        m_language = new CoherenceQueryLanguage();
        m_builder  = new WheneverStatementBuilder();

        AbstractQueryPlusStatementBuilder.AbstractOPToken token = m_builder.instantiateOpToken();

        m_language.addStatement(token.getFunctor(), m_builder);

        m_tokens = m_language.extendedSqlTokenTable();
        m_tokens.addToken(token);
        }

    private WheneverStatementBuilder m_builder;
    private TokenTable               m_tokens;
    private CoherenceQueryLanguage   m_language;
    }
