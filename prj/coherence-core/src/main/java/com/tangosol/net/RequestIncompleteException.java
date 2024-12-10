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
* Signals that a request execution in a distributed environment failed to
* complete successfully.  For some specific requests this exception could carry
* a partial execution result or failure information.
*
* @see PriorityTask
* @author bbc 2013.05.14
* @since Coherence 12.1.3
*/
public class RequestIncompleteException
        extends PortableException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a RequestIncompleteException with no detail message.
    */
    public RequestIncompleteException()
        {
        super();
        }

    /**
    * Construct a RequestIncompleteException with the specified detail message.
    *
    * @param s the String that contains a detailed message
    */
    public RequestIncompleteException(String s)
        {
        super(s);
        }

    /**
    * Construct a RequestIncompleteException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public RequestIncompleteException(Throwable e)
        {
        super(e);
        }

    /**
    * Construct a RequestIncompleteException from a Throwable object and an
    * additional description.
    *
    * @param s  the additional description
    * @param e  the Throwable object
    */
    public RequestIncompleteException(String s, Throwable e)
        {
        super(s, e);
        }


    // ----- accessors -------------------------------------------------------

    /**
    * Return a partial execution result that may have been assembled before an
    * exception occurred. The result type is specific to a request, most commonly
    * being either of the same Java type as the return value for the corresponding
    * request or a collection of failed keys.
    *
    * @return a partial execution result
    */
    public Object getPartialResult()
        {
        return m_oPartialResult;
        }

    /**
    * Specify a partial execution result.
    *
    * @param oPartialResult  a partial execution result
    */
    public void setPartialResult(Object oPartialResult)
        {
        m_oPartialResult = oPartialResult;
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_oPartialResult = in.readObject(4);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(4, m_oPartialResult);
        }


    // ----- data fields ----------------------------------------------------

    /**
    * Partial execution result (optional).
    */
    private Object m_oPartialResult;
    }