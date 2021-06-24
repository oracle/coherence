/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.tangosol.util.BinaryEntry;

/**
 * StoreObserver implementations provide a means for receivers, such as
 * {@link NonBlockingEntryStore}, to notify the provider of successful
 * completion ({@link #onNext}) or unsuccessful
 * completion ({@link #onError}).
 * <p>
 * A call to {@link #onComplete()} indicates that the receiver will no longer call
 * {@link #onNext} or {@link #onError} on this instance.
 * Note: {@link #onComplete()} is inferred if onNext or onError is called on all
 * entries received, however it provides a means for receivers to suggest
 * termination of the operation and thus to notify any waiting resources
 * or completion routines of the operation's completion.
 *
 * @since 21.06
 * @author mg/hr/as
 *
 * @see NonBlockingEntryStore
 */
 public interface StoreObserver<K, V>
    {
    /**
     * Indicates the associated operation (load or store) has completed
     * successfully and applied to the provided BinaryEntry.
     *
     * @param binEntry the associated entry
     *
     * @throws IllegalStateException if called after the observer is closed or
     *         {@link StoreObserver#onComplete()} has been called.
     */
    public void onNext(BinaryEntry<K, V> binEntry);

    /**
     * Indicate that the corresponding entry is in error, due to the
     * given exception.
     *
     * @param binEntry  the associated entry
     * @param exception exception providing error details
     *
     * @throws IllegalStateException if called after the observer is closed or
     *         {@link StoreObserver#onComplete()} has been called.
     */
    public void onError(BinaryEntry<K, V> binEntry, Exception exception);

    /**
     * Complete the current operation. This is the last call to make on an
     * observer, to signify that no further processing is necessary.
     */
    public void onComplete();
    }
