/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * A resource that is activated and then at a later time be released.
 *
 * @author jk 2015.05.21
 * @since Coherence 14.1.1
 */
public interface Releasable
    extends AutoCloseable
    {
    /**
    * Specifies whether the Releasable is active.
    *
    * @return true if active; false otherwise
    */
    public boolean isActive();

    /**
     * Specifies whether or this Releasable has been released.
     * </p>
     * Implementations must override this method to provide the necessary information.
     *
     * @return true if the Releasable has been released; false otherwise
     */
    public default boolean isReleased()
        {
        // to avoid cumbersome caller exception handling;
        // default is a no-op.
        return false;
        }

    /**
    * Release local resources associated with this Releasable instance.
    */
    public void release();

    @Override
    default void close()
        {
        release();
        }
    }
