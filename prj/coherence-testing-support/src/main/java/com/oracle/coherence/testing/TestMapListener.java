/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;


/**
* MapListener implementation that exposes the last reported MapEvent.
*
* @author jh  2005.11.29
*/
public class TestMapListener
        extends MultiplexingMapListener
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TestMapListener()
        {
        }


    // ----- MultiplexingMapListener methods --------------------------------

    /**
    * {@inheritDoc}
    */
    protected synchronized void onMapEvent(MapEvent evt)
        {
        m_event = evt;
        ++m_cEvents;
        notifyAll();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the MapEvent received by this MapListener.
    *
    * @return the last MapEvent received by this MapListener
    */
    public synchronized MapEvent getEvent()
        {
        MapEvent evt = m_event;
        clearEvent();
        return evt;
        }

    /**
    * Return the MapEvent received by this MapListener, blocking for 1 second
    * in the case that an event hasn't been received yet.
    *
    * @return the MapEvent received by this MapListener.
    */
    public synchronized MapEvent waitForEvent()
        {
        return waitForEvent(1000L);
        }

    /**
    * Return the MapEvent received by this MapListener, blocking for the
    * specified number of milliseconds in the case that an event hasn't been
    * received yet.
    *
    * @param cMillis  the number of milliseconds to wait for an event
    *
    * @return the MapEvent received by this MapListener.
    */
    public synchronized MapEvent waitForEvent(long cMillis)
        {
        MapEvent event;
        if ((event = m_event) == null)
            {
            try
                {
                wait(cMillis); // we don't use Blocking.wait here as this class is used as part of extend backwards compatibility tests which don't have the Blocking class
                event = m_event;
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw ensureRuntimeException(e);
                }
            }

        clearEvent();
        return event;
        }

    /**
    * Reset the MapEvent property.
    */
    public synchronized void clearEvent()
        {
        m_event = null;
        }

    /**
    * Return the number of MapEvents that have been received by this
    * MapListener.
    *
    * @return the number of MapEvents received by this MapListener
    */
    public synchronized int getCount()
        {
        return m_cEvents;
        }

    /**
    * Configure the number of MapEvents that have been received by this
    * MapListener.
    *
    * @param cEvents  the number of MapEvents received by this MapListener
    */
    public void setCount(int cEvents)
        {
        m_cEvents = cEvents;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The last MapEvent received by this MapListener.
    */
    private MapEvent m_event;

    /**
     * The number of events that have been received.
     */
    private int m_cEvents;
    }
