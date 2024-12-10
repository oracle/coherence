/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;

import java.util.List;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link SourceStatement}.
 *
 * @author jk  2013.12.09
 * @since Coherence 12.2.1
 */
public class SourceStatementBuilder
        extends AbstractStatementBuilder<SourceStatementBuilder.SourceStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public SourceStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                   ParameterResolver namedBindVars)
        {
        String sFile = getFile(term);

        if (sFile == null || sFile.isEmpty())
            {
            throw new CohQLException("File name needed for sourcing");
            }

        return new SourceStatement(sFile);
        }

    @Override
    public String getSyntax()
        {
        return "SOURCE FROM [FILE] 'filename'\n@ 'filename'\n. filename";
        }

    @Override
    public String getDescription()
        {
        return "Read and process a file of commands form the file named 'file-name. Each\n"
               + "statement must end with a ';'. The character '@' may be used as an alias for\n"
               + "SOURCE FROM [FILE] as in @ 'filename'. Source files may SOURCE other files.\n"
               + "At the command line only you may also use '.' as an abbreviation for '@' but\n"
               + "do not put quotes around the filename since '.' is processed specially before\n"
               + "the line executed.";
        }

    // ----- inner class: SourceStatement -----------------------------------
    /**
     * Implementation of the CohQL "source" command.
     */
    public static class SourceStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SourceStatement that will execute commands from
         * the specified file.
         *
         * @param sFileName  the file of commands to execute
         */
        public SourceStatement(String sFileName)
            {
            f_sFileName = sFileName;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            return source(f_sFileName, ctx);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("source('%s')", f_sFileName);
            }

        @Override
        public boolean isManagingTimeout()
            {
            return true;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Attempt to parse, build, and execute from the given file of
         * statements tracing on the given PrintWriter if the given flag
         * indicates the need to trace.
         *
         * @param sFileName  a String representing a filename to process
         * @param ctx        the {@link ExecutionContext} to use to execute
         *                   statements
         *
         * @return the StatementResult resulting form the last statement to
         *         be processed in the set of statements from the file
         */
        public StatementResult source(String sFileName, ExecutionContext ctx)
            {
            if (sFileName == null || sFileName.length() == 0)
                {
                return StatementResult.NULL_RESULT;
                }

            Reader reader;

            try
                {
                reader = new BufferedReader(new FileReader(sFileName));
                }
            catch (FileNotFoundException e)
                {
                String sError = "file not found " + sFileName;

                if (ctx.isStopOnError())
                    {
                    throw new CohQLException(sError);
                    }


                traceout(sError, ctx.getWriter(), ctx.isTraceEnabled());

                return StatementResult.NULL_RESULT;
                }

            boolean fSavedSilent = ctx.isSilent();
            boolean fSavedSanity = ctx.isSanityChecking();
            ctx.setSilentMode(true);
            try
                {
                ctx.getStatementExecutor().execute(reader, ctx);
                }
            finally
                {
                ctx.setSilentMode(fSavedSilent);
                ctx.setSanityCheckingEnabled(fSavedSanity);
                }

            return StatementResult.NULL_RESULT;
            }

        /**
         * Write the given line on the give PrintWriter if the trace flag is true.
         *
         * @param sLine   a String to be displayed
         * @param writer  a PrintWriter to write upon
         * @param fTrace  a flag indicating whether to trace
         */
        protected void traceout(String sLine, PrintWriter writer, boolean fTrace)
            {
            if (fTrace && writer != null)
                {
                writer.println(sLine);
                }
            }

        // ----- data members ---------------------------------------------------

        /**
         * The file name to be used in the CohQL "source" command.
         */
        protected final String f_sFileName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of a SourceStatementBuilder.
     */
    public static final SourceStatementBuilder INSTANCE = new SourceStatementBuilder();
    }
