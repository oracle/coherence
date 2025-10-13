/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import java.security.CodeSource;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.Set;
import java.util.function.Supplier;

import javax.security.auth.Subject;


/**
* A collection of security-related utilities.
*
* @author dag 2009.11.16
*/
public abstract class SecurityHelper
    {
    /**
    * Return the Subject from the current security context.
    *
    * @return the current Subject.
    */
    public static Subject getCurrentSubject()
        {
        return s_securityManager.getCurrentSubject();
        }

    public static boolean hasSecurityManager()
        {
        return s_securityManager.hasSecurityManager();
        }

    public static  <T> T doIfSecure(PrivilegedAction<T> action)
        {
        return s_securityManager.doPrivileged(action);
        }

    public static <T> T doIfSecure(PrivilegedExceptionAction<T> action) throws Exception
        {
        return s_securityManager.doPrivileged(action);
        }

    public static void doIfSecure(PrivilegedAction<?> action, Runnable fallback)
        {
        s_securityManager.doIfSecure(action, fallback);
        }

    public static void doIfSecure(Supplier<PrivilegedAction<?>> action, Runnable fallback)
        {
        s_securityManager.doIfSecure(action, fallback);
        }

    public static <T> T doIfSecure(PrivilegedAction<T> action, Supplier<T> fallback)
        {
        return s_securityManager.doIfSecure(action, fallback);
        }

    public static <T> T doIfSecureInDoAsAction(PrivilegedAction<T> action, Supplier<T> fallback)
        {
        return s_securityManager.doIfSecureInDoAsAction(action, fallback);
        }

    public static <T> T doIfSecureInDoAsAction(Supplier<PrivilegedAction<T>> supplier, Supplier<T> fallback)
        {
        return s_securityManager.doIfSecureInDoAsAction(supplier, fallback);
        }

    public static <T> T doIfSecure(Supplier<PrivilegedAction<T>> supplier, Supplier<T> fallback)
        {
        return s_securityManager.doIfSecure(supplier, fallback);
        }

    public static <T> T doPrivileged(PrivilegedAction<T> action)
        {
        return s_securityManager.doPrivileged(action);
        }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException
        {
        return s_securityManager.doPrivileged(action);
        }

    public static void checkPermission(Supplier<Permission> supplier)
        {
        s_securityManager.checkPermission(supplier);
        }

    public static void checkPermission(Permission permission)
        {
        s_securityManager.checkPermission(permission);
        }

    public static CodeSource getCodeSource()
        {
        return s_securityManager.getClass().getProtectionDomain().getCodeSource();
        }

    // ----- constants  -----------------------------------------------------

    /**
     * The {@link SecurityManagerWrapper} to use. This will be either the one in coherence-core
     * or the implementation in coherence-core-24 if running on Java 24 or higher.
     */
    private static final SecurityManagerWrapper s_securityManager = new SecurityManagerWrapperImpl();

    /**
     * A subject that represents nobody.
     */
    public static final Subject EMPTY_SUBJECT = new Subject(true, Set.of(), Set.of(), Set.of());
    }
