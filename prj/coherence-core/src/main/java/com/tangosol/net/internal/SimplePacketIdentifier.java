/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


/**
* Simple PacketIdentifier implementation.
*
* @author mf 2006.04.15
*/
public class SimplePacketIdentifier
         implements PacketIdentifier
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constuct an empty SimplePacketIdentifier object.
    *
    * @param lMsgId the Packet's MessageFromId
    * @param iIndex the Packet's MessagePartIndex
    */
    public SimplePacketIdentifier(long lMsgId, int iIndex)
        {
        m_lMsgId = lMsgId;
        m_iIndex = iIndex;
        }


    // ----- PacketIdentifier interface -------------------------------------

    /**
    * @inheritDoc
    */
    public long getFromMessageId()
        {
        return m_lMsgId;
        }

    /**
    * @inheritDoc
    */
    public int getMessagePartIndex()
        {
        return m_iIndex;
        }

    // ----- Object interface -----------------------------------------------

    /**
    * Return a string representation of the PacketIdentifier.
    *
    * @return a string representation of the PacketIdentifier
    */
    public String toString()
        {
        return m_lMsgId + ":" + m_iIndex;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The Packet's FromMessageId.
    */
    protected long m_lMsgId;

    /**
    * The Packet's Index in it's message.
    */
    protected int m_iIndex;
    }
