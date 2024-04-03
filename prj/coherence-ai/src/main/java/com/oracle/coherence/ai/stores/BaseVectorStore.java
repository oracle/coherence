/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.stores;

import com.oracle.coherence.ai.SimilarityQuery;
import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.VectorStore;

import com.oracle.coherence.ai.aggregators.SimilarityAggregator;

import com.oracle.coherence.ai.filters.MetadataFilter;

import com.oracle.coherence.ai.results.BinaryQueryResult;

import com.tangosol.io.ReadBuffer;

import com.tangosol.io.Serializer;

import com.tangosol.io.nio.ByteBufferReadBuffer;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedMap;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A base class for {@link VectorStore} implementations.
 *
 * @param <V>  the type of the store (this will always be a primitive array type)
 * @param <K>  the type of the key
 * @param <M>  the type of the metadata
 */
public abstract class BaseVectorStore<V, K, M>
        implements VectorStore<V, K, M>
    {
    /**
     * Create a {@link BaseVectorStore}.
     * <p>
     * <b>Note</b> the {@link NamedMap} must be a binary pass-thru instance as the code
     * in this store relies on the fact that {@link Binary} keys and values can be
     * passed-thru to the map unchanged.
     *
     * @param map  the {@link NamedMap} that holds the vector data.
     */
    @SuppressWarnings("unchecked")
    public BaseVectorStore(NamedMap<Binary, Binary> map)
        {
        f_map = map;
        CacheService             service = map.getService();
        BackingMapManagerContext context = service.getBackingMapManager().getContext();
        if (context != null)
            {
            f_converterKeyToBinary     = context.getKeyToInternalConverter();
            f_converterValueToBinary   = context.getValueToInternalConverter();
            f_converterKeyFromBinary   = context.getKeyFromInternalConverter();
            f_converterValueFromBinary = context.getValueFromInternalConverter();
            }
        else
            {
            Serializer serializer = service.getSerializer();
            f_converterKeyToBinary     = o -> ExternalizableHelper.toBinary(o, serializer);
            f_converterValueToBinary   = o -> ExternalizableHelper.toBinary(o, serializer);
            f_converterKeyFromBinary   = bin -> ExternalizableHelper.fromBinary(bin, serializer);
            f_converterValueFromBinary = bin -> ExternalizableHelper.fromBinary(bin, serializer);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Add the specified vector to this store.
     *
     * @param vector  the vector to add
     */
    protected void addInternal(Vector<V, K, M> vector)
        {
        NamedMap<Binary, Binary> namedMap = getNamedMap();
        Binary                   binKey   = f_converterKeyToBinary.convert(vector.getKey());
        Binary                   binValue = getBinaryVector(vector);
        namedMap.put(binKey, binValue);
        }

    /**
     * Add the specified vector to this store.
     *
     * @param vectors the vectors to add
     */
    protected void addAllInternal(Stream<Vector<V, K, M>> vectors, int batch)
        {
        NamedMap<Binary, Binary> namedMap = getNamedMap();
        Map<Binary, Binary>      map      = new HashMap<>();
        int                      count    = 0;

        vectors.forEach(vector ->
            {
            map.put(f_converterKeyToBinary.convert(vector.getKey()), getBinaryVector(vector));
            if (map.size() == batch)
                {
                namedMap.putAll(map);
                map.clear();
                }
            });

        if (!map.isEmpty())
            {
            namedMap.putAll(map);
            }
        }

    protected Binary getBinaryVector(Vector<V, K, M> vector)
        {
        ByteBufferReadBuffer     buffer   = new ByteBufferReadBuffer(vector.asBuffer());
        Binary                   binValue = buffer.toBinary();
        Optional<M>              metadata = vector.getMetadata();
        if (metadata.isPresent())
            {
            Binary binMeta = f_converterValueToBinary.convert(metadata.get());
            binValue = BaseVectorStore.addMetadata(binValue, binMeta).toBinary();
            }
        return binValue;
        }

    /**
     * Return the {@link NamedMap} containing the vector data.
     *
     * @return the {@link NamedMap} containing the vector data
     */
    public NamedMap<Binary, Binary> getNamedMap()
        {
        return f_map;
        }

    /**
     * Return a {@link ReadBuffer} containing the binary representation
     * of a vector stored with the specified key.
     *
     * @param key  the key of the vector to obtain
     *
     * @return a {@link ReadBuffer} containing the binary representation
     *         of a vector stored with the specified key, or {@code null}
     *         if no vector is stored with the specified key
     */
    public ReadBuffer getBinaryVector(K key)
        {
        return f_map.get(f_converterKeyToBinary.convert(key));
        }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    protected List<BinaryQueryResult> queryInternal(SimilarityQuery<V> query)
        {
        Filter<?>            filter     = query.getFilter();
        SimilarityAggregator aggregator = query.getAggregator();
        Object               binResult;

        if (filter == null)
            {
            binResult = f_map.aggregate(aggregator);
            }
        else
            {
            binResult = f_map.aggregate(new MetadataFilter<>(filter), aggregator);
            }

        return (List<BinaryQueryResult>) f_converterValueFromBinary.convert((Binary) binResult);
        }

    /**
     * Decorate a binary value with its binary metadata.
     *
     * @param binVector    a {@link ReadBuffer} containing the vector data
     * @param binMetadata  a {@link ReadBuffer} containing the metadata
     *
     * @return a decorated binary containing both the vector and metadata
     */
    public static ReadBuffer addMetadata(ReadBuffer binVector, ReadBuffer binMetadata)
        {
        return ExternalizableHelper.decorate(binVector, ExternalizableHelper.DECO_RSVD_1, binMetadata);
        }

    /**
     * Extract the metadata from a decorated binary vector.
     *
     * @param binaryValue  the decorated binary vector data
     *
     * @return  the binary metadata or {@code null} if there was no metadata
     */
    public static Binary getMetadata(Binary binaryValue)
        {
        ReadBuffer buffer = ExternalizableHelper.getDecoration((ReadBuffer) binaryValue, DECO_METADATA);
        return buffer == null ? null : buffer.toBinary();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The decoration identifier used to add metadata to a {@link Binary}.
     */
    public static final int DECO_METADATA = ExternalizableHelper.DECO_RSVD_1;

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedMap} that contains the vector data.
     */
    protected final NamedMap<Binary, Binary> f_map;

    /**
     * The {@link Converter} to convert vector keys to {@link Binary} values.
     */
    protected final Converter<K, Binary> f_converterKeyToBinary;

    /**
     * The {@link Converter} to convert vector values and metadata to {@link Binary} values.
     */
    protected final Converter<Object, Binary> f_converterValueToBinary;

    /**
     * The {@link Converter} to convert vector {@link Binary} values to vector keys.
     */
    protected final Converter<Binary, K> f_converterKeyFromBinary;

    /**
     * The {@link Converter} to convert {@link Binary} values to vector values and metadata.
     */
    protected final Converter<Binary, Object> f_converterValueFromBinary;
    }
