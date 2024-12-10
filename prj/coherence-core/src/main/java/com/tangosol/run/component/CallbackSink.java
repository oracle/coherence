/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.component;


/**
* The CallbackSink class is the base class for all the auto-generated
* by AbstractBean model (or any model derived from it) sink classes
* (i.e. Component.GUI.__Control$Sink)
*
* @version 1.00, 09/23/98
* @author 	Gene Gleyzer
*/
public abstract class CallbackSink
        implements RemoteSink
    {
    /**
    * Default constructor used by sub-sinks
    */
    protected CallbackSink()
        {
        }

    /**
    * Retrieves the feed object for this sink
    */
    public abstract Object get_Feed();
    
    /**
    * Retrieves the remote object for this sink
    */
    public Object get_RemoteObject()
        {
        throw new IntegrationException("Unexpected call to \"get_RemotedObject\" in callback sink");
        }

    /**
    * Establishes the remote object for this sink
    */
    public void set_RemoteObject(Object object)
        {
        throw new IntegrationException("Unexpected call to \"set_RemotedObject\" in callback sink");
        }
    }
