/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

/**
 * AutoCloseable which provides access to a resource, and releases a lock upon being closed.
 *
 * @see AutoLock#acquire()
 *
 * @author mf 2014.10.02
 */
public interface Sentry<R>
    extends AutoCloseable
    {
    @Override
    void close();

    /**
     * Return the resource associated with the sentry.
     * <p>
     * This resource is only valid until the sentry is closed.
     *
     * @return the resource.
     */
    public R getResource();
    }


