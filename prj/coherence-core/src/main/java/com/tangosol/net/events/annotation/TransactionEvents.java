/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.annotation;

import com.tangosol.net.events.partition.TransactionEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TransactionEventInterceptor is an annotation that should be applied to
 * {@link com.tangosol.net.events.EventInterceptor} implementations
 * that want to receive {@link TransactionEvent}s.
 *
 * @author as  2020.04.01
 * @since 20.06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Events
public @interface TransactionEvents
    {
    /**
     * This element specifies all types of {@link TransactionEvent.Type}s the
     * interceptor would like to be invoked on.
     * <p>
     * If not specified, the interceptor will be registered to handle ALL
     * {@link TransactionEvent.Type}s.
     *
     * @return all {@link TransactionEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     */
    TransactionEvent.Type[] value() default {};
    }
