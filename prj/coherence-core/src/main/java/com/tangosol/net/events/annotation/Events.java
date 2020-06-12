/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta annotation that must be applied to all event interceptor annotations,
 * in order to make them discoverable.
 *
 * @author as  2020.04.02
 * @since 20.06
 *
 * @see CacheLifecycleEvents
 * @see EntryEvents
 * @see EntryProcessorEvents
 * @see LifecycleEvents
 * @see TransactionEvents
 * @see TransferEvents
 * @see UnsolicitedCommitEvents
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Events
    {
    }
