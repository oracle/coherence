/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* Signals that an underlying communication channel used by a Connection may
* have been closed, severed, or become unusable because the suspect
* protocol has disconnected the client.
* <p>
* After this exception is thrown, any attempt to use the Connection (or any
* Channel created by the Connection) may result in an exception.
*
* @author par  2013.06.13
*
* @since Coherence 12.1.3
*/
public class SuspectConnectionException
        extends ConnectionException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SuspectConnectionException with no detail message.
    */
    public SuspectConnectionException()
        {
        super();
        }

    /**
    * Construct a SuspectConnectionException with the specified detail message.
    *
    * @param s  the String that contains a detailed message
    */
    public SuspectConnectionException(String s)
        {
        super(s);
        }

    /**
    * Construct a SuspectConnectionException with the specified detail message and
    * Connection.
    * 
    * @param s           the String that contains a detailed message
    * @param connection  the Connection where the error occurred
    */
    public SuspectConnectionException(String s, Connection connection)
        {
        super(s, connection);
        }

    /**
    * Construct a SuspectConnectionException from a Throwable.
    * 
    * @param e  the Throwable
    */
    public SuspectConnectionException(Throwable e)
        {
        super(e);
        }

    /**
    * Construct a SuspectConnectionException from a Throwable and Connection.
    * 
    * @param e           the Throwable
    * @param connection  the Connection where the error occurred
    */
    public SuspectConnectionException(Throwable e, Connection connection)
        {
        super(e, connection);
        }

    /**
    * Construct a SuspectConnectionException from a Throwable and an additional
    * description.
    * 
    * @param s  the additional description
    * @param e  the Throwable
    */
    public SuspectConnectionException(String s, Throwable e)
        {
        super(s, e);
        }

    /**
    * Construct a SuspectConnectionException from a Throwable, additional description,
    * and a Connection.
    * 
    * @param s           the additional description
    * @param e           the Throwable
    * @param connection  the Connection where the error occurred
    */
    public SuspectConnectionException(String s, Throwable e, Connection connection)
        {
        super(s, e, connection);
        }
    }
