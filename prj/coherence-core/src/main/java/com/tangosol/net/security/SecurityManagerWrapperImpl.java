/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import javax.security.auth.Subject;

import java.lang.reflect.Method;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.function.Supplier;

/**
 * A wrapper to handle the removal of Java's SecurityManger class.
 * <p>
 * This class is here to allow for pre-Java24 code that still uses a SecurityManager
 * or AccessController to work. There post-Java24 version of this class is in
 * the coherence-core-24 module and is built into the multi-release coherence.jar.
 *
 * @author Jonathan Knight 25/01/2025
 */
@SuppressWarnings("removal")
class SecurityManagerWrapperImpl
        implements SecurityManagerWrapper
    {
    SecurityManagerWrapperImpl()
        {
        }

    @Override
    public Subject getCurrentSubject()
        {
        try
            {
            return (currentMethod != null) ? (Subject) currentMethod.invoke(null, (Object[]) null) :
                    Subject.getSubject(AccessController.getContext());
            }
        catch (Exception ignore)
            {
            }
        return null;
        }

    @Override
    public boolean hasSecurityManager()
        {
        return System.getSecurityManager() != null;
        }

    @Override
    public <T> T doIfSecure(PrivilegedAction<T> action)
        {
        if (hasSecurityManager())
            {
            return AccessController.doPrivileged(action);
            }
        return action.run();
        }

    @Override
    public <T> T doIfSecure(PrivilegedExceptionAction<T> action) throws Exception
        {
        if (hasSecurityManager())
            {
            AccessController.doPrivileged(action);
            }
        return action.run();
        }

    @Override
    public void doIfSecure(PrivilegedAction<?> action, Runnable fallback)
        {
        if (hasSecurityManager())
            {
            AccessController.doPrivileged(action);
            }
        fallback.run();
        }

    @Override
    public <T> T doIfSecure(PrivilegedAction<T> action, Supplier<T> fallback)
        {
        if (hasSecurityManager())
            {
            return AccessController.doPrivileged(action);
            }
        return fallback.get();
        }

    @Override
    public <T> T doPrivileged(PrivilegedAction<T> action)
        {
        return AccessController.doPrivileged(action);
        }

    @Override
    public <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException
        {
        return AccessController.doPrivileged(action);
        }

    @Override
    public void checkPermission(Supplier<Permission> supplier)
        {
        checkPermission(supplier.get());
        }

    @Override
    public void checkPermission(Permission permission)
        {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            if (permission != null)
                {
                security.checkPermission(permission);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    static Method currentMethod = null;

    static
        {
        Class<?> c;
        try
            {
            c             = Class.forName("javax.security.auth.Subject");
            currentMethod = c.getMethod("current", (Class<?>[]) null);
            }
        catch (Exception ignore)
            {
            // pre-JDK23
            }
        }
    }
