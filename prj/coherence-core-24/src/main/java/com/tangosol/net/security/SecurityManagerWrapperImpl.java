/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import javax.security.auth.Subject;

import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.function.Supplier;

/**
 * A wrapper to handle the removal of Java's SecurityManger class.
 * <p>
 * This class is here to allow for Java 24 and above code to work with a SecurityManager
 * or AccessController class. This Java 24 and above version of this class is in
 * the coherence-core-24 module and is built into the multi-release coherence.jar.
 *
 * @author Jonathan Knight 25/01/2025
 */
public class SecurityManagerWrapperImpl
        implements SecurityManagerWrapper
    {
    SecurityManagerWrapperImpl()
        {
        }

    @Override
    public Subject getCurrentSubject()
        {
        return Subject.current();
        }

    @Override
    public boolean hasSecurityManager()
        {
        return false;
        }

    @Override
    public <T> T doIfSecure(PrivilegedAction<T> action)
        {
        return action.run();
        }

    @Override
    public <T> T doIfSecure(PrivilegedExceptionAction<T> action) throws Exception
        {
        return action.run();
        }

    @Override
    public void doIfSecure(PrivilegedAction<?> action, Runnable fallback)
        {
        fallback.run();
        }

    @Override
    public void doIfSecure(Supplier<PrivilegedAction<?>> action, Runnable fallback)
        {
        fallback.run();
        }

    @Override
    public <T> T doIfSecure(PrivilegedAction<T> action, Supplier<T> fallback)
        {
        return fallback.get();
        }

    @Override
    public <T> T doIfSecureInDoAsAction(PrivilegedAction<T> action, Supplier<T> fallback)
        {
        return fallback.get();
        }

    @Override
    public <T> T doIfSecureInDoAsAction(Supplier<PrivilegedAction<T>> supplier, Supplier<T> fallback)
        {
        return fallback.get();
        }

    @Override
    public <T> T doIfSecure(Supplier<PrivilegedAction<T>> action, Supplier<T> fallback)
        {
        return fallback.get();
        }

    @Override
    public <T> T doPrivileged(PrivilegedAction<T> action)
        {
        return action.run();
        }

    @Override
    public <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException
        {
        try
            {
            return action.run();
            }
        catch (Exception e)
            {
            throw new PrivilegedActionException(e);
            }
        }

    @Override
    public void checkPermission(Supplier<Permission> supplier)
        {
        }

    @Override
    public void checkPermission(Permission permission)
        {
        }

    }
