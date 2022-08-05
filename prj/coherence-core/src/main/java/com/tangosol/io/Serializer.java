/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import java.io.IOException;
import jakarta.inject.Named;

/**
 * The Serializer interface provides the capability of reading and writing a
 * Java object from and to an in-memory buffer.
 * <p>
 * Serializer implementations should implement the ClassLoaderAware interface if
 * they need access to a ClassLoader. However, to support hot-deploying
 * containers, it is important that a Serializer <b>not</b> hold any strong
 * references to that ClassLoader, or to any Class objects obtained from that
 * ClassLoader.
 * <p>
 * <b>Note:</b> it is extremely important that objects that are equivalent
 * according to their "equals()" implementation produce equivalent serialized
 * streams. Violating this relationship will result in non-deterministic
 * behavior for many Coherence services.
 *
 * @author cp/jh  2007.07.21
 * @see ReadBuffer
 * @see WriteBuffer
 * @since Coherence 3.2
 */
public interface Serializer
    {
    /**
     * Serialize an object to a WriteBuffer by writing its state using the
     * specified BufferOutput object.
     * <p>
     * <b>Note:</b> Starting with Coherence 12.2.1 classes that need to designate
     * an alternative object to be used by the Serializer when writing the object
     * to the buffer should implement the {@link SerializationSupport#writeReplace()}
     * method.
     *
     * @param out the BufferOutput with which to write the object's state
     * @param o   the object to serialize
     *
     * @throws IOException if an I/O error occurs
     */
    void serialize(WriteBuffer.BufferOutput out, Object o)
            throws IOException;

    /**
     * Deserialize an object from a ReadBuffer by reading its state using the
     * specified BufferInput object.
     * <p>
     * <b>Note:</b> Starting with Coherence 12.2.1 classes that need to designate
     * an alternative object to be returned by the Serializer after an object is
     * deserialized from the buffer should implement the
     * {@link SerializationSupport#readResolve()} method.
     *
     * @param in the BufferInput with which to read the object's state
     *
     * @return the deserialized user type instance
     *
     * @throws IOException if an I/O error occurs
     */
    default Object deserialize(ReadBuffer.BufferInput in)
            throws IOException
        {
        return deserialize(in, Object.class);
        }

    /**
     * Deserialize an object as an instance of the specified class by reading
     * its state using the specified BufferInput object.
     * <p>
     * <b>Note:</b> Starting with Coherence 12.2.1 classes that need to designate
     * an alternative object to be returned by the Serializer after an object is
     * deserialized from the buffer should implement the
     * {@link SerializationSupport#readResolve()} method.
     *
     * @param  <T>  the class to deserialize to
     * @param in    the BufferInput with which to read the object's state
     * @param clazz the type of the object to deserialize
     *
     * @return the deserialized user type instance
     *
     * @throws IOException if an I/O error occurs
     *
     * @since 12.2.1.4
     */
    @SuppressWarnings("unchecked")
    default <T> T deserialize(ReadBuffer.BufferInput in, Class<? extends T> clazz)
            throws IOException
        {
        return (T) deserialize(in);
        }

    /**
     * Return the name of this serializer.
     *
     * @return the name of this serializer
     *
     * @since 12.2.1.4
     */
    default String getName()
        {
        Named named = getClass().getAnnotation(Named.class);
        return named == null ? null : named.value();
        }
    }
