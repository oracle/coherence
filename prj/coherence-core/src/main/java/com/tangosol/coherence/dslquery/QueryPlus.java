/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.dslquery.queryplus.AbstractQueryPlusStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.CommandsStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.ExtendedLanguageStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.HelpStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.SanityCheckStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.ServicesStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.SetTimeoutStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.TraceStatementBuilder;
import com.tangosol.coherence.dslquery.queryplus.WheneverStatementBuilder;

import com.tangosol.coherence.dslquery.token.SQLPeekOPToken;

import com.tangosol.coherence.dsltools.precedence.PeekOPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ListMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * QueryPlus implements a simple command line processor for a sql like
 * language.
 * <p>
 * QueryPlus can use JLine for enhanced command-line editing capabilities,
 * such as having the up and down arrows move through the command history.
 * However, JLine is not required. QueryPlus supports JLine when the JLine 3.x library
 * is included in the QueryPlus JVM classpath. If JLine is not found,
 * a message displays and QueryPlus starts without JLine capabilities.
 *
 * @author djl  2009.08.31
 * @author jk   2014.01.02
 */
public class QueryPlus
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a QueryPlus instance that uses the specified {@link Dependencies}.
     *
     * @param dependencies  the Dependencies that will control the QueryPlus session
     */
    public QueryPlus(Dependencies dependencies)
        {
        Base.azzert(dependencies != null);
        f_dependencies = dependencies;
        f_executor     = dependencies.getStatementExecutor();
        f_context      = new ExecutionContext();
        f_fEcho        = dependencies.isEcho();

        f_context.setTimeout(dependencies.getTimeout());
        f_context.setTraceEnabled(dependencies.isTraceEnabled());
        f_context.setSanityCheckingEnabled(dependencies.isSanityChecking());
        f_context.setExtendedLanguage(dependencies.isExtendedLanguageEnabled());
        f_context.setWriter(dependencies.getOutputWriter());
        f_context.setCoherenceQueryLanguage(dependencies.getCoherenceQueryLanguage());
        f_context.setTitle(dependencies.getTitle());
        f_context.setSilentMode(dependencies.isSilent());
        f_context.setReader(dependencies.getReader());

        initializeLanguage();
        }

    /**
     * Run this instance of QueryPlus.
     */
    public void run()
        {
        PrintWriter out      = f_dependencies.getOutputWriter();

        // execute any statements contained in the dependencies
        for (String sStatement : f_dependencies.getStatements())
            {
            if (!evalLine(sStatement))
                {
                break;
                }
            }

        // execute any statement script files contained in the dependencies
        for (String sFile : f_dependencies.getFiles())
            {
            if (!processFile(sFile))
                {
                break;
                }
            }

        out.flush();

        if (f_dependencies.isExitWhenProcessingComplete())
            {
            return;
            }

        if (!f_context.isSilent())
            {
            out.println("Coherence Command Line Tool");
            out.flush();
           }

        repl();
        }

    /**
     * Start a statement processing loop.
     */
    public void repl()
        {
        BufferedReader reader   = f_context.getReader();
        PrintWriter    writer   = f_context.getWriter();
        boolean        fSilent  = f_context.isSilent();
        boolean        fWorking = true;

        while (fWorking)
            {
            try
                {
                if (!fSilent)
                    {
                    if (s_fUsingJline == false)
                        {
                        // JLine readLine manages PROMPT as a parameter.
                        writer.println();
                        writer.print(PROMPT);
                        }
                    }

                writer.flush();

                String sLine = reader.readLine();
                if (sLine == null)
                    {
                    fWorking = false;
                    }
                else
                    {
                    boolean  fContainsEOS = sLine.contains(END_OF_STATEMENT);
                    String[] statements   = sLine.split(END_OF_STATEMENT);

                    for (String statement : statements)
                        {
                        statement = statement.trim();
                        if (statement.isEmpty())
                            {
                            continue;
                            }

                        // equivalent of printing shell commands as they are read.
                        if (f_fEcho)
                            {
                            StringBuffer sb = new StringBuffer(statement);

                            if (fContainsEOS)
                                {
                                sb.append(END_OF_STATEMENT);
                                }
                            writer.println(sb.toString());
                            writer.flush();
                            }
                        fWorking = statement != null && evalLine(statement);
                        }
                    }
                }
            catch (IOException e)
                {
                if (fSilent)
                    {
                    return;
                    }
                else
                    {
                    writer.println("\n" + e.getMessage());
                    }
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the current {@link ExecutionContext} passed to {@link Statement}s.
     *
     * @return the current ExecutionContext passed to Statements
     */
    public ExecutionContext getExecutionContext()
        {
        return f_context;
        }

    /**
     * Initialize the {@link CoherenceQueryLanguage} with any QueryPlus
     * {@link Statement} extensions.
     */
    protected void initializeLanguage()
        {
        // Add the QueryPlus "commands" statement to CohQL
        addStatement(new CommandsStatementBuilder());

        // Add the QueryPlus "extended language" statement to CohQL
        addStatement(new ExtendedLanguageStatementBuilder());

        // Add the QueryPlus "help" statement to CohQL
        addStatement(new HelpStatementBuilder());

        // Add the QueryPlus "sanity check (on/off)" statement to CohQL
        addStatement(new SanityCheckStatementBuilder());

        // Add the QueryPlus "services" statement to CohQL
        addStatement(new ServicesStatementBuilder());

        // Add the QueryPlus "trace on/off" statement to CohQL
        CoherenceQueryLanguage language = f_context.getCoherenceQueryLanguage();
        addStatement(new TraceStatementBuilder(language.sqlTokenTable().lookup("trace")));

        // Add the QueryPlus "whenever" statement to CohQL
        addStatement(new WheneverStatementBuilder());

        // Add the QueryPlus "alter session" statement to CohQL
        addAlterSessionStatement();
        }

    /**
     * Add a new QueryPlus statement.
     *
     * @param builder  the statement builder to add
     */
    protected void addStatement(AbstractQueryPlusStatementBuilder builder)
        {
        AbstractQueryPlusStatementBuilder.AbstractOPToken token    = builder.instantiateOpToken();
        CoherenceQueryLanguage                            language = f_context.getCoherenceQueryLanguage();

        language.addStatement(token.getFunctor(), builder);
        language.extendedSqlTokenTable().addToken(token);
        language.sqlTokenTable().addToken(token);
        }

    /**
     * Add the QueryPlus ALTER SESSION statements
     */
    protected void addAlterSessionStatement()
        {
        CoherenceQueryLanguage                            language    = f_context.getCoherenceQueryLanguage();
        SetTimeoutStatementBuilder                        bldrTimeout = new SetTimeoutStatementBuilder();
        AbstractQueryPlusStatementBuilder.AbstractOPToken tokenTimeout = bldrTimeout.instantiateOpToken();

        // Add the SET TIMEOUT statement to the CohQL language
        language.addStatement(tokenTimeout.getFunctor(), bldrTimeout);

        // Add the tokens to the language
        SQLPeekOPToken tokenSet     = new SQLPeekOPToken("set", tokenTimeout);

        SQLPeekOPToken tokenSession = new SQLPeekOPToken("session", tokenSet);

        TokenTable  tokenTableExt = language.extendedSqlTokenTable();
        PeekOPToken tokenExtAlter = (PeekOPToken) tokenTableExt.lookup("alter");
        tokenExtAlter.addOPToken(tokenSession);

        TokenTable  tokenTableSql = language.sqlTokenTable();
        PeekOPToken tokenSqlAlter = (PeekOPToken) tokenTableSql.lookup("alter");
        tokenSqlAlter.addOPToken(tokenSession);
        }

    /**
     * Process the specified query.
     *
     * @param sQuery  a String that represents the query
     *
     * @return the results of the query
     */
    protected Object query(String sQuery)
        {
        return f_context.getStatementExecutor().execute(new StringReader(sQuery), f_context);
        }

    /**
     * Process the given file of CohQL statements.
     *
     * @param sFileName  the name of the file CohQL containing the
     *                   statements to execute
     *
     * @return true if all statements in the file were processed or
     *         false if an error occurred
     */
    protected boolean processFile(String sFileName)
        {
        String  sLine        = "@ " + "'" + sFileName.trim() + "'";
        boolean fSavedSilent = f_context.isSilent();

        f_context.setSilentMode(true);

        try
            {
            f_context.getStatementExecutor().execute(new StringReader(sLine), f_context);
            }
        catch (Exception e)
            {
            PrintWriter out = f_context.getWriter();

            out.println(e.getMessage());

            if (f_context.isTraceEnabled())
                {
                e.printStackTrace(out);
                }

            if (f_context.isStopOnError())
                {
                return false;
                }
            }
        finally
            {
            f_context.setSilentMode(fSavedSilent);
            }

        return true;
        }

    /**
     * Evaluate the given CohQL statement.
     *
     * @param sLine  the CohQL statement String to be evaluated
     *
     * @return a flag indicating whether to continue processing statements
     */
    protected boolean evalLine(String sLine)
        {
        try
            {
            if (sLine.trim().isEmpty())
                {
                return true;
                }

            if (sLine.equals("quit") || sLine.equals("bye"))
                {
                return false;
                }

            if (sLine.startsWith("."))
                {
                String ln = sLine.substring(1).trim();

                sLine = "@ " + "'" + ln + "'";
                }

            query(sLine);
            }
        catch (Exception e)
            {
            PrintWriter writer = f_context.getWriter();

            writer.println(e.getMessage());

            if (f_context.isTraceEnabled())
                {
                e.printStackTrace(writer);
                }

            if(f_context.isStopOnError())
                {
                return false;
                }
            }

        return true;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return an instance of BufferedReader if JLine is present.
     *
     * @param output   the {@link OutputStream} that will provide output
     * @param input    the {@link InputStream} that will provide input
     * @param fSilent  if true no message will be displayed if JLine is unavailable.
     *
     * @return an instance of BufferedReader or null
     */
    public static BufferedReader getJlineReader(OutputStream output, InputStream input, boolean fSilent)
        {
        try
            {
            Class<?> clzJLineReader     = Class.forName("org.jline.reader.LineReader");
            Class<?> clzJLineReaderBldr = Class.forName("org.jline.reader.LineReaderBuilder");
            Object   builder            = ClassHelper.invokeStatic(clzJLineReaderBldr, "builder", null);
            String   fieldHistoryFile   = (String) clzJLineReader.getField("HISTORY_FILE").get(clzJLineReader);

            File fileHistory = new File(".cohql-history");
            if (!fileHistory.exists())
                {
                fileHistory.createNewFile();
                }

            builder = ClassHelper.invoke(builder, "variable", new Object[] {fieldHistoryFile, fileHistory});
            Object jlineReader = ClassHelper.invoke(builder, "build", null);

            s_fUsingJline = true;
            return new BufferedReader(new InputStreamReader(input))
                {
                @Override
                public String readLine()
                        throws IOException
                    {
                    try
                        {
                        // to get up arrow with jline console history to work properly,
                        // provide prompt to JLine readLine.
                        String sLine = (String) ClassHelper.invoke(jlineReader, "readLine", new Object[] {PROMPT});
                        ClassHelper.invoke(jlineReader, "redrawLine", null);
                        ClassHelper.invoke(jlineReader, "flush", null);
                        return sLine;
                        }
                    catch (Throwable e)
                        {
                        Throwable eCause     = e.getCause();
                        String    eCauseName = eCause == null ? "" : eCause.getClass().getName();
                        if (e.getClass().getName().contains("InvocationTargetException"))
                            if (eCauseName.contains("EndOfFileException") || eCauseName.contains("org.jline.reader.UserInterruptException"))
                                {
                                // JLine 3.x JLineReader.readLine() method throws exceptions for EOF and ^C by console user.
                                // Method being overridden, java.BufferedReader#readLine(), is documented to return null on eof.
                                return null;
                            }
                        throw Base.ensureRuntimeException(e);
                        }
                    }
                };
            }
        catch (Exception e)
            {
            if (!fSilent)
                {
                PrintWriter writer = new PrintWriter(output);
                writer.println("jline library cannot be loaded, so you cannot "
                        + "use the arrow keys for line editing and history.");
                writer.flush();
                }

            return null;
            }
        }

    /**
     * The main application for CohQL statement processing.
     *
     * @param asArgs  an array of Strings that represents arguments
     */
    public static void main(String[] asArgs)
        {
        PrintWriter            writer   = new PrintWriter(System.out);
        CoherenceQueryLanguage language = new CoherenceQueryLanguage();
        QueryPlus.Dependencies deps     = DependenciesHelper.newInstance(writer, System.in, language, asArgs);

        if (deps == null)
            {
            return;
            }

        new QueryPlus(deps).run();
        }

    // ----- inner interface: Dependencies ----------------------------------

    /**
     * The Dependencies for QueryPlus.
     */
    public interface Dependencies
        {
        /**
         * Return an instance of {@link CoherenceQueryLanguage} to be
         * used by the QueryPlus session.
         *
         * @return the instance of CoherenceQueryLanguage to be
         *         used by the QueryPlus session
         */
        public CoherenceQueryLanguage getCoherenceQueryLanguage();

        /**
         * Return an instance of a {@link PrintWriter} that should be
         * used to display query output.
         *
         * @return the current PrintWriter to be used to display output
         */
        public PrintWriter getOutputWriter();

        /**
         * Return whether trace is enabled. Enabling trace displays verbose
         * output when executing statements.
         *
         * @return true if trace is enabled, otherwise returns false
         */
        public boolean isTraceEnabled();

        /**
         * Return whether sanity checking should be enabled when executing statements.
         *
         * @return whether sanity checking should be enabled when executing statements
         */
        public boolean isSanityChecking();

        /**
         * Return whether "Extended Language" features are enabled.
         *
         * @return whether "Extended Language" features are enabled
         */
        public boolean isExtendedLanguageEnabled();

        /**
         * Return whether the QueryPlus session should exit once all of the statements
         * added to the statements list have been executed.
         *
         * @return whether the QueryPlus session should exit once all of the statements
         *         added to the statements list have been executed
         */
        public boolean isExitWhenProcessingComplete();

        /**
         * Return true if echo is on, resulting in each input statement being written to output as it is read.
         * Default is false.
         *
         * @return if echo is on.
         *
         * @since 24.09
         */
        public boolean isEcho();

        /**
         * Return the list of statements that should be executed prior to the
         * start of the CohQL session.
         *
         * @return the list of statements that should be executed prior to the
         *         start of the CohQL session
         */
        public List<String> getStatements();

        /**
         * Return the list of statement script files that should be executed prior to the
         * start of the CohQL session.
         *
         * @return the list of statements that should be executed prior to the
         *         start of the CohQL session
         */
        public List<String> getFiles();

        /**
         * Return true if the current query session is running in silent mode.
         *
         * @return true if the current session is running in silent mode
         */
        public boolean isSilent();

        /**
         * Return the String to use for title that heads each result.
         *
         * @return the String to use for title that heads each result
         */
        public String getTitle();

        /**
         * Return the {@link BufferedReader} to use to obtain user input.
         *
         * @return the BufferedReader to use to obtain user input
         */
        public BufferedReader getReader();

        /**
         * Return the {@link StatementExecutor} to use to parse and
         * execute statements.
         *
         * @return  the StatementExecutor to use to parse and
         *          execute statements
         */
        public StatementExecutor getStatementExecutor();

        /**
         * Return the name of the optional GAR file to load
         * before running QueryPlus.
         *
         * @return the name of the optional GAR file to load
         *         before running QueryPlus
         */
        public String getGarFileName();

        /**
         * Return the optional application name to use if
         * loading a GAR file.
         *
         * @return  the optional application name to use if
         *          loading a GAR file
         */
        public String getApplicationName();

        /**
         * Return the optional array of domain partition names
         * to use if loading a GAR file.
         *
         * @return the optional array of domain partition names
         *         to use if loading a GAR file
         */
        public String[] getDomainPartitions();

        /**
         * Return the initial value that will be set as the
         * CohQL statement timeout.
         *
         * @return the initial value that will be set as the
         *         CohQL statement timeout.
         */
        public Duration getTimeout();
    }

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * A default implementation of {@link QueryPlus.Dependencies}.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a DefaultDependencies instance that will use the specified
         * {@link PrintWriter} and {@link BufferedReader} for output and input.
         *
         * @param writer    the PrintWriter to use to display output
         * @param reader    the reader to obtain user input
         * @param language  an instance of CoherenceQueryLanguage
         */
        public DefaultDependencies(PrintWriter writer, BufferedReader reader, CoherenceQueryLanguage language)
            {
            f_writer   = writer;
            m_reader   = reader;
            f_language = language;
            }

        // ----- QueryPlus.Dependencies interface ---------------------------

        @Override
        public CoherenceQueryLanguage getCoherenceQueryLanguage()
            {
            return f_language;
            }

        @Override
        public PrintWriter getOutputWriter()
            {
            return f_writer;
            }

        /**
         * Set whether trace logging is enabled.
         *
         * @param fTraceEnabled  is trace logging enabled
         */
        public void setTraceEnabled(boolean fTraceEnabled)
            {
            m_fTraceEnabled = fTraceEnabled;
            }

        @Override
        public boolean isTraceEnabled()
            {
            return m_fTraceEnabled;
            }

        /**
         * Set whether sanity checking is enabled.
         *
         * @param fSanity  is sanity checking enabled
         */
        public void setSanityCheckingEnabled(boolean fSanity)
            {
            m_fSanity = fSanity;
            }

        @Override
        public boolean isSanityChecking()
            {
            return m_fSanity;
            }

        /**
         * Set echo mode.
         *
         * @since 24.09
         */
        public void setEcho(boolean fEcho)
            {
            m_fEcho = fEcho;
            }

        @Override
        public boolean isEcho()
            {
            return m_fEcho;
            }

        /**
         * Set whether extended language features should be enabled.
         *
         * @param fExtendedLanguage  whether extended language features should be enabled
         */
        public void setExtendedLanguage(boolean fExtendedLanguage)
            {
            m_fExtendedLanguage = fExtendedLanguage;
            }

        @Override
        public boolean isExtendedLanguageEnabled()
            {
            return m_fExtendedLanguage;
            }

        /**
         * Set the flag that indicates the QueryPlus process should exit
         * after processing the statements from the command line.
         *
         * @param fExit  true if QueryPlus should exit after processing the
         *               command line statements
         */
        public void setExitWhenProcessingComplete(boolean fExit)
            {
            m_fExitWhenProcessingComplete = fExit;
            }

        @Override
        public boolean isExitWhenProcessingComplete()
            {
            return m_fExitWhenProcessingComplete;
            }

        /**
         * Set the list of statements to execute before the QueryPlus
         * session starts.
         *
         * @param listStatements  the list of statements to execute before the
         *                        QueryPlus session starts
         */
        public void setStatements(List<String> listStatements)
            {
            m_listStatements = listStatements;
            }

        @Override
        public List<String> getStatements()
            {
            return m_listStatements;
            }

        /**
         * Set the list of statement files to execute before the QueryPlus
         * session starts.
         *
         * @param listFiles  the list of flies of QueryPlus statements
         *                   to execute before the QueryPlus session starts.
         */
        public void setFiles(List<String> listFiles)
            {
            m_listFiles = listFiles;
            }

        @Override
        public List<String> getFiles()
            {
            return m_listFiles;
            }

        @Override
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
         * Set the title that will be displayed as the results heading.
         *
         * @param sTitle  the title that will be displayed as the
         *                results heading
         */
        public void setTitle(String sTitle)
            {
            m_sTitle = sTitle;
            }

        @Override
        public String getTitle()
            {
            return m_sTitle;
            }

        @Override
        public BufferedReader getReader()
            {
            return m_reader;
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

        @Override
        public StatementExecutor getStatementExecutor()
            {
            return m_executor;
            }

        /**
         * Set the name of the GAR file to load before
         * starting the QueryPlus session. This name
         * should point to an existing GAR file or an
         * exploded GAR file directory.
         *
         * @param sGarFile  the name of the GAR file to load
         */
        public void setGarFileName(String sGarFile)
            {
            m_sGarFileName = sGarFile;
            }

        @Override
        public String getGarFileName()
            {
            return m_sGarFileName;
            }

        /**
         * Set the application name to use. This name only applies
         * if the {@link #m_sGarFileName} has also been set.
         *
         * @param sApplicationName  the application name to use
         */
        public void setApplicationName(String sApplicationName)
            {
            m_sApplicationName = sApplicationName;
            }

        @Override
        public String getApplicationName()
            {
            return m_sApplicationName;
            }

        /**
         * Set the array of domain partition names to use.
         * This list only applies if the {@link #m_sGarFileName}
         * has also been set.
         *
         * @param asDomainPartitions  the comma delimited list of domain partition names
         */
        public void setDomainPartitions(String[] asDomainPartitions)
            {
            m_sDomainPartitions = asDomainPartitions;
            }

        @Override
        public String[] getDomainPartitions()
            {
            return m_sDomainPartitions;
            }

        /**
         * Set the timeout value for CohQL statement execution.
         *
         * @param timeout  timeout value for CohQL statement execution
         */
        public void setTimeout(Duration timeout)
            {
            m_timeout = timeout;
            }

        @Override
        public Duration getTimeout()
            {
            return m_timeout;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link CoherenceQueryLanguage} to use.
         */
        protected final CoherenceQueryLanguage f_language;

        /**
         * Flag that indicates tracing mode.
         */
        protected boolean m_fTraceEnabled;

        /**
         * A flag that enables sanity checking.
         */
        protected boolean m_fSanity = false;

        /**
         * Flag that controls whether we except Map, and List as literals.
         * This gives a json like feel to the language.
         */
        protected boolean m_fExtendedLanguage = false;

        /**
         * The {@link PrintWriter} to use to display output.
         */
        protected final PrintWriter f_writer;

        /**
         * A flag indicating whether the query session is running in silent mode.
         */
        protected boolean m_fSilent = false;

        /**
         * String to use for Title that heads each result displayed.
         */
        protected String m_sTitle = "Results";

        /**
         * A flag indicating whether a CohQL session should be exited when the list of
         * statements has been executed.
         */
        protected boolean m_fExitWhenProcessingComplete;

        /**
         * A list of statements to execute when the CohQL session starts.
         */
        protected List<String> m_listStatements = new LinkedList<>();

        /**
         * A list of statement script files to execute when the CohQL session starts.
         */
        protected List<String> m_listFiles = new LinkedList<>();

        /**
         * The {@link BufferedReader} to use to obtain user input.
         */
        protected BufferedReader m_reader;

        /**
         * The {@link StatementExecutor} to use to execute statements.
         */
        protected StatementExecutor m_executor = new StatementExecutor();

        /**
         * The name of an optional GAR file to load.
         */
        protected String m_sGarFileName;

        /**
         * An optional application name to use. This is only used in combination
         * with the GAR file named in {@link #m_sGarFileName}.
         */
        protected String m_sApplicationName;

        /**
         * A comma delimited list of domain partition names. This is only used in
         * combination with the GAR file named in {@link #m_sGarFileName}.
         */
        protected String[] m_sDomainPartitions;

        /**
         * The timeout value to use for CohQL statement execution.
         */
        protected Duration m_timeout = new Duration(1, Duration.Magnitude.MINUTE);

        /**
         * When {@code true}, echo input statement to output as it is read. Defaults to {@code false}.
         *
         * @since 24.09
         */
        protected boolean m_fEcho = false;
        }

    // ----- inner class: DependenciesHelper --------------------------------

    /**
     * The DependenciesHelper provides helper method for constructing
     * {@link Dependencies} implementations for {@link QueryPlus}.
     */
    public static class DependenciesHelper
        {
        /**
         * Create a new instance of {@link Dependencies}.
         * <p>
         * If the JLine library is present on the classpath and the -nojline argument
         * is not passed in the asArgs array then the specified {@link InputStream}
         * will be wrapped in a jline.ConsoleReaderInputStream.
         *
         * @param writer       the PrintWriter to use to display output
         * @param inputStream  the InputStream that will be used to supply input to QueryPlus
         * @param asArgs       the command line arguments to use to configure the dependencies
         * @param language     the instance of {link CoherenceQueryLanguage} to be used by QueryPlus
         *
         * @return a new instance of Dependencies
         */
        public static Dependencies newInstance(PrintWriter writer, InputStream inputStream,
                                               CoherenceQueryLanguage language, String[] asArgs)
            {
            String[] asValidArgs = new String[]{"c", "e", "extend", "help", "f", "l", "s", "t", "trace", "nojline",
                    "g", "a", "dp", "timeout", "jlinedebug", "v"};

            try
                {
                ListMap map               = CommandLineTool.parseArguments(asArgs, asValidArgs, false);
                boolean fExitOnCompletion = map.containsKey("c");
                boolean fSilent           = map.containsKey("s");
                BufferedReader reader     = null;

                if (map.containsKey("help"))
                    {
                    usage(writer);
                    writer.flush();
                    return null;
                    }

                if (!fExitOnCompletion && !map.containsKey("nojline"))
                    {
                    if (map.containsKey("jlinedebug"))
                        {
                        // enable debug JLine dumbterminal warning
                        final Logger logger = Logger.getLogger("org.jline");

                        logger.setLevel(Level.FINER);
                        }
                    reader = getJlineReader(System.out, inputStream, fSilent);
                    }

                if (reader == null)
                    {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    }

                DefaultDependencies deps   = new DefaultDependencies(writer, reader, language);

                deps.setExitWhenProcessingComplete(fExitOnCompletion);
                deps.setExtendedLanguage(map.containsKey("e") || map.containsKey("extend"));

                if (map.containsKey("f"))
                    {
                    Object oFiles = map.get("f");
                    deps.setFiles(oFiles instanceof List
                            ? (List) oFiles : Collections.singletonList((String) oFiles));
                    }

                if (map.containsKey("l"))
                    {
                    Object oStatements = map.get("l");
                    deps.setStatements(oStatements instanceof List
                            ? (List) oStatements : Collections.singletonList((String) oStatements));
                    }

                deps.setSilentMode(fSilent);
                deps.setTraceEnabled(map.containsKey("t") || map.containsKey("trace"));

                if (map.containsKey("g"))
                    {
                    deps.setGarFileName((String) map.get("g"));
                    deps.setApplicationName((String) map.get("a"));
                    }


                if (map.containsKey("dp"))
                    {
                    String sDomainPartitions = (String) map.get("dp");
                    deps.setDomainPartitions(sDomainPartitions.split(","));
                    }

                if (map.containsKey("timeout"))
                    {
                    String sTimeout = (String) map.get("timeout");
                    if (sTimeout.matches("\\d+$"))
                        {
                        deps.setTimeout(new Duration(sTimeout, Duration.Magnitude.MILLI));
                        }
                    else
                        {
                        throw new IllegalArgumentException("Invalid timeout value");
                        }
                    }

                if (map.containsKey("v"))
                    {
                    deps.setEcho(true);
                    }
                return deps;
                }
            catch (IllegalArgumentException e)
                {
                usage(writer);
                writer.flush();
                throw e;
                }
            }

        /**
         * Print the command line usage message to the specified writer.
         *
         * @param writer  the {@link PrintWriter} to print the usage message to
         */
        public static void usage(PrintWriter writer)
            {
            writer.println("java "
                    + QueryPlus.class.getCanonicalName() + " [-t] [-c] [-s] [-e] [-v] [-help] [-l <cmd>]*\n"
                    + "    [-f <file>]* [-g <garFile>] [-a <appName>] [-dp <parition-list>] [-timeout <value>] [-nojline] [-jlinedebug]");

            /**
             * The lines below should try not to exceed 80 characters
             *   --------------------------------------------------------------------------------
             */
            writer.println(
                "\nCommand Line Arguments:\n" +
                "-a               the application name to use. Used in combination with the -g\n" +
                "                 argument.\n" +
                "-c               exit when command line processing is finished\n" +
                "-e               or -extend \n" +
                "                 extended language mode.  Allows object literals in update and\n" +
                "                 insert statements.\n" +
                "                 elements between '[' and']'denote an ArrayList.\n" +
                "                 elements between '{' and'}'denote a HashSet.\n" +
                "                 elements between '{' and'}'with key/value pairs separated by\n" +
                "                 ':' denotes a HashMap. A literal HashMap  preceded by a class\n" +
                "                 name are processed by calling a zero argument constructor then\n" +
                "                 followed by each pair key being turned into a setter and\n" +
                "                 invoked with the value.\n" +
                "-f <value>       Each instance of -f followed by a filename load one file of\n" +
                "                 statements.\n" +
                "-g <value>       An optional GAR file to load before running QueryPlus.\n" +
                "                 If the -a argument is not used the application name will be the\n" +
                "                 GAR file name without the parent directory name.\n" +
                "-help            Print command line arguments documention.\n" +
                "-l <value>       Each instance of -l followed by a statement will execute one\n" +
                "                 statement.\n" +
                "-s               silent mode. Suppress prompts and result headings, read from\n" +
                "                 stdin and write to stdout. Useful for use in pipes or filters\n" +
                "-t               or -trace \n" +
                "                 turn on tracing. This shows information useful for debugging\n" +
                "-dp <list>       A comma delimited list of domain partition names to use.\n" +
                "                 On start-up the first domain partition in the list will be the\n" +
                "                 current partition. The -dp argument is only applicable in\n" +
                "                 combination with the -g argument.\n" +
                "-timeout <value> Specifies the timeout value for CohQL statements in\n" +
                "                 milli-seconds.\n" +
                "-v               verbose mode. Echo each input statement as it is read.\n" +
                "-nojline         do not use jline console\n" +
                "-jlinedebug      enable FINER logging to debug jline\n");
            }
        }
    // ----- constants ------------------------------------------------------

    /**
     * End of statement constant.
     *
     * @since 24.09
     */
    private static final String END_OF_STATEMENT = ";";

    /**
     * CohQL console prompt.
     *
     * @since 24.09
     */
    private static final String PROMPT = "CohQL> ";

    // ----- data members ---------------------------------------------------

    /**
     * The {@link QueryPlus.Dependencies} configuring this query session.
     */
    protected final Dependencies f_dependencies;

    /**
     * The {@link ExecutionContext} that will be passed to {@link Statement}s.
     */
    protected final ExecutionContext f_context;

    /**
     * The {@link StatementExecutor} to use to execute statements.
     */
    protected final StatementExecutor f_executor;

    /**
     * {@code true} iff input statement should be echoed to output as it is read.
     * Equivalent to {@code echo on} when running an OS shell script.
     * Defaults to {@code false}.
     *
     * @since 24.09
     */
    protected final boolean f_fEcho;

    /**
     * Set to {@code true} when using JLine console.
     *
     * @since 24.09
     */
    protected static boolean s_fUsingJline = false;
    }
