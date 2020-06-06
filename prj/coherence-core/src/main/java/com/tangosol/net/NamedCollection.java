/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;


/**
 * NamedCollection defines a common base interface for various named collection types.
 *
 * @author jk 2015.06.27
 * @since Coherence 14.1.1
 */
public interface NamedCollection
        extends Releasable
    {
    /**
     * Obtain the name of this NamedCollection.
     *
     * @return the name of this NamedCollection
     */
    public String getName();

    /**
     * Return the Service that this NamedCollection is a part of.
     *
     * @return the Service
     */
    public Service getService();

    /**
     * Release and destroy this instance of NamedCollection.
     * <p>
     * <b>Warning:</b> This method is used to completely destroy the specified
     * collection across the cluster. All references in the entire cluster to this
     * collection will be invalidated, the collection data will be cleared, and all
     * internal resources will be released.
     */
    public void destroy();

    /**
     * Specifies whether or not this NamedCollection has been destroyed.
     *
     * Implementations must override this method to provide the necessary information.
     *
     * @return true if the NamedCollection has been destroyed; false otherwise
     */
    public default boolean isDestroyed()
        {
        // to avoid cumbersome caller exception handling;
        // default is a no-op.
        return false;
        }

    /**
     * The Option interface defines the root interface of all NamedCollection Options.
     */
    public interface Option
        {
        }
    }
