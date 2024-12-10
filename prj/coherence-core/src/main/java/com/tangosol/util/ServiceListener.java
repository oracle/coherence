/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.EventListener;


/**
* The listener interface for receiving ServiceEvents.
*
* @see Service 
* @see ServiceEvent
*
* @author jh  2007.11.12
*/
public interface ServiceListener
        extends EventListener
    {
    /**
    * Invoked when a service is starting.
    *
    * @param evt  the ServiceEvent.SERVICE_STARTING event
    */
    public void serviceStarting(ServiceEvent evt);

    /**
    * Invoked when a service has started.
    *
    * @param evt  the ServiceEvent.SERVICE_STARTED event
    */
    public void serviceStarted(ServiceEvent evt);

    /**
    * Invoked when a service is stopping.
    *
    * @param evt  the ServiceEvent.SERVICE_STOPPING event
    */
    public void serviceStopping(ServiceEvent evt);

    /**
    * Invoked when a service has stopped.
    *
    * @param evt  the ServiceEvent.SERVICE_STOPPED event
    */
    public void serviceStopped(ServiceEvent evt);
    }