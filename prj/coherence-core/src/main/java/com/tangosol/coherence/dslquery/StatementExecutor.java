/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.oracle.coherence.common.base.Timeout;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.dsltools.precedence.EndOfStatementOPToken;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.util.Base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * A class that parses and executes CohQL statements read from
 * an instance of a {@link Reader}.
 *
 * @author jk 2014.08.06
 */
public class StatementExecutor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a StatementExecutor
     */
    public StatementExecutor()
        {
        }

    // ----- StatementExecutor methods --------------------------------------

    /**
     * Parse and execute all of the CohQL statements read from the specified
     * {@link Reader}.
     *
     * @param reader  the {@link Reader} containing the statements to execute
     * @param ctx     the {@link ExecutionContext} that will be used
     *
     * @return the result of the last statement executed
     */
    public Object execute(Reader reader, ExecutionContext ctx)
        {
        CoherenceQueryLanguage language   = ctx.getCoherenceQueryLanguage();
        OPParser               parser     = ctx.instantiateParser(reader);
        OPScanner              scanner    = parser.getScanner();
        PrintWriter            out        = ctx.getWriter();
        Object                 oResult    = null;
        boolean                fTrace     = ctx.isTraceEnabled();

        do
            try
                {
                // make sure we skip over any terminator from the previous statement
                scanner.advanceWhenMatching(EndOfStatementOPToken.INSTANCE.getValue());

                // If we have finished all the tokens then exit
                if (scanner.isEnd())
                    {
                    break;
                    }

                boolean fShowPlan = false;
                boolean fExecute = true;

                // Check whether we are doing a show plan statement
                while (scanner.matches("show") || scanner.matches("plan"))
                    {
                    scanner.advance();
                    fShowPlan = true;
                    }

                // Get the statement AST from the parser
                Term term = parser.parse();

                if (fTrace)
                    {
                    out.println("\nParsed: " + term);
                    }

                // Get the Statement for the AST
                Statement statement = language.prepareStatement((NodeTerm) term, ctx, null, null);

                if (fShowPlan || fTrace)
                    {
                    out.print("plan: ");
                    statement.showPlan(out);
                    out.println();

                    if (fShowPlan)
                        {
                        continue;
                        }
                    }

                if (ctx.isSanityChecking())
                    {
                    statement.sanityCheck(ctx);
                    }

                // check to see if this statement has a confirmation requirement
                // but only display confirmation if we are not in silent mode
                String sConfirmation = statement.getExecutionConfirmation(ctx);
                if (sConfirmation != null && !ctx.isSilent())
                    {
                    fExecute = confirmExecution(ctx.getReader(), out, sConfirmation);
                    }

                if (fExecute)
                    {
                    StatementResult result;

                    if (statement.isManagingTimeout())
                        {
                        // Execute the statement
                        result = statement.execute(ctx);
                        }
                    else
                        {
                        try (Timeout t = Timeout.after(ctx.getTimeout().as(Duration.Magnitude.MILLI)))
                            {
                            // Execute the statement
                            result = statement.execute(ctx);
                            }
                        }

                    // Print the result
                    String sTitle = ctx.isSilent() ? null : ctx.getTitle();

                    result.print(out, sTitle);

                    oResult = result.getResult();
                    }
                else
                    {
                    oResult = null;
                    }
                }
            catch (Throwable e)
                {
                if (fTrace)
                    {
                    out.println("\nCallstack for Exception: " + Base.printStackTrace(e));
                    }
                if (ctx.isStopOnError())
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                String sError = "Error: " + e.getMessage();
                out.println(sError);
                }
        while (!scanner.isEnd());

        return oResult;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Confirm execution of a {@link Statement} which has a getExecutionConfirmation()
     * return a non null value.
     *
     * @param reader  the {@link BufferedReader} to read responses from
     * @param out     the {@link PrintWriter} to write messages
     * @param sPrompt the prompt to display
     *
     * @return true if 'Y' or 'y' was entered otherwise false
     */
    private boolean confirmExecution(BufferedReader reader, PrintWriter out, String sPrompt)
        {
        String sLine;
        try
            {
            while (true)
                {
                out.print(sPrompt);
                out.flush();
                sLine = reader.readLine();
                if (sLine == null)
                    {
                    out.println("Please answer either y or n");
                    }
                else
                    {
                    return (sLine.equals("y") || sLine.equals("Y"));
                    }
                }
            }
        catch (IOException ioe)
            {
            throw new CohQLException("IOException reading confirmation " + ioe.getMessage());
            }
        }
    }
