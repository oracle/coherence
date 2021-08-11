/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interceptor is an annotation that can be applied to
 * {@link com.tangosol.net.events.EventInterceptor} implementations.
 * The annotation allows the specification of an identifier and
 * an order.
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
