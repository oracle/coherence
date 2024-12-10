/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.util.Base;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import javax.inject.Named;

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

    /**
     * Return a {@link Map} of {@link SerializerFactory} instances discovered on the class path.
     *
     * @return a {@link Map} of {@link SerializerFactory} instances discovered on the class path
     */
    static Map<String, SerializerFactory> discoverSerializers()
        {
        return discoverSerializers(Classes.getContextClassLoader());
        }

    /**
     * Return a {@link Map} of {@link SerializerFactory} instances discovered on the class path.
     *
     * @param loader  the {@link ClassLoader} to use to discover any serializers
     *
     * @return a {@link Map} of {@link SerializerFactory} instances discovered on the class path
     */
    static Map<String, SerializerFactory> discoverSerializers(ClassLoader loader)
        {
        Map<String, SerializerFactory> map = new HashMap<>();
        map.putAll(loadSerializers(ServiceLoader.load(SerializerFactory.class, loader), SerializerFactory.class));
        map.putAll(loadSerializers(ServiceLoader.load(Serializer.class, loader), Serializer.class));
        return map;
        }

    /**
     * Helper method for {@link #discoverSerializers()}.
     *
     * @param serviceLoader  the {@link ServiceLoader} to load the services from
     * @param <T>            the service type
     *
     * @see #discoverSerializers()
     */
    private static <T> Map<String, SerializerFactory> loadSerializers(ServiceLoader<T> serviceLoader, Class<T> clz)
        {
        Map<String, SerializerFactory> mapSerializer = new HashMap<>();
        Iterator<T>                    iterator      = serviceLoader.iterator();

        while (iterator.hasNext())
            {
            try
                {
                T                 service = iterator.next();
                String            sName   = null;
                SerializerFactory factory = null;

                if (service instanceof SerializerFactory)
                    {
                    factory = (SerializerFactory) service;
                    sName   = factory.getName();
                    }
                else
                    {
                    sName = ((Serializer) service).getName();

                    factory = clzLoader ->
                        {
                        try
                            {
                            Serializer serializer = (Serializer) service.getClass().getConstructor().newInstance();
                            if (serializer instanceof ClassLoaderAware)
                                {
                                ((ClassLoaderAware) serializer).setContextClassLoader(clzLoader);
                                }
                            return serializer;
                            }
                        catch (Exception e)
                            {
                            throw Base.ensureRuntimeException(e,
                                    String.format("Unable to create serializer type [%s]",
                                            service.getClass().getName()));
                            }
                        };
                    }

                if (mapSerializer.putIfAbsent(sName, factory) != null)
                    {
                    Logger.warn(String.format("serializer factory already defined for %s, type [%s]; ignoring this"
                                              + " discovered implementation",
                                              sName, service.getClass().getName()));
                    }
                }
            catch (Throwable t)
                {
                Logger.err("Failed to load service of type " + clz, t);
                }
            }
        return mapSerializer;
        }
    }
