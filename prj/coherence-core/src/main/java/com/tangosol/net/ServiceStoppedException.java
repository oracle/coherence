/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* Signals that a request was not able to execute because the underlying service
* has been stopped.
*
* @since Coherence 12.1.3
*/
public class ServiceStoppedException
        extends IllegalStateException
    {
    /**
    * Constructs a ServiceStoppedException with no detail message.
    */
    public ServiceStoppedException()
        {
        super();
        }

    /**
    * Constructs a ServiceStoppedException with the specified detail message.
    *
    * @param s the String that contains a detailed message
    */
    public ServiceStoppedException(String s)
        {
        super(s);
        }

    /**
    * Construct a ServiceStoppedException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public ServiceStoppedException(Throwable e)
        {
        super(e);
        }

    /**
    * Construct a ServiceStoppedException from a Throwable object and an
    * additional description.
    *
    * @param s  the additional description
    * @param e  the Throwable object
    */
    public ServiceStoppedException(String s, Throwable e)
        {
        super(s, e);
        }
    }