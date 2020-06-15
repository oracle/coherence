/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

/**
 * A helper class to expose the {@link Subject#doAs} call as a privileged action.
 * <p>
 * This class is used to work around a JAAS implementation issue causing
 * {@link Subject#getSubject} not to return the Subject associated with the
 * thread of execution inside of a {@link java.security.AccessController#doPrivileged}
 * code block.
 */
public class DoAsAction<T>
        implements PrivilegedAction<T>
    {
    /**
     * Construct a privileged action.
     *
     * @param action  the action to run with privileges of the current subject
     */
    public DoAsAction(PrivilegedAction<T> action)
        {
        this(SecurityHelper.getCurrentSubject(), action);
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
        return Subject.doAs(m_subject, m_action);
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