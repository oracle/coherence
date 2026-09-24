/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
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

import java.util.concurrent.CompletionException;

import java.util.function.Supplier;

/**
 * A wrapper to handle the removal of Java's SecurityManager class.
 * <p>
 * This class allows code that still uses a {@link SecurityManager} or
 * {@link AccessController} to work on Java 21 through 23. The Java 24 and
 * later implementation is in the {@code coherence-core-24} multi-release
 * layer.
 *
 * @author Jonathan Knight 25/01/2025
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
                    callAs(subject, action));
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
                    callAs(subject, action));
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
                        callAs(subject, action));
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
                        callAs(subject, action));
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
                    callAs(subject, action));
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
                    callAs(subject, action));
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
                callAs(subject, action));
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
                callAs(subject, action));
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

    // ----- helper methods -------------------------------------------------

    /**
     * Invoke the specified action using the current subject while preserving
     * the pre-Java 21 exception semantics expected by callers.
     */
    private static <T> T callAs(Subject subject, PrivilegedAction<T> action)
        {
        try
            {
            return Subject.callAs(subject, action::run);
            }
        catch (CompletionException e)
            {
            throw rethrow(e);
            }
        }

    /**
     * Invoke the specified action using the current subject while preserving
     * the pre-Java 21 exception semantics expected by callers.
     */
    private static <T> T callAs(Subject subject, PrivilegedExceptionAction<T> action) throws Exception
        {
        try
            {
            return Subject.callAs(subject, action::run);
            }
        catch (CompletionException e)
            {
            throw rethrowChecked(e);
            }
        }

    /**
     * Unwrap {@link CompletionException} used by {@link Subject#callAs} for
     * unchecked failures so callers still observe the original cause.
     */
    private static RuntimeException rethrow(CompletionException e)
        {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException runtimeException)
            {
            return runtimeException;
            }
        if (cause instanceof Error error)
            {
            throw error;
            }
        return e;
        }

    /**
     * Unwrap {@link CompletionException} used by {@link Subject#callAs} so
     * checked exceptions are re-wrapped by {@link AccessController} as
     * {@link PrivilegedActionException}, matching the previous behavior.
     */
    private static Exception rethrowChecked(CompletionException e)
        {
        Throwable cause = e.getCause();
        if (cause instanceof Exception exception)
            {
            return exception;
            }
        if (cause instanceof Error error)
            {
            throw error;
            }
        return e;
        }
    }
