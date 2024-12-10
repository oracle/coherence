/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.concurrent.executor.ComposableContinuation;
import com.oracle.coherence.concurrent.executor.ContinuationService;

import com.tangosol.net.CacheService;

import com.tangosol.net.events.Event;

import com.tangosol.util.InvocableMap.Entry;

/**
 * A {@link LiveObject} is a Cache Entry Value that responds to Coherence Cache Events
 * occurring on itself.
 * <p>
 * Each event that may be handled is represented by a single method, providing a mechanism
 * to perform synchronous processing on the {@link CacheService} {@link Thread} that
 * raised the event and asynchronous processing of the event through returned
 * {@link ComposableContinuation}s, which may be later processed on back-ground {@link Thread}s
 * by a {@link ContinuationService}.
 *
 * @author bo
 * @since 21.12
 */
public interface LiveObject
    {
    /**
     * The {@link LiveObject} was inserted.
     *
     * @param service  the {@link CacheService} for the {@link LiveObject}
     * @param entry    the entry being inserted
     * @param cause    the {@link Cause} of the {@link Event}
     *
     * @return an optional {@link ComposableContinuation} to be performed asynchronously
     *        for the {@link LiveObject} (may be <code>null</code>)
     */
    ComposableContinuation onInserted(CacheService service, Entry entry, Cause cause);

    /**
     * The {@link LiveObject} was updated.
     * <p>
     * This callback is invoked against the updated value, not the original value.
     * The original value is available via the provided {@link Event}.
     *
     * @param service  the {@link CacheService} for the {@link LiveObject}
     * @param entry    the entry being updated
     * @param cause    the {@link Cause} of the {@link Event}
     *
     * @return  an optional {@link ComposableContinuation} to be performed asynchronously
     *          for the {@link LiveObject} (may be <code>null</code>)
     */
    ComposableContinuation onUpdated(CacheService service, Entry entry, Cause cause);

    /**
     * The {@link LiveObject} was deleted.
     *
     * @param service  the {@link CacheService} for the {@link LiveObject}
     * @param entry    the entry being deleted
     * @param cause    the {@link Cause} of the {@link Event}
     *
     * @return an optional {@link ComposableContinuation} to be performed asynchronously
     *         for the {@link LiveObject} (may be <code>null</code>)
     */
    ComposableContinuation onDeleted(CacheService service, Entry entry, Cause cause);
    }
