/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.tangosol.util.NullImplementation;

/**
 * The Lockable interface is used for managing exclusive access to thread-safe
 * classes.
 *
 * @author bbc  2021.12.06
 */
public interface Lockable
    {
    /**
     * Suggest to this Lockable that the caller requires exclusive access
     * until {@link AutoCloseable#close() close} is called on the
     * returned {@link AutoCloseable}.
     * <p>
     * Note: the caller <b>must</b> call {@link AutoCloseable#close() close} on
     *       the returned object
     *
     * @return an {@link AutoCloseable} object that <b>requires</b> close to be
     *         called on it when exclusive access is no longer needed
     */
    public default Unlockable exclusively()
        {
        return null;
        }

    /**
     * The Unlockable interface is used for releasing the exclusive access to
     * this Lockable.
     */
    public interface Unlockable
            extends AutoCloseable
        {
        /**
         * Release the acquired exclusive access to this Lockable.
         */
        void close();
        }
    }