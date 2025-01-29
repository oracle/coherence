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
 * This class is here to allow for pre-Java24 code that still uses a SecurityManager
 * AccessController to work. There post-Java24 version of this class is in
 * the coherence-core-24 module and is built into the multi-release coherence.jar.
 *
 * @author Jonathan Knight 25/01/2025
 */
public interface SecurityManagerWrapper
    {
    /**
     * Returns the current subject.
     * <p>
     * The current subject is installed by the callAs method. When {@code callAs(subject, action)}
     * is called, action is executed with subject as its current subject which can be retrieved
     * by this method. After action is finished, the current subject is reset to its previous
     * value. The current subject is null before the first call of {@code callAs()}.
     *
     * @return the current subject, or null if a current subject is not installed or the current
     *         subject is set to null
     */
    Subject getCurrentSubject();

    /**
     * Return {@code true} if the JVM is running with a security manager enabled.
     *
     * @return {@code true} if the JVM is running with a security manager enabled
     */
    boolean hasSecurityManager();

    /**
     * Performs the specified PrivilegedAction with privileges enabled or if
     * the JVM is running without a security manager this method just
     * calls {@link PrivilegedAction#run()}.
     * <p>
     * The action is performed with all the permissions possessed by the caller's
     * protection domain. If the action's run method throws an (unchecked) exception,
     * it will propagate through this method.
     * Note that any DomainCombiner associated with the current AccessControlContext
     * will be ignored while the action is performed.
     *
     * @param action  the action to be performed.
     * @param <T>  the type of the value returned by the PrivilegedAction's run method.
     *
     * @return the value returned by the action's run method.
     *
     * @throws  NullPointerException – if the action is null
     */
    <T> T doIfSecure(PrivilegedAction<T> action);

    /**
     * Performs the specified PrivilegedExceptionAction with privileges enabled
     * or if the JVM is running without a security manager this method just
     * calls {@link PrivilegedAction#run()}.
     * <p>
     * The action is performed with all the permissions possessed by the caller's protection domain.
     * If the action's run method throws an unchecked exception, it will propagate through this method.
     * Note that any DomainCombiner associated with the current AccessControlContext will be ignored
     * while the action is performed.
     *
     * @param action  the action to be performed
     * @param <T>  the type of the value returned by the PrivilegedExceptionAction's run method.
     *
     * @return the value returned by the action's run method
     *
     * @throws PrivilegedActionException – if the specified action's run method threw a checked exception
     * @throws NullPointerException – if the action is null
     */
    <T> T doIfSecure(PrivilegedExceptionAction<T> action) throws Exception;

    /**
     * Performs the specified PrivilegedAction with privileges enabled if the
     * JVM is running with a security manager enabled otherwise run the
     * {@code fallback} runnable.
     * <p>
     * The action is performed with all the permissions possessed by the caller's
     * protection domain. If the action's run method throws an (unchecked) exception,
     * it will propagate through this method.
     * Note that any DomainCombiner associated with the current AccessControlContext
     * will be ignored while the action is performed.
     *
     * @param action    the action to be performed.
     * @param fallback  the {@link Runnable} to execute if the JVM is not running with a security manager
     */
    void doIfSecure(PrivilegedAction<?> action, Runnable fallback);

    <T> T doIfSecure(PrivilegedAction<T> action, Supplier<T> fallback);

    /**
     * Performs the specified PrivilegedAction with privileges enabled.
     * The action is performed with all the permissions possessed by the caller's
     * protection domain. If the action's run method throws an (unchecked) exception,
     * it will propagate through this method.
     * Note that any DomainCombiner associated with the current AccessControlContext
     * will be ignored while the action is performed.
     *
     * @param action  the action to be performed.
     * @param <T>  the type of the value returned by the PrivilegedAction's run method.
     *
     * @return the value returned by the action's run method.
     *
     * @throws  NullPointerException – if the action is null
     */
    <T> T doPrivileged(PrivilegedAction<T> action);

    /**
     * Performs the specified PrivilegedExceptionAction with privileges enabled.
     * The action is performed with all the permissions possessed by the caller's protection domain.
     * If the action's run method throws an unchecked exception, it will propagate through this method.
     * Note that any DomainCombiner associated with the current AccessControlContext will be ignored
     * while the action is performed.
     *
     * @param action  the action to be performed
     * @param <T>  the type of the value returned by the PrivilegedExceptionAction's run method.
     *
     * @return the value returned by the action's run method
     *
     * @throws PrivilegedActionException – if the specified action's run method threw a checked exception
     * @throws NullPointerException – if the action is null
     */
    <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException;

    /**
     * Throws a {@link SecurityException} if the requested access, specified by the given permission,
     * is not permitted based on the security policy currently in effect.
     * <p>
     * This method calls AccessController.checkPermission with the given permission.
     *
     * @param supplier  a {@link Supplier} to provide the permission to check
     *
     * @throws SecurityException if access is not permitted based on the current security policy.
     * @throws NullPointerException if the permission argument is null.
     */
    void checkPermission(Supplier<Permission> supplier);

    /**
     * Throws a {@link SecurityException} if the requested access, specified by the given permission,
     * is not permitted based on the security policy currently in effect.
     * <p>
     * This method calls AccessController.checkPermission with the given permission
     *
     * @param permission  the requested permission.
     *
     * @throws SecurityException if access is not permitted based on the current security policy.
     * @throws NullPointerException if the permission argument is null.
     */
    void checkPermission(Permission permission);
    }
