/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* MapTriggerListener is a special purpose MapListener implementation that is
* used to register a {@link MapTrigger} on a corresponding ObservableMap.
* <p>
* <b>Note:</b> Currently, the MapTriggerListener can only be registered
* with partitioned caches and only "globally" (without specifying any filter or
* key), using the {@link ObservableMap#addMapListener(MapListener)} method.
*
* @author cp/gg  2008.03.11
* @since Coherence 3.4
*/
public class MapTriggerListener
        extends MultiplexingMapListener
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MapTriggerListener that can be used to register the specified
    * MapTrigger.
    *
    * @param trigger  the MapTrigger
    */
    public MapTriggerListener(MapTrigger trigger)
        {
        Base.azzert(trigger != null, "Null trigger");

        m_trigger = trigger;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the MapTrigger agent represented by this TriggerListener.
    *
    * @return the MapTrigger agent represented by this TriggerListener
    */
    public MapTrigger getTrigger()
        {
        return m_trigger;
        }


    // ----- MultiplexingMapListener methods --------------------------------

    /**
    * {@inheritDoc}
    */
    protected void onMapEvent(MapEvent evt)
        {
        throw new IllegalStateException(
                "MapTriggerListener may not be used as a generic MapListener");
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying MapTrigger.
    */
    private MapTrigger m_trigger;
    }
