/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.stores;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple store implementation that accepts vectors, keys and metadata
 * as serialized binary {@link ReadBuffer} instances.
 *
 * @param <KeyType>    the type of the keys for the store
 * @param <ValueType>  the type of the values for the store
 */
public abstract class AbstractVectorStore<KeyType, ValueType>
    {
    /**
     * Create a {@link AbstractVectorStore}.
     *
     * @param ccf   the {@link Session} managing the underlying caches
     * @param sName the name of the vector store
     */
    public AbstractVectorStore(ConfigurableCacheFactory ccf, String sName)
        {
        this(ccf.ensureCache(sName, NullImplementation.getClassLoader()));
        }

    /**
     * Create a {@link AbstractVectorStore}.
     * <p>
     * <b>Note</b> the {@link NamedMap} must be a binary pass-thru instance as the code
     * in this store relies on the fact that {@link Binary} keys and values can be
     * passed-thru to the map unchanged.
     *
     * @param map  the {@link NamedMap} that holds the vector data.
     */
    @SuppressWarnings("unchecked")
    protected AbstractVectorStore(NamedMap<KeyType, ValueType> map)
        {
        CacheService             service    = map.getService();
        BackingMapManagerContext context    = service.getBackingMapManager().getContext();
        Serializer               serializer = service.getSerializer();
        service.getCluster().getDependencies().getSerializerMap();
        f_map     = map;
        f_sFormat = serializer.getName();
        if (context != null)
            {
            f_converterKeyToBinary     = context.getKeyToInternalConverter();
            f_converterValueToBinary   = context.getValueToInternalConverter();
            f_converterKeyFromBinary   = context.getKeyFromInternalConverter();
            f_converterValueFromBinary = context.getValueFromInternalConverter();
            }
        else
            {
            f_converterKeyToBinary     = o -> ExternalizableHelper.toBinary(o, serializer);
            f_converterValueToBinary   = o -> ExternalizableHelper.toBinary(o, serializer);
            f_converterKeyFromBinary   = bin -> ExternalizableHelper.fromBinary(bin, serializer);
            f_converterValueFromBinary = bin -> ExternalizableHelper.fromBinary(bin, serializer);
            }
        }

    public void clear()
        {
        f_map.clear();
        }

    public void destroy()
        {
        f_map.destroy();
        }

    public void release()
        {
        f_map.release();
        }

    public Service getService()
        {
        return f_map.getService();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the named {@link Serializer}.
     *
     * @param sFormat  the name of the serializer to return
     *
     * @return the named {@link Serializer}
     *
     * @throws IllegalStateException if there is no serializer factor configured with the specified name
     */
    protected Serializer ensureSerializer(String sFormat)
        {
        String sName = sFormat == null ? "" : sFormat.trim();
        Serializer serializer = s_mapSerializer.computeIfAbsent(sName, s ->
            {
            if (s.isEmpty() || sFormat.equals(s))
                {
                // empty string or same name as service serializer, so use the cache service's serializer
                return f_map.getService().getSerializer();
                }
            // specific name, so use the requested serializer
            SerializerFactory factory = f_map.getService().getCluster().getDependencies().getSerializerMap().get(s);
            return factory != null ? factory.createSerializer(Classes.getContextClassLoader()) : null;
            });
        if (serializer == null)
            {
            throw new IllegalStateException("No serializer factor has been configured with the name " + sName);
            }
        return serializer;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedMap} that contains the vector data.
     */
    protected final NamedMap<KeyType, ValueType> f_map;

    /**
     * The serialization format used by this store.
     */
    protected final String f_sFormat;

    /**
     * The {@link Converter} to convert vector keys to {@link Binary} values.
     */
    protected final Converter<Object, Binary> f_converterKeyToBinary;

    /**
     * The {@link Converter} to convert vector values and metadata to {@link Binary} values.
     */
    protected final Converter<Object, Binary> f_converterValueToBinary;

    /**
     * The {@link Converter} to convert vector {@link Binary} values to vector keys.
     */
    protected final Converter<Binary, Object> f_converterKeyFromBinary;

    /**
     * The {@link Converter} to convert {@link Binary} values to vector values and metadata.
     */
    protected final Converter<Binary, Object> f_converterValueFromBinary;

    /**
     * The map of serializers.
     */
    protected final Map<String, Serializer> s_mapSerializer = new ConcurrentHashMap<>();
    }
