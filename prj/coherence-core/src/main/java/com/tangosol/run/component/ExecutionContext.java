/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.component;


/**
* This interface is implemented by any class that wishes to be used as an
* execution context.  The interface is designed to support a largely
* stateless implementation by passing the relevant information to each
* method.  Note that some execution contexts, such as those for EJB
* components, could theoretically be re-entrant; design accordingly.
*
* For debugging purposes, classes implementing ExecutionContext should
* implement toString() and support it for the duration of the object's
* existence, including before executionInitialized is invoked and after
* executionTerminated is invoked.
*
* @version 1.00, 2001.04.02
* @author  Cameron Purdy
*/
public interface ExecutionContext
    {
    /**
    * Invoked when the execution context is registered.
    *
    * This transfers the context from an "uninitialized" to a "running"
    * state.
    *
    * @param ctxOuter  the outer ExecutionContext which was suspended,
    *                  or null if there is no outer context
    */
    public void executionInitialized(ExecutionContext ctxOuter);

    /**
    * Invoked when execution transfers from this execution context that is
    * in the "running" state to another execution context that is about to be
    * initialized.
    *
    * This transfers the context from a "running" state to a "suspended"
    * state.
    *
    * @param ctxInner  the ExecutionContext which is going to be initialized
    */
    public void executionSuspended(ExecutionContext ctxInner);

    /**
    * Invoked when execution returns to this execution context.
    *
    * This transfers the context from a "suspended" state to a "running"
    * state.
    *
    * @param ctxInner  the ExecutionContext which was terminated (always the
    *                  same as was passed to executionSuspended)
    */
    public void executionResumed(ExecutionContext ctxInner);

    /**
    * Invoked when the execution context is unregistered.
    *
    * This transfers the context from a "running" to an "uninitialized"
    * state.
    *
    * @param ctxOuter  the ExecutionContext which will be resumed (always the
    *                  same as was passed to executionInitialized)
    */
    public void executionTerminated(ExecutionContext ctxOuter);

    /**
    * Get an outer (parent) context for this execution context
    *
    * @return the outer ExecutionContext or null
    */
    public ExecutionContext getOuterContext();
    }
