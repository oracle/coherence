/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;

/**
 * An implementation of a SIGNAL event.
 *
 * @author mf  2014.03.24
 */
public class SignalEvent
    extends Number
    implements Event
    {
    public SignalEvent(EndPoint point, long lContent)
        {
        f_point    = point;
        f_lContent = lContent;
        }

    // ----- Event interface -----------------------------------


    @Override
    public Type getType()
        {
        return Type.SIGNAL;
        }

    @Override
    public EndPoint getEndPoint()
        {
        return f_point;
        }

    @Override
    public Object getContent()
        {
        return this;
        }

    @Override
    public Object dispose(boolean fTakeContent)
        {
        return this;
        }

    @Override
    public void dispose()
        {
        }

    // ----- Number interface ----------------------------------

    @Override
    public int intValue()
        {
        return (int) f_lContent;
        }

    @Override
    public long longValue()
        {
        return f_lContent;
        }

    @Override
    public float floatValue()
        {
        return (float) f_lContent;
        }

    @Override
    public double doubleValue()
        {
        return (double) f_lContent;
        }

    @Override
    public byte byteValue()
        {
        return (byte) f_lContent;
        }

    @Override
    public short shortValue()
        {
        return (short) f_lContent;
        }

    // ----- Object interface --------------------------------------

    @Override
    public String toString()
        {
        return "SignalEvent(" + f_point + ", content=" + f_lContent + ")";
        }


    // ----- data members ------------------------------------------

    /**
     * The associated endpoint.
     */
    protected final EndPoint f_point;

    /**
     * The content.
     */
    protected final long f_lContent;
    }
