/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * Instances of this context are passed to {@link Statement}s to allow
 * commands to discern execution conditions, altering their behavior / result
 * as needed. In addition, commands may wish to alter the execution conditions
 * or hold state across executions; the latter is possible via {@link
 * #getResourceRegistry()}.
 *
 * @author jk 2014.07.10
 * @since Coherence 12.2.1
 */
public class ExecutionContext
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an ExecutionContext.
     */
    public ExecutionContext()
        {
        f_resourceRegistry = new SimpleResourceRegistry();
        }


    // ----- ExecutionContext methods ------------------------------------------------

    /**
     * Return the {@link ResourceRegistry} that may be used to register
     * ad-hoc resources with this context as a way to maintain state between
     * different command executions.
     *
     * @return the ResourceRegistry that may be used to register ad-hoc resources
     */
    public ResourceRegistry getResourceRegistry()
        {
        return f_resourceRegistry;
        }

    /**
     * Set the current {@link ConfigurableCacheFactory} to be used by commands
     * executed under this context.
     *
     * @param ccf  the ConfigurableCacheFactory to be used by commands
     *             executed with this context
     */
    public void setCacheFactory(ConfigurableCacheFactory ccf)
        {
        m_cacheFactory = ccf;
        }

    /**
     * Return the current {@link ConfigurableCacheFactory} to be used
     * by commands executed under this context.
     *
     * @return the current ConfigurableCacheFactory
     */
    public ConfigurableCacheFactory getCacheFactory()
        {
        if (m_cacheFactory == null)
            {
            m_cacheFactory = CacheFactory.getConfigurableCacheFactory();
            }

        return m_cacheFactory;
        }

    /**
     * Set the current {@link Cluster} to be used by commands
     * executed under this context.
     *
     * @param cluster  the Cluster to be used by commands
     *                 executed with this context
     */
    public void setCluster(Cluster cluster)
        {
        m_cluster = cluster;
        }

    /**
     * Return the current {@link Cluster} to be used
     * by commands executed under this context.
     *
     * @return the current Cluster
     */
    public Cluster getCluster()
        {
        if (m_cluster == null)
            {
            m_cluster = CacheFactory.ensureCluster();
            }

        return m_cluster;
        }

    /**
     * Set the {@link CoherenceQueryLanguage} that will be used by commands.
     *
     * @param language  the CoherenceQueryLanguage to be used by commands
     */
    public void setCoherenceQueryLanguage(CoherenceQueryLanguage language)
        {
        m_language = language;
        }

    /**
     * Return an instance of {@link CoherenceQueryLanguage} to be
     * used by commands.
     *
     * @return the instance of CoherenceQueryLanguage to be
     *         used by commands
     */
    public CoherenceQueryLanguage getCoherenceQueryLanguage()
        {
        return m_language;
        }

    /**
     * Set whether to display trace output when executing commands.
     *
     * @param fTrace  whether to display trace output when executing commands
     */
    public void setTraceEnabled(boolean fTrace)
        {
        m_fTrace = fTrace;
        }

    /**
     * Return whether trace to display output when executing commands.
     *
     * @return true if trace is enabled, otherwise returns false
     */
    public boolean isTraceEnabled()
        {
        return m_fTrace;
        }

    /**
     * Set whether sanity checking is enabled when executing commands.
     *
     * @param fSanity  whether sanity checking is enabled when executing commands
     */
    public void setSanityCheckingEnabled(boolean fSanity)
        {
        m_fSanity = fSanity;
        }

    /**
     * Return whether sanity checking should be enabled when executing commands.
     *
     * @return whether sanity checking should be enabled when executing commands
     */
    public boolean isSanityChecking()
        {
        return m_fSanity;
        }

    /**
     * Set whether "Extended Language" features are enabled.
     *
     * @param fExtendedLanguage  whether "Extended Language" features are enabled
     */
    public void setExtendedLanguage(boolean fExtendedLanguage)
        {
        m_fExtendedLanguage = fExtendedLanguage;
        }

    /**
     * Return whether "Extended Language" features are enabled.
     *
     * @return whether "Extended Language" features are enabled
     */
    public boolean isExtendedLanguageEnabled()
        {
        return m_fExtendedLanguage;
        }

    /**
     * Set the {@link PrintWriter} that can be used by commands to display output.
     *
     * @param writer  the PrintWriter that can be used by commands to display output
     */
    public void setWriter(PrintWriter writer)
        {
        m_writer = writer;
        }

    /**
     * Return the {@link PrintWriter} that can be used by commands to display output.
     *
     * @return the PrintWriter that can be used by commands to display output
     */
    public PrintWriter getWriter()
        {
        return m_writer;
        }

    /**
     * Return the flag indicating whether CohQL should stop processing multiple
     * statements if an error occurs (for example when processing a file), or
     * whether errors should be logged and statement processing continue.
     *
     * @return true if statement processing should stop when errors occur.
     */
    public boolean isStopOnError()
        {
        return m_fStopOnError;
        }

    /**
     * Set the flag indicating whether CohQL should stop processing multiple
     * statements if an error occurs (for example when processing a file), or
     * whether errors should be logged and statement processing continue.
     *
     * @param fStopOnError  true if statement processing should stop when
     *                      errors occur
     */
    public void setStopOnError(boolean fStopOnError)
        {
        m_fStopOnError = fStopOnError;
        }

    /**
     * Set the title that will be displayed as the results heading.
     *
     * @param sTitle  the title that will be displayed as the
     *                results heading
     */
    public void setTitle(String sTitle)
        {
        m_sTitle = sTitle;
        }

    /**
     * Return the String to use for title that heads each result.
     *
     * @return the String to use for title that heads each result
     */
    public String getTitle()
        {
        return m_sTitle;
        }

    /**
     * Set the {@link StatementExecutor} to use to parse and
     * execute statements.
     *
     * @param executor the StatementExecutor to use
     */
    public void setStatementExecutor(StatementExecutor executor)
        {
        Base.azzert(executor != null);
        m_executor = executor;
        }

    /**
     * Return the {@link StatementExecutor} to use to parse and
     * execute statements.
     *
     * @return  the StatementExecutor to use to parse and
     *          execute statements
     */
    public StatementExecutor getStatementExecutor()
        {
        return m_executor;
        }

    /**
     * Instantiate an instance of an {@link OPParser} to parse
     * the CohQL statements provided by the specified {@link Reader}.
     *
     * @param reader  the {@link java.io.Reader} containing the statements to execute
     *
     * @return an instance of an OPParser to parse the CohQL statements
     */
    public OPParser instantiateParser(Reader reader)
        {
        TokenTable tokenTable = m_fExtendedLanguage
                              ? m_language.extendedSqlTokenTable()
                              : m_language.sqlTokenTable();

        return new OPParser(reader, tokenTable, m_language.getOperators());
        }

    /**
     * Return true if the current query session is running in silent mode.
     *
     * @return true if the current session is running in silent mode
     */
    public boolean isSilent()
        {
        return m_fSilent;
        }

    /**
     * Set the flag indicating that the QueryPlus session is running
     * in silent mode.
     *
     * @param fSilent  true to indicate that the QueryPlus session is
     *                 running in silent mode.
     */
    public void setSilentMode(boolean fSilent)
        {
        m_fSilent = fSilent;
        }

    /**
     * Return the {@link BufferedReader} that can be used to obtain user input,
     * typically from {@link System#in}.
     *
     * @return Return the BufferedReader that can be used to obtain user input
     */
    public BufferedReader getReader()
        {
        return m_reader;
        }

    /**
     * Set the {@link BufferedReader} that should be used to obtain user input.
     *
     * @param reader  the BufferedReader to use to obtain user input
     */
    public void setReader(BufferedReader reader)
        {
        m_reader = reader;
        }

    /**
     * Obtain the number of mill-seconds to wait for a {@link Statement} to execute
     * before timing out.
     *
     * @return  the number of mill-seconds to wait for a {@link Statement} to execute
     *          before timing out
     */
    public Duration getTimeout()
        {
        return m_timeout;
        }

    /**
     * Set the {@link Duration} to wait for a {@link Statement} to execute
     * before timing out.
     *
     * @param timeout  the timeout duration
     *
     * @throws IllegalArgumentException if the timeout value is null
     */
    public void setTimeout(Duration timeout)
        {
        if (timeout == null)
            {
            throw new IllegalArgumentException("Timeout duration cannot be null");
            }

        m_timeout = timeout;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ResourceRegistry} used to store various resources
     * used by queries managed by this context.
     */
    protected final ResourceRegistry f_resourceRegistry;

    /**
     * The {@link ConfigurableCacheFactory} to use to access caches.
     */
    protected ConfigurableCacheFactory m_cacheFactory;

    /**
     * The {@link Cluster} to use to access services.
     */
    protected Cluster m_cluster;

    /**
     * Flag that indicates tracing mode.
     */
    protected boolean m_fTrace;

    /**
     * A flag that enables sanity checking.
     */
    protected boolean m_fSanity = true;

    /**
     * Flag that controls whether we except Map, and List as literals.
     * This gives a json like feel to the language.
     */
    protected boolean m_fExtendedLanguage = false;

    /**
     * The {@link CoherenceQueryLanguage} to use.
     */
    protected CoherenceQueryLanguage m_language;

    /**
     * The {@link PrintWriter} that can be used by commands to display output.
     */
    protected PrintWriter m_writer;

    /**
     * A flag indicating when true that CohQL should stop processing multiple
     * statements if an error occurs (for example when processing a file), or
     * when false that errors will be logged and statement processing will continue.
     */
    protected boolean m_fStopOnError = false;

    /**
     * String to use for Title that heads each result displayed.
     */
    protected String m_sTitle = "Results";

    /**
     * The {@link StatementExecutor} to use to execute statements.
     */
    protected StatementExecutor m_executor = new StatementExecutor();

    /**
     * A flag indicating whether the query session is running in silent mode.
     */
    protected boolean m_fSilent = false;

    /**
     * The reader to use to get user input.
     */
    protected BufferedReader m_reader;

    /**
     * The number of milli-seconds to wait for a {@link Statement} to execute
     * before timing out.
     */
    protected Duration m_timeout = new Duration(Long.MAX_VALUE);
    }
