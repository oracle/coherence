/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableException;

import java.io.IOException;


/**
* Signals that a request execution in a distributed environment did not
* complete in a pre-determined amount of time. For some specific requests
* this exception could carry a partial execution result.
*
* @see PriorityTask
* @author gg 2006.11.02
* @since Coherence 3.3
*/
public class RequestTimeoutException
        extends RequestIncompleteException
    {
    /**
    * Constructs a RequestTimeoutException with no detail message.
    */
    public RequestTimeoutException()
        {
        super();
        }

    /**
    * Constructs a RequestTimeoutException with the specified detail message.
    *
    * @param s the String that contains a detailed message
    */
    public RequestTimeoutException(String s)
        {
        super(s);
        }

    /**
    * Construct a RequestTimeoutException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public RequestTimeoutException(Throwable e)
        {
        super(e);
        }

    /**
    * Construct a RequestTimeoutException from a Throwable object and an
    * additional description.
    *
    * @param s  the additional description
    * @param e  the Throwable object
    */
    public RequestTimeoutException(String s, Throwable e)
        {
        super(s, e);
        }
    }