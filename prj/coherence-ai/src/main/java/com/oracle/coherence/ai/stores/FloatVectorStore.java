/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.stores;

import com.oracle.coherence.ai.Converters;
import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.SimilarityQuery;
import com.oracle.coherence.ai.Vector;

import com.oracle.coherence.ai.results.BinaryQueryResult;
import com.oracle.coherence.ai.results.ConverterResult;

import com.tangosol.io.ReadBuffer;

import com.tangosol.net.Session;

import java.util.List;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link com.oracle.coherence.ai.VectorStore} that stores vectors
 * of {@code float} values.
 *
 * @param <KeyType>       the type of the key
 * @param <MetadataType>  the type of the metadata
 */
public class FloatVectorStore<KeyType, MetadataType>
        extends PrimitiveVectorStore<float[], KeyType, MetadataType>
    {
    /**
     * Create a {@link FloatVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public FloatVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public List<QueryResult<float[], KeyType, MetadataType>> query(SimilarityQuery<float[]> query)
        {
        List<BinaryQueryResult> list = queryInternal(query);
        return ConverterResult.listOfFloatResults(list, f_converterValueFromBinary);
        }

    @Override
    protected Vector<float[], KeyType, MetadataType> createVector(KeyType key, ReadBuffer vector, MetadataType metadata)
        {
        return Vector.ofFloats(Converters.floatsFromReadBuffer(vector), key, metadata);
        }

    @Override
    public void add(KeyType key, float[] vector, MetadataType metadata)
        {
        addFloats(key, vector, metadata);
        }

    @Override
    public void add(float[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
        {
        addFloats(vectors, sequence, batch);
        }

    @Override
    public void add(Vector<float[], KeyType, MetadataType> vector)
        {
        addFloats(vector);
        }

    @Override
    public void addDoubles(Vector<double[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addFloats(Vector<float[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addInts(Vector<int[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addLongs(Vector<long[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addShorts(Vector<short[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addAll(Iterable<? extends Vector<?, KeyType, MetadataType>> vectors, int batch)
        {
        addAllInternal(StreamSupport.stream(vectors.spliterator(), false)

                .map(Vector::asFloats), batch);
        }

    @Override
    public void addAll(Stream<? extends Vector<?, KeyType, MetadataType>> vectors, int batch)
        {
        addAllInternal(vectors.map(Vector::asFloats), batch);
        }
    }
