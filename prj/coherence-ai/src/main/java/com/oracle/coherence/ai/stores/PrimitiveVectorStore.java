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

import com.oracle.coherence.ai.internal.BinaryVector;

import com.oracle.coherence.ai.results.BinaryQueryResult;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.io.nio.ByteBufferReadBuffer;

import com.tangosol.net.Session;

import com.tangosol.util.Binary;
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
 * @param <VectorType>    the type of the store (this will always be a primitive array type)
 * @param <KeyType>       the type of the key
 * @param <MetadataType>  the type of the metadata
 */
public abstract class PrimitiveVectorStore<VectorType, KeyType, MetadataType>
        extends AbstractVectorStore<KeyType, BinaryVector>
        implements VectorStore<VectorType, KeyType, MetadataType>
    {
    /**
     * Create a {@link PrimitiveVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public PrimitiveVectorStore(Session session, String sName)
        {
        super(session.getCache(sName));
        }

    // ----- VectorStore methods --------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Vector<VectorType, KeyType, MetadataType>> getVector(KeyType key)
        {
        BinaryVector binaryVector = f_map.get(key);
        if (binaryVector != null)
            {
            Serializer   serializer  = ensureSerializer(binaryVector.getFormat());
            MetadataType metadata    = binaryVector.getMetadata()
                    .map(ReadBuffer::toBinary)
                    .map(bin -> (MetadataType) ExternalizableHelper.fromBinary(bin, serializer))
                    .orElse(null);

            return Optional.ofNullable(createVector(key, binaryVector.getVector(), metadata));
            }
        return Optional.empty();
        }

    // ----- helper methods -------------------------------------------------

    protected abstract Vector<VectorType, KeyType, MetadataType> createVector(KeyType key, ReadBuffer vector, MetadataType metadata);

    /**
     * Add the specified vector to this store.
     *
     * @param vector  the vector to add
     */
    protected void addInternal(Vector<VectorType, KeyType, MetadataType> vector)
        {
        f_map.put(vector.getKey(), getBinaryVector(vector));
        }

    /**
     * Add the specified vector to this store.
     *
     * @param vectors the vectors to add
     */
    protected void addAllInternal(Stream<Vector<VectorType, KeyType, MetadataType>> vectors, int batch)
        {
        Map<KeyType, BinaryVector> map = new HashMap<>();

        vectors.forEach(vector ->
            {
            map.put(vector.getKey(), getBinaryVector(vector));
            if (map.size() == batch)
                {
                f_map.putAll(map);
                map.clear();
                }
            });

        if (!map.isEmpty())
            {
            f_map.putAll(map);
            }
        }

    protected BinaryVector getBinaryVector(Vector<VectorType, KeyType, MetadataType> vector)
        {
        MetadataType metadata     = vector.getMetadata().orElse(null);
        Binary       binMetadata  = metadata == null ? null : f_converterValueToBinary.convert(metadata);
        ReadBuffer   binVector    = new ByteBufferReadBuffer(vector.asBuffer());
        return new BinaryVector(binMetadata, f_sFormat, binVector);
        }

    @SuppressWarnings({"unchecked"})
    protected List<BinaryQueryResult> queryInternal(SimilarityQuery<VectorType> query)
        {
        Filter<?>                     filter     = query.getFilter();
        SimilarityAggregator<KeyType> aggregator = query.getAggregator();
        List<BinaryQueryResult>       listResult;

        if (filter == null)
            {
            listResult = f_map.aggregate(aggregator);
            }
        else
            {
            listResult = f_map.aggregate(new MetadataFilter<>(filter), aggregator);
            }

        return listResult;
        }
    }
