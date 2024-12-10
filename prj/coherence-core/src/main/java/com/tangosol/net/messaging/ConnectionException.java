/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.io.pof.PortableException;


/**
* Signals that an underlying communication channel used by a Connection may
* have been closed, severed, or become unusable.
* <p>
* After this exception is thrown, any attempt to use the Connection (or any
* Channel created by the Connection) may result in an exception.
*
* @author jh  2006.06.08
*
* @since Coherence 3.2
*/
public class ConnectionException
        extends PortableException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ConnectionException with no detail message.
    */
    public ConnectionException()
        {
        super();
        }

    /**
    * Construct a ConnectionException with the specified detail message.
    *
    * @param s the String that contains a detailed message
    */
    public ConnectionException(String s)
        {
        super(s);
        }

    /**
    * Construct a ConnectionException with the specified detail message and
    * Connection.
    * 
    * @param s           the String that contains a detailed message
    * @param connection  the Connection where the error occurred
    */
    public ConnectionException(String s, Connection connection)
        {
        super(connection == null ? s : s == null ? connection.toString()
                                                 : connection + ": " + s);
        }

    /**
    * Construct a ConnectionException from a Throwable.
    * 
    * @param e  the Throwable
    */
    public ConnectionException(Throwable e)
        {
        super(e);
        }

    /**
    * Construct a ConnectionException from a Throwable and Connection.
    * 
    * @param e           the Throwable
    * @param connection  the Connection where the error occurred
    */
    public ConnectionException(Throwable e, Connection connection)
        {
        super(connection == null ? null : connection.toString(), e);
        }

    /**
    * Construct a ConnectionException from a Throwable and an additional
    * description.
    * 
    * @param s  the additional description
    * @param e  the Throwable
    */
    public ConnectionException(String s, Throwable e)
        {
        super(s, e);
        }

    /**
    * Construct a ConnectionException from a Throwable, additional description,
    * and a Connection.
    * 
    * @param s           the additional description
    * @param e           the Throwable
    * @param connection  the Connection where the error occurred
    */
    public ConnectionException(String s, Throwable e, Connection connection)
        {
        super(connection == null ? s : s == null ? connection.toString()
                                                 : connection + ": " + s, e);
        }
    }
