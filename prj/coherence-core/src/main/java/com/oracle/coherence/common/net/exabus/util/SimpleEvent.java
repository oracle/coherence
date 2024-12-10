/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;

import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;

public class SimpleEvent
        implements Event
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SimpleEvent.
     *
     * @param type      the event type
     * @param point     the associated EndPoint
     */
    public SimpleEvent(Type type, EndPoint point)
        {
        this(type, point, null);
        }

    /**
     * Construct a SimpleEvent.
     *
     * @param type      the event type
     * @param point     the associated EndPoint
     * @param oContent  the event content
     */
    public SimpleEvent(Type type, EndPoint point, Object oContent)
        {
        m_type     = type;
        m_point    = point;
        m_oContent = oContent;
        }


    // ----- Event interface ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public Type getType()
        {
        return m_type;
        }

    /**
     * {@inheritDoc}
     */
    public EndPoint getEndPoint()
        {
        return m_point;
        }

    /**
     * {@inheritDoc}
     */
    public Object getContent()
        {
        return m_oContent;
        }

    /**
     * {@inheritDoc}
     */
    public Object dispose(boolean fTakeContent)
        {
        return fTakeContent ? m_oContent : null;
        }

    /**
     * {@inheritDoc}
     */
    public void dispose()
        {
        dispose(/*fTakeContent*/ false);
        }


    // ----- Object interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        String s        = getType() + " event for " + getEndPoint();
        Object oContent = getContent();
        return oContent == null ? s : s + " content " + oContent;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The event type.
     */
    private final Type m_type;

    /**
     * The EndPoint associated with the event
     */
    private final EndPoint m_point;

    /**
     * The Event content.
     */
    private final Object m_oContent;
    }
