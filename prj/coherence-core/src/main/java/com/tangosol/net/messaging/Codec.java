/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import java.io.IOException;


/**
* A Codec converts a {@link Message} object to and from a binary
* representation.
*
* @author cp jh  2006.04.18
*
* @since Coherence 3.2
*/
public interface Codec
    {
    /**
    * Encode and write a binary representation of the given Message to
    * the given stream.
    * <p>
    * Using the passed Channel, the Codec has access to both the
    * MessageFactory for the Channel and the underlying Connection.
    *
    * @param channel  the Channel object through which the binary-encoded
    *                 Message will be passed
    * @param message  the Message to encode
    * @param out      the BufferOutput to write the binary representation
    *                 of the Message to
    *
    * @throws IOException if an error occurs encoding or writing the
    *         Message
    */
    public void encode(Channel channel, Message message, WriteBuffer.BufferOutput out)
            throws IOException;

    /**
    * Reads a binary-encoded Message from the passed BufferInput object.
    * <p>
    * Using the passed Channel, the Codec has access to both the
    * MessageFactory for the Channel and the underlying Connection.
    *
    * @param channel  the Channel object through which the binary-encoded
    *                 Message was passed
    * @param in       the BufferInput containing the binary-encoded
    *                 Message
    *
    * @return the Message object encoded in the given BufferInput
    *
    * @throws IOException if an error occurs reading or decoding the
    *         Message
    */
    public Message decode(Channel channel, ReadBuffer.BufferInput in)
            throws IOException;
    }
