/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* Signals that a cluster related exception of some sort has occurred.
* This class is the general class of exceptions produced by failed or
* interrupted cluster operations.
*
* @author gg  2006.06.08
*
* @deprecated As of release 3.4, replaced by
*             {@link com.tangosol.io.pof.PortableException}
*/
public class ClusterException
        extends RuntimeException
    {
    /**
    * Constructs a ClusterException with no detail message.
    */
    public ClusterException()
        {
        super();
        }

    /**
    * Constructs a ClusterException with the specified detail message.
    *
    * @param s the String that contains a detailed message
    */
    public ClusterException(String s)
        {
        super(s);
        }

    /**
    * Construct a ClusterException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public ClusterException(Throwable e)
        {
        super(e);
        }

    /**
    * Construct a ClusterException from a Throwable object and an additional
    * description.
    *
    * @param s  the additional description
    * @param e  the Throwable object
    */
    public ClusterException(String s, Throwable e)
        {
        super(s, e);
        }
    }