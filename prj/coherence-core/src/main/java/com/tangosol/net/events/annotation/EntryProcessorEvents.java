/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.annotation;

import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EntryProcessorEventInterceptor is an annotation that should be applied to
 * {@link com.tangosol.net.events.EventInterceptor} implementations
 * that want to receive {@link EntryProcessorEvent}s.
 *
 * @author as  2020.04.01
 * @since 20.06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Events
public @interface EntryProcessorEvents
    {
    /**
     * This element specifies all types of {@link EntryProcessorEvent.Type}s the
     * interceptor would like to be invoked on.
     * <p>
     * If not specified, the interceptor will be registered to handle ALL
     * {@link EntryProcessorEvent.Type}s.
     *
     * @return all {@link EntryProcessorEvent.Type}s the
     *         {@link com.tangosol.net.events.EventInterceptor} would like to
     *         receive notifications on
     */
    EntryProcessorEvent.Type[] value() default {};
    }
