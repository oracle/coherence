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
 * AccessAdapter is an internal, environment-specific component which knows how to execute PrivilegedActions.
 * A common way to execute a PrivilegedAction is to call:
 * <pre>
 *   Subject.doAs(subject, action);
 * </pre>
 * However, some containers require a different way of executing the PrivilegedActions. For example, WLS mandates
 * using its subject stack:
 * <pre>
 *   weblogic.security.Security.runAs(subject, action);
 * </pre>
 * This interface is used as an abstraction that allows the caller not to know the details of the environment-
 * specific execution.
 * 
 * @author dag 2012.03.12
 * @since Coherence 12.1.2
 */
public interface AccessAdapter
    {
    /**
     * Execute the action in the context of the given Subject.
     *
     * @param subject  the Subject representing a previously validated identity
     * @param action   the action to be executed
     */
    public void doAs(Subject subject, PrivilegedAction action);
    }
