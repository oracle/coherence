/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

/**
 * An implementation of a {@link PrivilegedAction} that is used to execute
 * an {@link AccessAdapter#doAs(Subject, PrivilegedAction)} call
 * in the context of a given {@link Subject}.
 *
 * @author jk 2014.04.18
 */
public class AccessAdapterPrivilegedAction
        implements PrivilegedAction
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AccessAdapterPrivilegedAction that will execute
     * the {@link AccessAdapter#doAs(Subject, PrivilegedAction)} call.
     *
     * @param adapter  the {@link AccessAdapter} to call {@link AccessAdapter#doAs(Subject, PrivilegedAction)} on
     * @param subject  the {@link Subject} to pass to the {@link AccessAdapter#doAs(Subject, PrivilegedAction)} call
     * @param action   the {@link PrivilegedAction} to pass to the
     *                 {@link AccessAdapter#doAs(Subject, PrivilegedAction)} call
     */
    public AccessAdapterPrivilegedAction(AccessAdapter adapter, Subject subject, PrivilegedAction action)
        {
        f_adapter = adapter;
        f_subject = subject;
        f_action  = action;
        }

    // ----- PrivilegedAction methods ---------------------------------------

    /**
     * Call the {@link AccessAdapter#doAs(Subject, PrivilegedAction)} method on the
     * {@link AccessAdapter}.
     *
     * @return this method will always return null
     */
    @Override
    public Object run()
        {
        f_adapter.doAs(f_subject, f_action);

        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link AccessAdapter} to call {@link AccessAdapter#doAs(Subject, PrivilegedAction)} on.
     */
    protected final AccessAdapter f_adapter;

    /**
     * The {@link Subject} to pass to the {@link AccessAdapter#doAs(Subject, PrivilegedAction)} call.
     */
    protected final Subject f_subject;

    /**
     * The {@link PrivilegedAction} to pass to the {@link AccessAdapter#doAs(Subject, PrivilegedAction)} call.
     */
    protected final PrivilegedAction f_action;
    }
