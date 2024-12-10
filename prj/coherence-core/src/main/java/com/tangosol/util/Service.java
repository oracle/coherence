/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A Service is a Controllable that emits service lifecycle events.
*
* @see ServiceListener 
*
* @author jh 2007.11.12
*/
public interface Service
        extends Controllable
    {
    /**
    * Register a ServiceListener that will receive events pertaining to the
    * lifecycle of this Service.
    *
    * @param listener  the new ServiceListener to register; if the listener
    *                  has already been registered, this method has no effect
    */
    public void addServiceListener(ServiceListener listener);

    /**
    * Unregister a ServiceListener from this ConnectionManager.
    * <p>
    * After a ServiceListener is removed, it will no longer receive events
    * pertaining to the lifecycle of this Service.
    *
    * @param listener  the ServiceListener to deregister; if the listener has
    *                  not previously been registered, this method has no
    *                  effect
    */
    public void removeServiceListener(ServiceListener listener);
    }
