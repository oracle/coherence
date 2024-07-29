/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.protobuf;

import com.google.protobuf.Message;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import java.lang.reflect.InvocationTargetException;

/**
 * A generic {@link PofSerializer} that can serialize and deserialize any
 * Protobuf {@link Message} to or from a POF stream.
 */
public class ProtobufPofSerializer
        implements PofSerializer<Message>
    {
    @Override
    public void serialize(PofWriter out, Message message) throws IOException
        {
        out.writeString(0, message.getClass().getName());
        out.writeByteArray(1, message.toByteArray());
        out.writeRemainder(null);
        }

    @Override
    public Message deserialize(PofReader in) throws IOException
        {
        String sClass = in.readString(0);
        byte[] ab     = in.readByteArray(1);
        try
            {
            Class<?> clz = Class.forName(sClass);
            return (Message) Classes.invoke(clz, null, "parseFrom", new Object[]{ab});
            }
        catch (ClassNotFoundException e)
            {
            throw new IOException("Could find class " + sClass, e);
            }
        catch (NoSuchMethodException e)
            {
            throw new IOException("Could not deserialize an instance of " + sClass + " as it does not have a static parseFrom method", e);
            }
        catch (InvocationTargetException | IllegalAccessException e)
            {
            throw new IOException("Could not deserialize an instance of " + sClass, e);
            }
        }
    }
