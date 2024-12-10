/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.component;


import java.io.Serializable;

/**
* The RemoteSink interface is the base interface for all the auto-generated
* by AbstractBean model when in remote mode (or any model derived from it)
* sink classes (i.e. Component.GUI.__Control$Sink)
*
* @version 1.00, 12/10/99
* @author  Pat McNerthney
*/
public interface RemoteSink
        extends Serializable
    {
    /**
    * Retrieves the remote object for this sink
    */
    public Object get_RemoteObject();

    /**
    * Establishes the remote object for this sink
    */
    public void set_RemoteObject(Object object);
    }
