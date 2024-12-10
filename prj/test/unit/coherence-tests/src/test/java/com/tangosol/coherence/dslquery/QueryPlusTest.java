/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.filter.AlwaysFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;

import java.io.StringWriter;

/**
 * @author jk 2014.08.05
 */
public class QueryPlusTest
    {
    @Before
    public void setup()
            throws Exception
        {
        m_savedClassLoader = Thread.currentThread().getContextClassLoader();

        m_cluster = mock(Cluster.class);
        m_cache   = mock(NamedCache.class);
        m_session = mock(Session.class);

        when(m_session.getCache(eq("test"), any(TypeAssertion.class))).thenReturn(m_cache);

        m_commandFile1 = temporaryFolder.newFile("commands1.txt");

        PrintWriter writer1 = new PrintWriter(m_commandFile1);

        writer1.println("create cache test;");
        writer1.println("bad command;");
        writer1.flush();
        writer1.close();

        m_commandFile2 = temporaryFolder.newFile("commands2.txt");

        PrintWriter writer2 = new PrintWriter(m_commandFile2);

        writer2.println("select * from 'test';");
        writer2.flush();
        writer2.close();
        }

    @After
    public void cleanup()
        {
        Thread.currentThread().setContextClassLoader(m_savedClassLoader);
        }

    @Test
    public void shouldStopExecutionOfCommandsOnError()
            throws Exception
        {
        String[] asArgs = new String[]
            {
            "-c", "-l", "whenever cohqlerror then exit", "-l", "create cache 'test'", "-l", "bad command", "-l",
            "select * from 'test'"
            };

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setCluster(m_cluster);
        ctx.setSession(m_session);
        ctx.setSanityCheckingEnabled(false);

        queryPlus.run();

        verify(m_session, atLeastOnce()).getCache(eq("test"), any(TypeAssertion.class));
        verify(m_cache, never()).entrySet(isA(AlwaysFilter.class));
        }

    @Test
    public void shouldContinueExecutionOfCommandsOnError()
            throws Exception
        {
        String[] asArgs = new String[]
            {
            "-c", "-l", "whenever cohqlerror then continue", "-l", "create cache 'test'", "-l", "bad command", "-l",
            "select * from 'test'"
            };

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setCluster(m_cluster);
        ctx.setSession(m_session);
        ctx.setSanityCheckingEnabled(false);

        queryPlus.run();

        verify(m_session, atLeastOnce()).getCache(eq("test"), any(TypeAssertion.class));
        verify(m_cache).entrySet(isA(AlwaysFilter.class));
        }

    @Test
    public void shouldStopExecutionOfFilesOnError()
            throws Exception
        {
        String[] asArgs = new String[]
            {
            "-c", "-l", "whenever cohqlerror then exit", "-f", m_commandFile1.getCanonicalPath(), "-f",
            m_commandFile2.getCanonicalPath(),
            };

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setCluster(m_cluster);
        ctx.setSession(m_session);
        ctx.setSanityCheckingEnabled(false);
        ctx.setSilentMode(false);

        queryPlus.run();

        assertThat(ctx.isSilent(), is(false));
        verify(m_session, atLeastOnce()).getCache(eq("test"), any(TypeAssertion.class));
        verify(m_cache, never()).entrySet(isA(AlwaysFilter.class));
        }

    @Test
    public void shouldContinueExecutionOfFilesOnError()
            throws Exception
        {
        String[] asArgs = new String[]
            {
            "-c", "-l", "whenever cohqlerror then continue", "-f", m_commandFile1.getCanonicalPath(), "-f",
            m_commandFile2.getCanonicalPath(),
            };

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setCluster(m_cluster);
        ctx.setSession(m_session);
        ctx.setSanityCheckingEnabled(false);
        ctx.setSilentMode(false);

        queryPlus.run();

        assertThat(ctx.isSilent(), is(false));
        verify(m_session, atLeastOnce()).getCache(eq("test"), any(TypeAssertion.class));
        verify(m_cache).entrySet(isA(AlwaysFilter.class));
        }

    @Test
    public void shouldPassSilentModeAsFalseToExecutionContext()
            throws Exception
        {
        String[]               asArgs       = new String[]
            {
            };

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        assertThat(ctx.isSilent(), is(false));
        }

    @Test
    public void shouldPassSilentModeAsTrueToExecutionContext()
            throws Exception
        {
        String[]               asArgs       = new String[] {"-s"};

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        assertThat(ctx.isSilent(), is(true));
        }

    @Test
    public void shouldSetTimeoutValue() throws Exception
        {
        String[]               asArgs       = new String[] {"-timeout", "10000"};

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        assertThat(ctx.getTimeout(), is(new Duration(10000, Duration.Magnitude.MILLI)));
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSetInvalidTimeoutValue() throws Exception
        {
        String[]               asArgs       = new String[] {"-timeout", "10000x"};

        PrintWriter            writer       = new PrintWriter(System.out);
        InputStream            inputStream  = System.in;
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        assertThat(ctx.getTimeout(), is(new Duration(10000, Duration.Magnitude.MILLI)));
        }

    @Test
    public void shouldNotPrintCommandPromptInSilentMode() throws Exception
        {
        String[]               asArgs       = {"-s"};
        StringWriter           out          = new StringWriter();
        PrintWriter            writer       = new PrintWriter(out);
        InputStream            inputStream  = new ByteArrayInputStream(new byte[0]);
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setCluster(m_cluster);
        ctx.setSession(m_session);

        queryPlus.repl();

        assertThat(out.toString(), not(containsString("CohQL>")));
        }

    @Test
    public void shouldPrintCommandPromptInNonSilentMode() throws Exception
        {
        String[]               asArgs       = new String[0];
        StringWriter           out          = new StringWriter();
        PrintWriter            writer       = new PrintWriter(out);
        InputStream            inputStream  = new ByteArrayInputStream(new byte[0]);
        CoherenceQueryLanguage language     = new CoherenceQueryLanguage();
        QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(writer, inputStream, language,
                                                  asArgs);

        QueryPlus        queryPlus = new QueryPlus(dependencies);
        ExecutionContext ctx       = queryPlus.getExecutionContext();

        ctx.setCluster(m_cluster);
        ctx.setSession(m_session);

        queryPlus.repl();

        assertThat(out.toString(), containsString("CohQL>"));
        }

    // ----- data members ---------------------------------------------------

    protected ClassLoader              m_savedClassLoader;
    protected Session                  m_session;
    protected Cluster                  m_cluster;
    protected NamedCache               m_cache;
    protected File                     m_commandFile1;
    protected File                     m_commandFile2;

    /**
     * JUnit rule to use to create and destroy temporary folders for tests
     * that use files. This should ensure all the test file junk is cleaned up.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    }
