/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.stores;

import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.SimilarityQuery;
import com.oracle.coherence.ai.Vector;

import com.oracle.coherence.ai.results.BinaryQueryResult;
import com.oracle.coherence.ai.results.ConverterResult;

import com.tangosol.net.NamedMap;

import com.tangosol.util.Binary;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link com.oracle.coherence.ai.VectorStore} that stores vectors
 * of {@code float} values.
 *
 * @param <K>  the type of the key
 * @param <M>  the type of the metadata
 */
public class FloatVectorStore<K, M>
        extends BaseVectorStore<float[], K, M>
    {
    /**
     * Create a {@link FloatVectorStore}.
     * <p>
     * <b>Note</b> the {@link NamedMap} must be a binary pass-thru instance as the code
     * in this store relies on the fact that {@link Binary} keys and values can be
     * passed-thru to the map unchanged.
     *
     * @param map  the {@link NamedMap} that holds the {@code float} vectors.
     */
    public FloatVectorStore(NamedMap<Binary, Binary> map)
        {
        super(map);
        }

    @Override
    public List<QueryResult<float[], K, M>> query(SimilarityQuery<float[]> query)
        {
        List<BinaryQueryResult> list = queryInternal(query);
        return ConverterResult.listOfFloatResults(list, f_converterValueFromBinary);
        }

    @Override
    public void add(K key, float[] vector, M metadata)
        {
        addFloats(key, vector, metadata);
        }

    @Override
    public void add(float[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addFloats(vectors, sequence, batch);
        }

    @Override
    public void add(Vector<float[], K, M> vector)
        {
        addFloats(vector);
        }

    @Override
    public void addDoubles(Vector<double[], K, M> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addFloats(Vector<float[], K, M> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addInts(Vector<int[], K, M> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addLongs(Vector<long[], K, M> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addShorts(Vector<short[], K, M> vector)
        {
        addInternal(vector.asFloats());
        }

    @Override
    public void addAll(Iterable<? extends Vector<?, K, M>> vectors, int batch)
        {
        addAllInternal(StreamSupport.stream(vectors.spliterator(), false)

                .map(Vector::asFloats), batch);
        }

    @Override
    public void addAll(Stream<? extends Vector<?, K, M>> vectors, int batch)
        {
        addAllInternal(vectors.map(Vector::asFloats), batch);
        }
    }
