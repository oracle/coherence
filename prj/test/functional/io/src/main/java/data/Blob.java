/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data;


import java.io.Serializable;
import java.util.Arrays;


/**
 * Java serializable class
 */
public class Blob
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public Blob()
        {
        m_arPayload = new byte[0];
        }

    public Blob(int nSize)
        {
        m_arPayload = new byte[nSize];
        Arrays.fill(m_arPayload, (byte) 28);
        }

    // ----- Blob methods -----------------------------------------------

    public void setPayload(byte[] arPayload)
        {
        m_arPayload = arPayload;
        }

    public byte[] getPayload()
        {
        return m_arPayload;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (! (o instanceof Blob))
            {
            return false;
            }
        Blob other = (Blob) o;
        return Arrays.equals(m_arPayload, other.getPayload());
        }

    @Override
    public int hashCode()
        {
        return m_arPayload == null || m_arPayload.length == 0 ? 1 : m_arPayload.length + m_arPayload[0];
        }

    @Override
    public String toString()
        {
        return "Blob payload length=" + m_arPayload.length + " first element:" + m_arPayload[0];
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -1L;

    // ----- data -----------------------------------------------------------

    private byte[] m_arPayload;
    }
