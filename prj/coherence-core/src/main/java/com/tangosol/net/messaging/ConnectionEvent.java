/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.util.ClassHelper;
import com.tangosol.util.Listeners;

import java.util.EventListener;
import java.util.EventObject;


/**
* An event which indicates that a {@link Connection} was:
* <ul>
*   <li>opened</li>
*   <li>closed</li>
*   <li>determined to be unusable</li>
* </ul>
* A ConnectionEvent object is passed as an argument to the
* {@link ConnectionListener} interface methods.
*
* @author jh  2006.03.28
*
* @see ConnectionListener
*
* @since Coherence 3.2
*/
public class ConnectionEvent
        extends EventObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new ConnectionEvent.
    *
    * @param connection the Connection for which the event applies
    * @param nId        the event's ID, one of {@link #CONNECTION_OPENED},
    *                   {@link #CONNECTION_CLOSED}, or
    *                   {@link #CONNECTION_ERROR}
    */
    public ConnectionEvent(Connection connection, int nId)
        {
        this(connection, nId, null);
        }

    /**
    * Construct a new ConnectionEvent.
    *
    * @param connection the Connection for which the event applies
    * @param nId        the event's ID, one of {@link #CONNECTION_OPENED},
    *                   {@link #CONNECTION_CLOSED}, or
    *                   {@link #CONNECTION_ERROR}
    * @param e          an optional Throwable associated with the event
    */
    public ConnectionEvent(Connection connection, int nId, Throwable e)
        {
        super(connection);

        if (nId < CONNECTION_OPENED || nId > CONNECTION_ERROR)
            {
            throw new IllegalArgumentException();
            }

        m_nId = nId;
        m_e   = e;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a String representation of this ConnectionEvent object.
    *
    * @return a String representation of this ConnectionEvent object
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '{' + getDescription() + '}';
        }


    // ----- helpers ------------------------------------------------------

    /**
    * Dispatch this event to the specified listeners collection.
    *
    * @param listeners the listeners collection
    *
    * @throws ClassCastException if any of the targets is not an instance of
    *         the ConnectionListener interface
    */
    public void dispatch(Listeners listeners)
        {
        if (listeners != null)
            {
            dispatch(listeners.listeners());
            }
        }

    /**
    * Dispatch this event to the specified array of listeners.
    *
    * @param aListeners  the array of listeners
    *
    * @throws ClassCastException if any of the targets is not
    *         an instance of MemberListener interface
    */
    public void dispatch(EventListener[] aListeners)
        {
        for (int i = aListeners.length; --i >= 0; )
            {
            ConnectionListener target = (ConnectionListener) aListeners[i];

            switch (getId())
                {
                case CONNECTION_OPENED:
                    target.connectionOpened(this);
                    break;

                case CONNECTION_CLOSED:
                    target.connectionClosed(this);
                    break;

                case CONNECTION_ERROR:
                    target.connectionError(this);
                    break;
                }
            }
        }

    /**
    * Get the event's description.
    *
    * @return this event's description
    */
    protected String getDescription()
        {
        StringBuffer sb = new StringBuffer(DESCRIPTIONS[getId()])
                .append(' ')
                .append(getConnection());

        Throwable err = getThrowable();
        if (err != null)
            {
            sb.append(' ')
              .append(err);
            }

        return sb.toString();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the Connection associated with this event.
    *
    * @return the Connection
    */
    public Connection getConnection()
        {
        return (Connection) getSource();
        }

    /**
    * Return an identifier for this event.
    *
    * @return the event ID, one of {@link #CONNECTION_OPENED},
    *         {@link #CONNECTION_CLOSED}, or {@link #CONNECTION_ERROR}
    */
    public int getId()
        {
        return m_nId;
        }

    /**
    * Return the optional Throwable associated with this event.
    * <p>
    * This method will usually return a null value if the event identifier is
    * anything but {@link #CONNECTION_ERROR}.
    *
    * @return the Throwable
    */
    public Throwable getThrowable()
        {
        return m_e;
        }


    // ----- constants ------------------------------------------------------

    /**
    * This event identifier indicates that a Connection has been established.
    */
    public static final int CONNECTION_OPENED = 1;

    /**
    * This event identifier indicates that a Connection was closed.
    */
    public static final int CONNECTION_CLOSED = 2;

    /**
    * This event identifier indicates that a Connection has failed.
    */
    public static final int CONNECTION_ERROR  = 3;

    /**
    * Descriptions of the various event identifiers.
    */
    private static final String[] DESCRIPTIONS   =
            {"<unknown>", "OPENED", "CLOSED", "ERROR"};


    // ----- data members ---------------------------------------------------

    /**
    * The event identifier.
    */
    private int m_nId;

    /**
    * An optional Throwable associated with the event.
    */
    private Throwable m_e;
    }
