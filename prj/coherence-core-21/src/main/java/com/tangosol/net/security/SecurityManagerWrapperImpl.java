/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import javax.security.auth.Subject;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.function.Supplier;

/**
 * A wrapper to handle the removal of Java's SecurityManger class.
 * <p>
 * This is a Java21 version of this class in the coherence-core-21
 * module and is built into the multi-release coherence.jar.
 * <p>
 * Note, In case of Exception, Subject.callAs() wraps the Exception
 * returned by action::run with the java.util.concurrent.CompletionException.
 *
 * @author Jonathan Knight, lh 26/06/2025
 * @since 14.1.2.0.4
 */
@SuppressWarnings("removal")
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
        return System.getSecurityManager() != null;
        }

    @Override
    public <T> T doIfSecure(PrivilegedAction<T> action)
        {
        if (hasSecurityManager())
            {
            if (action instanceof DoAsAction)
                {
                return AccessController.doPrivileged(action);
                }

            Subject subject = getCurrentSubject();
            return AccessController.doPrivileged((PrivilegedAction<T>) () ->
                    Subject.callAs(subject, action::run));
            }
        return action.run();
        }

    @Override
    public <T> T doIfSecure(PrivilegedExceptionAction<T> action) throws Exception
        {
        if (hasSecurityManager())
            {
            if (action instanceof DoAsAction)
                {
                return AccessController.doPrivileged(action);
                }

            Subject subject = getCurrentSubject();
            return AccessController.doPrivileged((PrivilegedExceptionAction<T>) () ->
                    Subject.callAs(subject, action::run));
            }
        return action.run();
        }

    @Override
    public void doIfSecure(PrivilegedAction<?> action, Runnable fallback)
        {
        if (hasSecurityManager())
            {
            if (action instanceof DoAsAction)
                {
                AccessController.doPrivileged(action);
                }
            else
                {
                Subject subject = getCurrentSubject();
                AccessController.doPrivileged((PrivilegedAction<?>) () ->
                        Subject.callAs(subject, action::run));
                }
            }
        else
            {
            fallback.run();
            }
        }

    @Override
    public void doIfSecure(Supplier<PrivilegedAction<?>> supplier, Runnable fallback)
        {
        if (hasSecurityManager())
            {
            PrivilegedAction<?> action = supplier.get();
            if (action instanceof DoAsAction)
                {
                AccessController.doPrivileged(action);
                }
            else
                {
                Subject subject = getCurrentSubject();
                AccessController.doPrivileged((PrivilegedAction<?>) () ->
                        Subject.callAs(subject, action::run));
                }
            }
        else
            {
            fallback.run();
            }
        }

    @Override
    public <T> T doIfSecure(PrivilegedAction<T> action, Supplier<T> fallback)
        {
        if (hasSecurityManager())
            {
            if (action instanceof DoAsAction)
                {
                return AccessController.doPrivileged(action);
                }
            Subject subject = getCurrentSubject();
            return AccessController.doPrivileged((PrivilegedAction<T>) () ->
                    Subject.callAs(subject, action::run));
            }
        return fallback.get();
        }

    @Override
    public <T> T doIfSecureInDoAsAction(PrivilegedAction<T> action, Supplier<T> fallback)
        {
        if (hasSecurityManager())
            {
            return AccessController.doPrivileged(new DoAsAction<>(action));
            }
        return fallback.get();
        }

    @Override
    public <T> T doIfSecureInDoAsAction(Supplier<PrivilegedAction<T>> supplier, Supplier<T> fallback)
        {
        if (hasSecurityManager())
            {
            return AccessController.doPrivileged(new DoAsAction<>(supplier.get()));
            }
        return fallback.get();
        }

    @Override
    public <T> T doIfSecure(Supplier<PrivilegedAction<T>> supplier, Supplier<T> fallback)
        {
        if (hasSecurityManager())
            {
            PrivilegedAction<T> action = supplier.get();
            if (action instanceof DoAsAction)
                {
                return AccessController.doPrivileged(action);
                }
            Subject subject = getCurrentSubject();
            return AccessController.doPrivileged((PrivilegedAction<T>) () ->
                    Subject.callAs(subject, action::run));
            }
        return fallback.get();
        }

    @Override
    public <T> T doPrivileged(PrivilegedAction<T> action)
        {
        if (action instanceof DoAsAction)
            {
            return AccessController.doPrivileged(action);
            }

        Subject subject = getCurrentSubject();
        return AccessController.doPrivileged((PrivilegedAction<T>) () ->
                Subject.callAs(subject, action::run));
        }

    @Override
    public <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException
        {
        if (action instanceof DoAsAction)
            {
            return AccessController.doPrivileged(action);
            }

        Subject subject = getCurrentSubject();
        return AccessController.doPrivileged((PrivilegedExceptionAction<T>) () ->
                Subject.callAs(subject, action::run));
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
    }
