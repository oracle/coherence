/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import java.io.PrintWriter;

/**
 * The result of executing a CohQL {@link Statement}.
 * <p>
 * A StatementResult encapsulates the raw result when executing a Statement
 * allowing a generic mechanism for callers to request certain operations
 * such as displaying the result {@link PrintWriter} ({@link
 * #print(PrintWriter, String)}).
 *
 * @author jk 2014.07.15
 * @since Coherence 12.2.1
 */
public interface StatementResult
    {
    /**
     * Return the actual result Object that this StatementResult wraps.
     *
     * @return the actual result Object that this StatementResult wraps
     */
    public Object getResult();

    /**
     * Print the result object to the specified {@link PrintWriter}.
     *
     * @param writer  the PrintWriter to print the results to
     * @param sTitle  the title to print before the results
     */
    public void print(PrintWriter writer, String sTitle);


    // ----- inner class: NullResult ----------------------------------------

    /**
     * A StatementResult with a null result value.
     */
    public StatementResult NULL_RESULT = new StatementResult()
        {
        @Override
        public Object getResult()
            {
            return null;
            }

        @Override
        public void print(PrintWriter writer, String sTitle)
            {
            }
        };
    }
