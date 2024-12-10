/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


/**
* Interface for obtaining a packet's identity information.
*
* @author mf 2006.04.11
*/
public interface PacketIdentifier
    {
    /**
    * Return the FromId of the message this packet belongs to.
    *
    * @return the packet's message FromId
    */
    public long getFromMessageId();

    /**
    * Return the packet's position within its message.
    *
    * @return the packet's PartIndex
    */
    public int getMessagePartIndex();
    }