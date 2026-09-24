/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import java.security.PrivilegedAction;

import java.util.concurrent.CompletionException;

import javax.security.auth.Subject;

/**
 * A helper class to expose the {@link Subject#callAs} call as a privileged action.
 * <p>
 * This class bridges Coherence privileged actions onto a JAAS subject using
 * the Java 21 API while preserving the previous unchecked-exception contract.
 */
public class DoAsAction<T>
        implements PrivilegedAction<T>
    {
    /**
     * Construct a privileged action.
     *
     * @param action  the action to run with privileges of the current subject
     *
     * @since 14.1.2.0.4
     */
    public DoAsAction(PrivilegedAction<T> action)
        {
        this(Subject.current(), action);
        }
    /**
     * Construct a privileged action.
     *
     * @param subject  the subject that the specified action will run as
     * @param action   the action to run with privileges of the specified subject
     */
    public DoAsAction(Subject subject, PrivilegedAction<T> action)
        {
        m_subject = subject;
        m_action  = action;
        }

    @Override
    public T run()
        {
        try
            {
            return Subject.callAs(m_subject, m_action::run);
            }
        catch (CompletionException e)
            {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException)
                {
                throw runtimeException;
                }
            if (cause instanceof Error error)
                {
                throw error;
                }
            throw e;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * The subject that the specified action will run as.
     */
    private Subject m_subject;

    /**
     * The privileged action to be run as the specified subject.
     */
    private PrivilegedAction<T> m_action;
    }
