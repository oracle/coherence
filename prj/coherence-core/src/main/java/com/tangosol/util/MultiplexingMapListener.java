/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A base class that simplifies the implementation of a MapListener by
* multiplexing all events into a single listener method.
*
* @author cp  2006.01.19
* @since Coherence 3.1
*/
public abstract class MultiplexingMapListener<K, V>
        extends Base
        implements MapListener<K, V>
    {
    /**
    * Invoked when a map entry has been inserted, updated or deleted. To
    * determine what action has occurred, use {@link MapEvent#getId()}.
    *
    * @param evt  the MapEvent carrying the insert, update or delete
    *             information
    */
    protected abstract void onMapEvent(MapEvent<K, V> evt);


    // ----- MapListener interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void entryInserted(MapEvent<K, V> evt)
        {
        onMapEvent(evt);
        }

    /**
    * {@inheritDoc}
    */
    public void entryUpdated(MapEvent<K, V> evt)
        {
        onMapEvent(evt);
        }

    /**
    * {@inheritDoc}
    */
    public void entryDeleted(MapEvent<K, V> evt)
        {
        onMapEvent(evt);
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object oThat)
        {
        return oThat != null &&
                super.equals(MapListenerSupport.unwrap((MapListener) oThat));
        }
    }
