/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.annotation;

import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interceptor is an annotation that should be applied to
 * {@link com.tangosol.net.events.EventInterceptor} implementations.
 * The annotation allows the specification of an identifier, event types and
 * an order. These are used to restrict the events an
 * {@link com.tangosol.net.events.EventInterceptor} subscribes to.
 * <p>
 * When used in combination with the cache configuration the annotation
 * members {@link #identifier()} and {@link #order()} can be overridden by
 * the corresponding XML elements.
 *
 * @author hr  2011.10.07
 * @since Coherence 12.1.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Interceptor
    {
    /**
     * A unique identifier for the interceptor.
     *
     * @return unique identifier for the interceptor
     */
    String identifier() default "";

    /**
     * This element specifies all types of {@link EntryEvent.Type}s the
     * interceptor would like to be invoked on.
     *
     * @return all {@link EntryEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     *
     * @deprecated use {@link EntryEvents} annotation instead
     */
    @Deprecated
    EntryEvent.Type[] entryEvents() default {};

    /**
     * This element specifies all types of {@link EntryProcessorEvent.Type}s
     * the interceptor would like to be invoked on.
     *
     * @return all {@link EntryProcessorEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     *
     * @deprecated use {@link EntryProcessorEvents} annotation instead
     */
    @Deprecated
    EntryProcessorEvent.Type[] entryProcessorEvents() default {};

    /**
     * This element specifies all types of {@link TransferEvent.Type}s the
     * interceptor would like to be invoked on.
     *
     * @return all {@link TransferEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     *
     * @deprecated use {@link TransferEvents} annotation instead
     */
    @Deprecated
    TransferEvent.Type[] transferEvents() default {};

    /**
     * This element specifies all types of {@link TransactionEvent.Type}s the
     * interceptor would like to be invoked on.
     *
     * @return all {@link TransactionEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     *
     * @deprecated use {@link TransactionEvents} annotation instead
     */
    @Deprecated
    TransactionEvent.Type[] transactionEvents() default {};

    /**
     * This element specifies all types of {@link UnsolicitedCommitEvent.Type}s
     * the interceptor would like to be invoked on.
     *
     * @return all {@link UnsolicitedCommitEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     *
     * @deprecated use {@link UnsolicitedCommitEvents} annotation instead
     */
    @Deprecated
    UnsolicitedCommitEvent.Type[] unsolicitedEvents() default {};

    /**
     * This element specifies all types of {@link CacheLifecycleEvent.Type}s
     * the interceptor would like to be invoked on.
     *
     * @return all {@link CacheLifecycleEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     *
     * @deprecated use {@link CacheLifecycleEvents} annotation instead
     */
    @Deprecated
    CacheLifecycleEvent.Type[] cacheLifecycleEvents() default {};

    /**
     * This element specifies all types of {@link LifecycleEvent.Type}s
     * the interceptor would like to be invoked on.
     *
     * @return all {@link LifecycleEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     *
     * @deprecated use {@link LifecycleEvents} annotation instead
     */
    @Deprecated
    LifecycleEvent.Type[] lifecycleEvents() default {};

    /**
     * Iff a value of {@link Order#HIGH} is provided this interceptor will
     * request to be the first in the chain of interceptors.
     *
     * @return whether this {@link com.tangosol.net.events.EventInterceptor}
     *         should be first ({@link Order#HIGH}) in the chain of
     *         {@link com.tangosol.net.events.EventInterceptor}s
     */
    Order order() default Order.LOW;

    /**
     * This enum provides an indication of whether the
     * {@link com.tangosol.net.events.EventInterceptor} should request to be
     * first in the chain of
     * {@link com.tangosol.net.events.EventInterceptor}s, hence have a HIGH
     * priority.
     *
     * @author hr 2011.10.05
     *
     * @since 12.1.2
     */
    public enum Order
        {
        /**
         * Indicates an intention to be higher in the chain of
         * {@link com.tangosol.net.events.EventInterceptor}s.
         */
        HIGH,
        /**
         * Indicates that the {@link com.tangosol.net.events.EventInterceptor}
         * is not concerned with where in the chain it resides.
         */
        LOW
        }
    }
