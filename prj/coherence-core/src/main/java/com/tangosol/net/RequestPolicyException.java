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
* Signals that a request was not allowed to complete due to the related service
* action being disallowed by the corresponding ActionPolicy.
*
* @see ActionPolicy
* @author rhl 2009.10.02
* @since Coherence 3.6
*/
public class RequestPolicyException
        extends PortableException
    {
    /**
    * Constructs a RequestPolicyException with no detail message.
    */
    public RequestPolicyException()
        {
        super();
        }

    /**
    * Constructs a RequestPolicyException with the specified detail message.
    *
    * @param s the String that contains a detailed message
    */
    public RequestPolicyException(String s)
        {
        super(s);
        }

    /**
    * Construct a RequestPolicyException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public RequestPolicyException(Throwable e)
        {
        super(e);
        }

    /**
    * Construct a RequestPolicyException from a Throwable object and an
    * additional description.
    *
    * @param s  the additional description
    * @param e  the Throwable object
    */
    public RequestPolicyException(String s, Throwable e)
        {
        super(s, e);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);
        }
    }