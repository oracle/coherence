/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package dslquery;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.QueryPlus;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.util.Base;

import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.Arrays;
import java.util.LinkedList;


/**
 * This class runs instances of QueryPlus and allows
 * commands to be piped in and output to be read.
 *
 * @author jk  2014.02.14
 */
public class QueryPlusRunner
        extends ExternalResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new QueryPlusRunner
     */
    public QueryPlusRunner()
        {
        this(null);
        }

    public QueryPlusRunner(String[] asArgs)
        {
        if (asArgs == null || asArgs.length == 0)
            {
            m_asArgs = new String[] {"-nojline"};
            }
        else if (Arrays.asList(asArgs).contains("-nojline"))
            {
            m_asArgs = asArgs;
            }
        else
            {
            m_asArgs = new String[asArgs.length + 1];
            m_asArgs[0] = "-nojline";
            System.arraycopy(asArgs, 0, m_asArgs, 1, asArgs.length);
            }
        }

// ----- methods --------------------------------------------------------

    public void setTrace(boolean flag)
        {
        m_executionContext.setTraceEnabled(flag);
        }

    public boolean isTraceEnabled()
        {
        return m_executionContext.isTraceEnabled();
        }

    public void setSanityCheck(boolean flag)
        {
        m_executionContext.setSanityCheckingEnabled(flag);
        }

    public boolean isSanityCheckEnabled()
        {
        return m_executionContext.isSanityChecking();
        }

    public void setExtendedLanguage(boolean flag)
        {
        m_executionContext.setExtendedLanguage(flag);
        }

    public boolean isExtendedLanguageEnabled()
        {
        return m_executionContext.isExtendedLanguageEnabled();
        }

    public CoherenceQueryLanguage getLanguage()
        {
        return m_executionContext.getCoherenceQueryLanguage();
        }

    public void setConfigurableCacheFactory(ConfigurableCacheFactory ccf)
        {
        m_executionContext.setCacheFactory(ccf);
        }

    /**
     * Execute the specified QueryPlus command
     *
     * @param query the command to execute
     *
     * @return the output displayed by QueryPlus as a list of lines
     *
     * @throws Exception
     */
    public LinkedList<String> runCommand(String query)
            throws Exception
        {
        m_inputWriter.write(query);
        if (!query.endsWith("\n"))
            {
            m_inputWriter.newLine();
            }
        m_inputWriter.flush();

        String             output = readOuput();
        LinkedList<String> lines  = new LinkedList<String>();
        BufferedReader     reader = new BufferedReader(new StringReader(output));
        String             line   = reader.readLine();

        while (line != null)
            {
            lines.add(line);
            line = reader.readLine();
            }
        return lines;
        }

    private String readOuput()
            throws Exception
        {
        m_queryPlusWriter.flush();
        StringBuffer buffer = m_outputReader.getBuffer();
        while (buffer.length() == 0)
            {
            Blocking.sleep(10);
            m_queryPlusWriter.flush();
            }
        StringBuilder out = new StringBuilder();
        String output;

        do
            {
            m_queryPlusWriter.flush();
            synchronized (buffer) // block writter from adding while we copy and truncate in order to avoid over truncation
                {
                output = buffer.toString();
                out.append(output);
                buffer.setLength(0); // truncate
                }
            }
        while(!output.endsWith(PROMPT_LINE));

        return out.toString();
        }

    // ----- ExternalResource methods ---------------------------------------

    /**
     * Called by JUnit to start QueryPlus.
     *
     * @throws Throwable
     */
    @Override
    protected void before()
            throws Throwable
        {
        final PipedOutputStream inputWriter = new PipedOutputStream();
        final PipedInputStream  inputReader = new PipedInputStream(inputWriter);

        m_outputReader    = new StringWriter();
        m_queryPlusWriter = new PrintWriter(m_outputReader);
        m_inputWriter     = new BufferedWriter(new OutputStreamWriter(inputWriter));

        m_queryPlusThread = new Thread(new Runnable()
        {
        @Override
        public void run()
            {
            try
                {
                PrintWriter            queryWriter  = new PrintWriter(m_queryPlusWriter);
                CoherenceQueryLanguage language     = new CoherenceQueryLanguage();

                QueryPlus.Dependencies dependencies = QueryPlus.DependenciesHelper.newInstance(queryWriter,
                                                                                               inputReader,
                                                                                               language,
                                                                                               m_asArgs);

                m_queryPlus        = new QueryPlus(dependencies);
                m_executionContext = m_queryPlus.getExecutionContext();
                m_queryPlus.run();
                }
            catch (Exception e)
                {
                e.printStackTrace();
                }
            System.out.println("Input Ended");
            }
        });

        m_queryPlusThread.setDaemon(true);
        m_queryPlusThread.start();

        readOuput();
        }

    /**
     * Called by JUnit to stop QueryPlus.
     *
     * @throws Throwable
     */
    @Override
    protected void after()
        {
        try
            {
            if (m_queryPlus != null)
                {
                m_inputWriter.close();
                m_queryPlusThread.join();
                m_queryPlusThread = null;
                m_queryPlus = null;
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- member fields --------------------------------------------------

    protected ExecutionContext m_executionContext;
    protected QueryPlus        m_queryPlus;
    protected BufferedWriter   m_inputWriter;
    protected PrintWriter      m_queryPlusWriter;
    protected StringWriter     m_outputReader;
    protected Thread           m_queryPlusThread;
    protected String[]         m_asArgs;

    public static final String PROMPT_LINE = "CohQL> ";
    }
