/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;

/**
 * Pseudo MapListener that can be used to listen for a deactivation event
 * from a NamedCache.
 * <p>
 * Instances of this interface can be added to a NamedCache with the single
 * parameter {@link NamedCache#addMapListener(MapListener) addMapListener()}
 * method. The reason for calling each method is defined below:
 * <ol>
 *     <li>{@code entryDeleted} - a {@link NamedCache cache} has been destroyed</li>
 *     <li>{@code entryUpdated} - a {@link NamedCache cache} has been {@link
 *         NamedCache#truncate() truncated}.</li>
 *     <li>{@code entryInserted} - Unused.</li>
 * </ol>
 *
 * @author jh  2013.06.27
 */
public interface NamedCacheDeactivationListener
        extends MapListenerSupport.SynchronousListener
    {
    /**
     * This event handler will never be called.
     *
     * @param evt  the MapEvent carrying the insert information
     */
    @Override
    public void entryInserted(MapEvent evt);

    /**
     * This event handler will never be called.
     *
     * @param evt  the MapEvent carrying the update information
     */
    @Override
    public void entryUpdated(MapEvent evt);

    /**
     * Invoked when the NamedCache has been deactivated.
     *
     * @param evt  the MapEvent carrying the deactivation information
     */
    @Override
    public void entryDeleted(MapEvent evt);
    }