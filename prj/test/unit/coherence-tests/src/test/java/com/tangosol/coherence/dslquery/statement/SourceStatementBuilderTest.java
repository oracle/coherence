/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementExecutor;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dsltools.precedence.OPParser;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.ValueExtractor;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.mockito.InOrder;

import org.mockito.invocation.InvocationOnMock;

import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.PrintWriter;
import java.io.Reader;

import java.util.Comparator;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.12.09
 */
public class SourceStatementBuilderTest
    {
    @Test
    public void shouldRealizeQuery()
            throws Exception
        {
        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create("sqlSourceNode(file('test'))");

        SourceStatementBuilder builder = SourceStatementBuilder.INSTANCE;

        SourceStatementBuilder.SourceStatement query
                = builder.realize(context, term, null, null);

        assertThat(query.f_sFileName, is("test"));
        }

    @Test
    public void shouldThrowExceptionIfFileIsEmptyString()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed for sourcing");

        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create("sqlSourceNode(file(''))");

        SourceStatementBuilder builder = SourceStatementBuilder.INSTANCE;

        builder.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfFileIsBlank()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed for sourcing");

        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create("sqlSourceNode(file())");

        SourceStatementBuilder builder = SourceStatementBuilder.INSTANCE;

        builder.realize(context, term, null, null);
        }

    @Test
    public void shouldThrowExceptionIfFileIsMissing()
            throws Exception
        {
        expectedEx.expect(CohQLException.class);
        expectedEx.expectMessage("File name needed for sourcing");

        ExecutionContext       context = mock(ExecutionContext.class);
        NodeTerm               term    = (NodeTerm) Terms.create("sqlSourceNode(foo())");

        SourceStatementBuilder builder = SourceStatementBuilder.INSTANCE;

        builder.realize(context, term, null, null);
        }

    @Test
    public void shouldPerformSourceCommand()
            throws Exception
        {
        File        commandFile = temporaryFolder.newFile("commands.txt");
        PrintWriter writer      = new PrintWriter(commandFile);

        writer.println("create index on test firstName;");
        writer.println("drop index on test firstName;");
        writer.flush();
        writer.close();

        ExecutionContext         context  = mock(ExecutionContext.class);
        Session                  session  = mock(Session.class);
        NamedCache               cache    = mock(NamedCache.class);
        CoherenceQueryLanguage   language = new CoherenceQueryLanguage();
        StatementExecutor        executor = new StatementExecutor();

        when(session.getCache(eq("test"), any(TypeAssertion.class))).thenReturn(cache);
        when(context.getSession()).thenReturn(session);
        when(context.getCoherenceQueryLanguage()).thenReturn(language);
        when(context.getTimeout()).thenReturn(new Duration(Long.MAX_VALUE));
        when(context.getStatementExecutor()).thenReturn(executor);
        when(context.instantiateParser(any(Reader.class))).then(new Answer<Object>()
            {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
                {
                Reader reader = (Reader) invocation.getArguments()[0];
                return new OPParser(reader, m_language.sqlTokenTable(), m_language.getOperators());
                }
            });

        SourceStatementBuilder.SourceStatement statement
                = new SourceStatementBuilder.SourceStatement(commandFile.getCanonicalPath());

        StatementResult result = statement.execute(context);

        assertThat(result.getResult(), is(nullValue()));

        InOrder inOrder = inOrder(cache);

        inOrder.verify(cache).addIndex(any(ValueExtractor.class), eq(true), (Comparator) isNull());
        inOrder.verify(cache).removeIndex(any(ValueExtractor.class));
        }

    @Test
    public void shouldForceSilentModeAndResetAfterExecution()
            throws Exception
        {
        File        commandFile = temporaryFolder.newFile("commands.txt");
        PrintWriter writer      = new PrintWriter(commandFile);

        writer.println("create index on test firstName;");
        writer.println("drop index on test firstName;");
        writer.flush();
        writer.close();

        ExecutionContext         context  = mock(ExecutionContext.class);
        StatementExecutor        executor = mock(StatementExecutor.class);

        when(context.getStatementExecutor()).thenReturn(executor);
        when(context.isSilent()).thenReturn(false);

        SourceStatementBuilder.SourceStatement statement
                = new SourceStatementBuilder.SourceStatement(commandFile.getCanonicalPath());

        StatementResult result = statement.execute(context);

        assertThat(result.getResult(), is(nullValue()));

        InOrder inOrder = inOrder(executor, context);
        inOrder.verify(context).setSilentMode(true);
        inOrder.verify(executor).execute(any(Reader.class), same(context));
        inOrder.verify(context).setSilentMode(false);
        }

    @Test
    public void shouldPerformSourceCommandAndExitOnError()
            throws Exception
        {
        File        commandFile = temporaryFolder.newFile("commands.txt");
        PrintWriter writer      = new PrintWriter(commandFile);

        writer.println("create index on test firstName;");
        writer.println("this is not going to work;");
        writer.println("drop index on test firstName;");
        writer.flush();
        writer.close();

        ExecutionContext       context  = mock(ExecutionContext.class);
        Session                session  = mock(Session.class);
        NamedCache             cache    = mock(NamedCache.class);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();
        StatementExecutor      executor = new StatementExecutor();

        when(session.getCache(eq("test"), any(TypeAssertion.class))).thenReturn(cache);
        when(context.getSession()).thenReturn(session);
        when(context.getCoherenceQueryLanguage()).thenReturn(language);
        when(context.isStopOnError()).thenReturn(true);
        when(context.getTimeout()).thenReturn(new Duration(Long.MAX_VALUE));
        when(context.getStatementExecutor()).thenReturn(executor);
        when(context.instantiateParser(any(Reader.class))).then(new Answer<Object>()
            {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
                {
                Reader reader = (Reader) invocation.getArguments()[0];
                return new OPParser(reader, m_language.sqlTokenTable(), m_language.getOperators());
                }
            });

        SourceStatementBuilder.SourceStatement statement
                = new SourceStatementBuilder.SourceStatement(commandFile.getCanonicalPath());

        try
            {
            statement.execute(context);
            fail("Should have thrown CohQLException");
            }
        catch (Exception e)
            {
            assertThat(e, is(instanceOf(CohQLException.class)));
            }

        InOrder inOrder = inOrder(cache);

        inOrder.verify(cache).addIndex(any(ValueExtractor.class), eq(true), (Comparator) isNull());
        inOrder.verify(cache, never()).removeIndex(any(ValueExtractor.class));
        }

    @Test
    public void shouldPerformSourceCommandAndContinueOnError()
            throws Exception
        {
        File        commandFile = temporaryFolder.newFile("commands.txt");
        PrintWriter writer      = new PrintWriter(commandFile);

        writer.println("create index on test firstName;");
        writer.println("this is not going to work;");
        writer.println("drop index on test firstName;");
        writer.flush();
        writer.close();

        ExecutionContext       context  = mock(ExecutionContext.class);
        Session                session  = mock(Session.class);
        NamedCache             cache    = mock(NamedCache.class);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();
        StatementExecutor      executor = new StatementExecutor();

        when(session.getCache(eq("test"), any(TypeAssertion.class))).thenReturn(cache);
        when(context.getSession()).thenReturn(session);
        when(context.getCoherenceQueryLanguage()).thenReturn(language);
        when(context.isStopOnError()).thenReturn(false);
        when(context.getTimeout()).thenReturn(new Duration(Long.MAX_VALUE));
        when(context.getStatementExecutor()).thenReturn(executor);
        when(context.getWriter()).thenReturn(new PrintWriter(System.out));
        when(context.instantiateParser(any(Reader.class))).then(new Answer<Object>()
            {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
                {
                Reader reader = (Reader) invocation.getArguments()[0];
                return new OPParser(reader, m_language.sqlTokenTable(), m_language.getOperators());
                }
            });

        SourceStatementBuilder.SourceStatement statement
                = new SourceStatementBuilder.SourceStatement(commandFile.getCanonicalPath());

        statement.execute(context);

        InOrder inOrder = inOrder(cache);

        inOrder.verify(cache).addIndex(any(ValueExtractor.class), eq(true), (Comparator) isNull());
        inOrder.verify(cache).removeIndex(any(ValueExtractor.class));
        }

    @Test
    public void shouldPerformSourceCommandWithNonExistentFile()
            throws Exception
        {
        File commandFile = temporaryFolder.newFile("bad-file.txt");

        if (commandFile.exists())
            {
            commandFile.delete();
            }

        ExecutionContext context = mock(ExecutionContext.class);

        SourceStatementBuilder.SourceStatement statement
                = new SourceStatementBuilder.SourceStatement(commandFile.getCanonicalPath());

        StatementResult result = statement.execute(context);

        assertThat(result.getResult(), is(nullValue()));
        }

    /**
     * JUnit rule to use to capture expected exceptions
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * JUnit rule to use to create and destroy temporary folders for tests
     * that use files. This should ensure all the test file junk is cleaned up.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    protected final CoherenceQueryLanguage m_language = new CoherenceQueryLanguage();
    }
