/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.Disposable;

/**
 * A {@link ResourceRegistry} is a registry and owner of strongly typed and
 * explicitly named resources.
 * <p>
 * When a resource is registered with a {@link ResourceRegistry}, the
 * {@link ResourceRegistry} assumes ownership of the said resource, up until
 * at which point the {@link ResourceRegistry} is {@link #dispose() disposed}.
 * <p>
 * <strong>Important:</strong> Although a {@link ResourceRegistry} manages
 * resources in a thread-safe manner, it is possible for a thread calling
 * {@link #getResource(Class, String)} to receive a <code>null</code> return
 * value while another thread is registering a resource.
 *
 * @author bo  2011.06.05
 * @since Coherence 12.1.2
 */
public interface ResourceRegistry
        extends ResourceResolver, Disposable
    {
    /**
     * Registers the resource for later retrieval with {@link #getResource(Class)}.
     * <p>
     * Notes:
     * <ol>
     *  <li>Multiple resources for the same class can be registered if each
     *      resource is registered with a unique name via
     *      {@link #registerResource(Class, String, Object)}
     *  <li>Registration of resources will occur in a thread-safe manner.
     *  <li>Resources that are {@link Disposable} will be disposed when the
     *      {@link ResourceRegistry} is disposed.
     * </ol>
     *
     * @param clzResource  the class of the resource
     * @param resource     the resource
     *
     * @return  the actual name used to register the resource
     *
     * @throws IllegalArgumentException  if a resource of the same specified type
     *                                   is already registered
     */
    public <R> String registerResource(Class<R> clzResource, R resource)
            throws IllegalArgumentException;

    /**
     * Registers the resource with the specified name for later retrieval with
     * {@link #getResource(Class, String)}.
     * <p>
     * Notes:
     * <ol>
     *  <li>Registration of resources will occur in a thread-safe manner.
     *  <li>Resources that are {@link Disposable} will be disposed when the
     *      {@link ResourceRegistry} is disposed.
     * </ol>
     *
     * @param clzResource    the class of the resource
     * @param resource       the resource
     * @param sResourceName  the proposed name of the resource
     *
     * @return  the actual name used to register the resource
     *
     * @throws IllegalArgumentException  if a resource of the same specified type
     *                                   and name is already registered
     */
    public <R> String registerResource(Class<R> clzResource, String sResourceName, R resource)
            throws IllegalArgumentException;

    /**
     * Registers a resource according to the specified {@link RegistrationBehavior}.
     * If successful the registered resource may later be retrieved using method
     * {@link #getResource(Class)}.
     * <p>
     * Notes:
     * <ol>
     *  <li>Multiple resources for the same class can be registered if each
     *      resource is registered with a unique name via
     *      {@link #registerResource(Class, String, Builder, RegistrationBehavior, ResourceLifecycleObserver)}
     *  <li>Registration of resources will occur in a thread-safe manner.
     *  <li>Resources that are {@link Disposable} will be disposed when the
     *      {@link ResourceRegistry} is disposed.
     * </ol>
     *
     * @param <R>            the type of the resource
     * @param clzResource    the class of the resource
     * @param bldrResource   the {@link Builder} to realize the resource
     *                       to register (if required the specified behavior
     *                       requires a resource)
     * @param behavior       the {@link RegistrationBehavior} to use
     * @param observer       an optional {@link ResourceLifecycleObserver} that
     *                       will be called when the resource is being released
     *                       from the {@link ResourceRegistry} (may be null)
     *
     * @return  the actual name used to register the resource
     *
     * @throws IllegalArgumentException  if a resource with the specified class
     *                                   is already registered with
     *                                   the {@link ResourceRegistry} and
     *                                   the behavior was {@link RegistrationBehavior#FAIL}
     */
    public <R> String registerResource(Class<R> clzResource, Builder<? extends R> bldrResource,
                                       RegistrationBehavior behavior, ResourceLifecycleObserver<R> observer)
            throws IllegalArgumentException;

    /**
     * Registers a resource according to the specified {@link RegistrationBehavior}.
     * If successful the registered resource may later be retrieved using method
     * {@link #getResource(Class, String)}.
     * <p>
     * Notes:
     * <ol>
     *  <li>Registration of resources will occur in a thread-safe manner.
     *  <li>Resources that are {@link Disposable} will be disposed when the
     *      {@link ResourceRegistry} is disposed.
     * </ol>
     *
     * @param <R>            the type of the resource
     * @param clzResource    the class of the resource
     * @param sResourceName  the proposed name of the resource
     * @param bldrResource   the {@link Builder} to realize the resource
     *                       to register (if required the specified behavior
     *                       requires a resource)
     * @param behavior       the {@link RegistrationBehavior} to use
     * @param observer       an optional {@link ResourceLifecycleObserver} that
     *                       will be called when the resource is being released
     *                       from the {@link ResourceRegistry} (may be null)
     *
     * @return  the actual name used to register the resource
     *
     * @throws IllegalArgumentException  if a resource with the specified class
     *                                   is already registered with
     *                                   the {@link ResourceRegistry} and
     *                                   the behavior was {@link RegistrationBehavior#FAIL}
     */
    public <R> String registerResource(Class<R> clzResource, String sResourceName, Builder<? extends R> bldrResource,
                                       RegistrationBehavior behavior, ResourceLifecycleObserver<R> observer)
            throws IllegalArgumentException;

    /**
     * Unregisters the resource that was previously registered of the specified class and name.
     * <p>
     * Note: Unregistering a resource does not cause it to be disposed, but it
     * does call {@link ResourceLifecycleObserver#onRelease(Object)} if an
     * observer was provided at the time of registration.
     *
     * @param <R>            the type of the resource
     * @param clzResource    the class of the resource
     * @param sResourceName  the name of the resource
     */
    public <R> void unregisterResource(Class<R> clzResource, String sResourceName);

    // ----- ResourceLifecycleObserver interface ----------------------------

    /**
     * The {@link ResourceLifecycleObserver} interface defines lifecycle handlers
     * for resources registered with a {@link ResourceRegistry}.
     */
    public interface ResourceLifecycleObserver<R>
        {
        /**
         * Called by a {@link ResourceRegistry} when a resource is being released.
         * Resources are released when a {@link ResourceRegistry} is disposed.
         *
         * @param resource  the resource being released
         */
        public void onRelease(R resource);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name to use for the registration of a singleton and thus default resource.
     */
    public static final String DEFAULT_NAME = "default";
    }
