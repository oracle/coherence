/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

/**
 * A helper class to expose the {@link Subject#doAs} call as a privileged action.
 * <p>
 * This is a Java21 version of this class which replaces the {@link Subject#doAs},
 * which is deprecated with {@link Subject#callAs} in the coherence-core-21
 * module and is built into the multi-release coherence.jar.
 *
 * Note: In case of Exception, Subject.callAs() wraps the Exception
 * returned by action::run with the java.util.concurrent.CompletionException.
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
        return Subject.callAs(m_subject, m_action::run);
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