/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import java.io.PrintWriter;

import java.util.concurrent.CompletableFuture;

/**
 * Implementations of this interface are able to execute CohQL statements,
 * for example a Select, Update, a backup command etc.
 * <p>
 * Each {@link #execute(ExecutionContext) execution} is provided a {@link
 * ExecutionContext context} in which to execute the statement and is obliged
 * to return a {@link StatementResult result} to the caller. Ths allows the
 * caller to invoke statement agnostic operations, which each implementation
 * can specialize based on the format of the results.
 *
 * @author jk  2013.12.09
 * @since Coherence 12.2.1
 *
 * @see StatementResult
 */
public interface Statement
    {
    /**
     * Execute a CohQL query or command and return the relevant {@link
     * StatementResult result}.
     *
     * @param ctx  the {@link ExecutionContext context} to use
     *
     * @return a StatementResult containing the results of executing the statement
     */
    public StatementResult execute(ExecutionContext ctx);

    /**
     * Execute a CohQL query or command asynchronously and return the
     * {@code CompletableFuture} with the relevant {@link StatementResult result}.
     *
     * @param ctx  the {@link ExecutionContext context} to use
     *
     * @return a StatementResult future containing the results of executing the statement
     *
     * @throws UnsupportedOperationException if this statement does not support
     *         asynchronous execution
     */
    public default CompletableFuture<StatementResult> executeAsync(ExecutionContext ctx)
        {
        throw new UnsupportedOperationException();
        }

    // ----- validation methods ---------------------------------------------

    /**
     * Perform sanity checks on the statement that will be executed.
     * <p>
     * Implementations can fail sanity checking by throwing an unchecked exception
     * (RuntimeException).
     *
     * @param ctx  the {@link ExecutionContext context} to use
     *
     * @throws RuntimeException if sanity checking fails
     */
    public void sanityCheck(ExecutionContext ctx);

    /**
     * Output to the provided {@link PrintWriter} a human readable trace of the
     * actions that will be taken by this statement if or when executed.
     *
     * @param out  the PrintWriter to write the trace to
     */
    public void showPlan(PrintWriter out);

    /**
     * Return a string that will be used as a question to confirm execution of
     * a statement. If null is returned then no confirmation is required.
     *
     * @param ctx  the {@link ExecutionContext context} to use
     *
     * @return a String that will be used to confirm execution of a statement
     */
    public String getExecutionConfirmation(ExecutionContext ctx);

    /**
     * Obtain a flag indicating whether this Statement will manage its own
     * timeout handling.
     *
     * @return true if this Statement manages timeout handling or false if
     *         the StatementExecutor should manage timeouts.
     */
    public default boolean isManagingTimeout()
        {
        return false;
        }
    }
