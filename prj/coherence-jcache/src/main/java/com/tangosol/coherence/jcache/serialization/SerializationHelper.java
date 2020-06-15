/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Serialization Helper methods for the Coherence JCache implementation.
 *
 * @author bo  2013.11.11
 * @since Coherence 12.1.3
 */
public class SerializationHelper
    {
    /**
     * Serializes the specified {@link java.io.Serializable} into a byte array
     * (using Java Serialization).
     *
     * @param serializable  a {@link java.io.Serializable} to serialize
     *
     * @return  a byte array
     * @throws java.io.IOException  should serialization fail
     */
    public static byte[] toByteArray(Serializable serializable)
            throws IOException
        {
        ByteArrayOutputStream streamByteArray = new ByteArrayOutputStream();
        ObjectOutputStream    streamObject    = new ObjectOutputStream(streamByteArray);

        streamObject.writeObject(serializable);
        streamObject.flush();
        streamObject.close();

        return streamByteArray.toByteArray();
        }

    /**
     * Deserializes an {@link Object} from a byte array representation
     * (using Java Serialization).
     *
     * @param aBytes  the byte array
     * @param clz     the expected type of the object
     *
     * @return  an {@link Object}
     * @throws java.io.IOException  should deserialization fail
     */
    public static <T> T fromByteArray(byte[] aBytes, Class<T> clz)
            throws IOException
        {
        ByteArrayInputStream streamByteArray = new ByteArrayInputStream(aBytes);
        ObjectInputStream    streamObject    = new ObjectInputStream(streamByteArray);

        try
            {
            Object oObject = streamObject.readObject();

            if (oObject == null || clz.isInstance(oObject))
                {
                return clz.cast(oObject);
                }
            else
                {
                throw new ClassCastException("Expected " + clz.getName() + ", Found " + oObject.getClass().getName());
                }
            }
        catch (ClassCastException e)
            {
            throw e;
            }
        catch (Exception e)
            {
            throw new IOException("Failed to read underlying exception", e);
            }
        finally
            {
            streamObject.close();
            }
        }
    }
