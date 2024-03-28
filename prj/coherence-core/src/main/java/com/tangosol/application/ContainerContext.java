/*
 * Copyright (c) 2000, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.application;


import java.util.Set;
import java.util.concurrent.Callable;


/**
 * ContainerContext represents various aspects of the container infrastructure
 * that could be used by Coherence applications working in the context of the
 * {@link ContainerAdapter}.
 *
 * @author gg 2014.06.04
 * @since Coherence 12.2.1
 */
public interface ContainerContext
    {
    /**
     * Obtain the Domain Partition name associated with this ContainerContext.
     * <p>
     * Note, that to get the name of the GLOBAL DomainPartition, the caller
     * should do the following:
     * <pre>
     *    getGlobalContext().getDomainPartition();
     * </pre>
     *
     * @return the Domain Partition name or null if the container does not support
     *         Multi-Tenancy or this feature is turned off
     */
    public String getDomainPartition();

    /**
     * Check whether or not the DomainPartition associated with this context is
     * GLOBAL.
     * <p>
     * Note, that this call is basically equivalent to the following:
     * <pre>
     *    equals(getGlobalContext());
     * </pre>
     *
     * @return {@code true} if the DomainPartition associated with this context is GLOBAL
     */
    public boolean isGlobalDomainPartition();

    /**
     * Obtain the ContainerContext associated with GLOBAL Domain Partition.
     * <p>
     * Important note: since the ContainerContext object could use the context
     * as a part of the event dispatcher or listener identity, it's imperative
     * for the container to maintain no more than one instance of the global
     * context.
     *
     * @return the ContainerContext associated with the GLOBAL Domain Partition.
     */
    public ContainerContext getGlobalContext();

    /**
     * Obtain the ContainerContext associated with the current thread.
     * Note, that the returned context could <b>only</b> be one of
     * <ul>
     *     <li>null, if the current thread is not associated with any context;
     *     <li>the GLOBAL context;
     *     <li>this ContainerContext;
     *     <li>another instance of ContainerContext representing the same
     *         Domain Partition as this context.
     * </ul>
     * Note2: since the ContainerContext object could use the context as a part
     * of the event dispatcher or listener identity, it's imperative for the
     * container to maintain no more than one instance of the context per
     * distinct Domain Partition per application.
     *
     * @return the ContainerContext associated with the current thread or null
     *         if the thread has no association
     */
    public ContainerContext getCurrentThreadContext();

    /**
     * Set the execution context for the calling thread to be associated with
     * this ContainerContext's Domain Partition.
     * <p>
     * Note, that to set the execution context as GLOBAL, the caller should do
     * the following:
     * <pre>
     *    getGlobalContext().setCurrentThreadContext();
     * </pre>
     */
    public void setCurrentThreadContext();

    /**
     * Reset the execution context for the calling thread to be associated with
     * this ContainerContext's Domain Partition. This method should only be called
     * if the thread context was changed using {@link #setCurrentThreadContext()}.
     */
    public void resetCurrentThreadContext();

    /**
     * Call the specified action in the context of the associated Domain Partition.
     * <p>
     * Note, that to call the action in the GLOBAL context, the caller should do
     * the following:
     * <pre>
     *    getGlobalContext().runInDomainPartitionContext(action);
     * </pre>
     *
     * @param <V>     the result type of the action
     * @param action  the action to call
     *
     * @return the result of the Callable action
     */
    public <V> V runInDomainPartitionContext(Callable<V> action);

    /**
     * Run the specified action in the context of the associated Domain Partition.
     * <p>
     * Note, that to run the action in the GLOBAL context, the caller should do
     * the following:
     * <pre>
     *    getGlobalContext().runInDomainPartitionContext(action);
     * </pre>
     *
     * @param action  the action to run
     */
    default public void runInDomainPartitionContext(Runnable action)
        {
        runInDomainPartitionContext(() ->
            {
            action.run();
            return null;
            });
        }

    /**
     * Check whether or not the cache with the specified name should be shared
     * across various Domain Partitions.
     *
     * @param sCache the cache name
     *
     * @return true iff the specified cache should be shared
     */
    public boolean isCacheShared(String sCache);

    /**
     * Obtain a value for a configuration attribute for a given cache.
     *
     * @param sCache     the cache name
     * @param sAttribute the attribute name
     *
     * @return the specified attribute value or null, if the default value
     *         should be used
     */
    public Object getCacheAttribute(String sCache, String sAttribute);

    /**
     * Get a set of names for all shared caches. Those names will be used to
     * start corresponding cache services during {@link ContainerAdapter#activate
     * application activation}.
     *
     * @return the set of shared cache names
     */
    public Set<String> getSharedCaches();
    }
