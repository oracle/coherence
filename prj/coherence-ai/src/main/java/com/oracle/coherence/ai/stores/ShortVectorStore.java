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
 * of {@code short} values.
 *
 * @param <K>  the type of the key
 * @param <M>  the type of the metadata
 */
public class ShortVectorStore<K, M>
        extends PrimitiveVectorStore<short[], K, M>
    {
    /**
     * Create a {@link ShortVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public ShortVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public List<QueryResult<short[], K, M>> query(SimilarityQuery<short[]> query)
        {
        List<BinaryQueryResult> list = queryInternal(query);
        return ConverterResult.listOfShortResults(list, f_converterValueFromBinary);
        }

    @Override
    protected Vector<short[], K, M> createVector(K key, ReadBuffer vector, M metadata)
        {
        return Vector.ofShorts(Converters.shortsFromReadBuffer(vector), key, metadata);
        }

    @Override
    public void add(K key, short[] vector, M metadata)
        {
        addShorts(key, vector, metadata);
        }

    @Override
    public void add(short[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addShorts(vectors, sequence, batch);
        }

    @Override
    public void add(Vector<short[], K, M> vector)
        {
        addShorts(vector);
        }

    @Override
    public void addDoubles(Vector<double[], K, M> vector)
        {
        addInternal(vector.asShorts());
        }

    @Override
    public void addFloats(Vector<float[], K, M> vector)
        {
        addInternal(vector.asShorts());
        }

    @Override
    public void addInts(Vector<int[], K, M> vector)
        {
        addInternal(vector.asShorts());
        }

    @Override
    public void addLongs(Vector<long[], K, M> vector)
        {
        addInternal(vector.asShorts());
        }

    @Override
    public void addShorts(Vector<short[], K, M> vector)
        {
        addInternal(vector.asShorts());
        }

    @Override
    public void addAll(Iterable<? extends Vector<?, K, M>> vectors, int batch)
        {
        addAllInternal(StreamSupport.stream(vectors.spliterator(), false)

                .map(Vector::asShorts), batch);
        }

    @Override
    public void addAll(Stream<? extends Vector<?, K, M>> vectors, int batch)
        {
        addAllInternal(vectors.map(Vector::asShorts), batch);
        }
    }
