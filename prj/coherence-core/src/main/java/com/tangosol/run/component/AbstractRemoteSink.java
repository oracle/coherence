/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.component;


import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;

import com.tangosol.util.Converter;
import com.tangosol.util.CollectionHelper;


/**
* The AbstractRemoteSink class is the base class for all the auto-generated
* by Remote sink classes (i.e. Component.Service.__Bean$Sink$EJB)
*
* @version 1.00, 02/21/00
* @author  Pat McNerthney
*/
public abstract class AbstractRemoteSink
        implements RemoteSink
    {
    /**
    * Default constructor used by sub-sinks
    */
    protected AbstractRemoteSink()
        {
        }

    /**
    * Retrieves the remote object for this sink
    */
    public abstract Object get_RemoteObject();

    /**
    * Establishes the remote object for this sink
    */
    public abstract void set_RemoteObject(Object object);
    }
