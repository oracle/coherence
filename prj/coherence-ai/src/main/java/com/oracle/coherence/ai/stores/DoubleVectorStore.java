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
 * of {@code double} values.
 *
 * @param <KeyType>       the type of the key
 * @param <MetadataType>  the type of the metadata
 */
public class DoubleVectorStore<KeyType, MetadataType>
        extends PrimitiveVectorStore<double[], KeyType, MetadataType>
    {
    /**
     * Create a {@link DoubleKeylessVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public DoubleVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public List<QueryResult<double[], KeyType, MetadataType>> query(SimilarityQuery<double[]> query)
        {
        List<BinaryQueryResult> list = queryInternal(query);
        return ConverterResult.listOfDoubleResults(list, f_converterValueFromBinary);
        }

    @Override
    protected Vector<double[], KeyType, MetadataType> createVector(KeyType key, ReadBuffer vector, MetadataType metadata)
        {
        return Vector.ofDoubles(Converters.doublesFromReadBuffer(vector), key, metadata);
        }

    @Override
    public void add(KeyType key, double[] vector, MetadataType metadata)
        {
        addDoubles(key, vector, metadata);
        }

    @Override
    public void add(double[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
        {
        addDoubles(vectors, sequence, batch);
        }

    @Override
    public void add(Vector<double[], KeyType, MetadataType> vector)
        {
        addDoubles(vector);
        }

    @Override
    public void addDoubles(Vector<double[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addFloats(Vector<float[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addInts(Vector<int[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addLongs(Vector<long[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addShorts(Vector<short[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addAll(Iterable<? extends Vector<?, KeyType, MetadataType>> vectors, int batch)
        {
        addAllInternal(StreamSupport.stream(vectors.spliterator(), false)
                .map(Vector::asDoubles), batch);
        }

    @Override
    public void addAll(Stream<? extends Vector<?, KeyType, MetadataType>> vectors, int batch)
        {
        addAllInternal(vectors.map(Vector::asDoubles), batch);
        }
    }
