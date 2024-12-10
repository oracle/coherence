/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.EventObject;
import java.util.EventListener;


/**
* An event which indicates that a Service state has changed:
* <ul>
* <li>a service is starting</li>
* <li>a service has started</li>
* <li>a service is stopping</li>
* <li>a service has stopped</li>
* </ul>
* A ServiceEvent object is sent as an argument to the ServiceListener
* interface methods.
*
* @see Service
* @see ServiceListener
*
* @author jh  2007.11.12
*/
public class ServiceEvent
        extends EventObject
    {
    /**
    * Constructs a new ServiceEvent.
    *
    * @param service  the Service that fired the event
    * @param nId      this event's ID, one of the SERVICE_* enum values
    */
    public ServiceEvent(Service service, int nId)
        {
        super(service);
        m_nId = nId;
        }

    /**
    * Return this event's ID.
    *
    * @return the event ID, one of the SERVICE_* enum values
    */
    public int getId()
        {
        return m_nId;
        }

    /**
    * Return the Service that fired the event.
    *
    * @return the Service
    */
    public Service getService()
        {
        return (Service) getSource();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Returns a String representation of this ServiceEvent object.
    *
    * @return a String representation of this ServiceEvent object
    */
    public String toString()
        {
        return new StringBuffer("ServiceEvent{")
          .append(DESCRIPTIONS[getId()])
          .append(' ')
          .append(getSource().getClass().getName())
          .append('}')
          .toString();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Dispatch this event to the specified listeners collection.
    *
    * @param listeners the listeners collection
    *
    * @throws ClassCastException if any of the targets is not
    *         an instance of the ServiceListener interface
    */
    public void dispatch(Listeners listeners)
        {
        if (listeners != null)
            {
            dispatch(listeners.listeners());
            }
        }

    /**
    * Dispatch this event to the specified array of listeners.
    *
    * @param aListeners  the array of listeners
    *
    * @throws ClassCastException if any of the targets is not
    *         an instance of ServiceListener interface
    */
    public void dispatch(EventListener[] aListeners)
        {
        for (int i = aListeners.length; --i >= 0; )
            {
            ServiceListener target = (ServiceListener) aListeners[i];

            switch (getId())
                {
                case SERVICE_STARTING:
                    target.serviceStarting(this);
                    break;

                case SERVICE_STARTED:
                    target.serviceStarted(this);
                    break;

                case SERVICE_STOPPING:
                    target.serviceStopping(this);
                    break;

                case SERVICE_STOPPED:
                    target.serviceStopped(this);
                    break;
                 }
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * This event indicates that a service is starting.
    */
    public static final int SERVICE_STARTING = 1;

    /**
    * This event indicates that a service has started.
    */
    public static final int SERVICE_STARTED  = 2;

    /**
    * This event indicates that a service is stopping.
    */
    public static final int SERVICE_STOPPING = 3;

    /**
    * This event indicates that a service has stopped.
    */
    public static final int SERVICE_STOPPED  = 4;

    /**
    * Descriptions of the various event IDs.
    */
    private static final String[] DESCRIPTIONS =
            {"<unknown>", "STARTING", "STARTED", "STOPPING", "STOPPED"};


    // ----- data members ---------------------------------------------------

    /**
    * The event's id.
    */
    private int m_nId;
    }