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
 * of {@code double} values.
 *
 * @param <K>  the type of the key
 * @param <M>  the type of the metadata
 */
public class DoubleVectorStore<K, M>
        extends BaseVectorStore<double[], K, M>
    {
    /**
     * Create a {@link DoubleVectorStore}.
     * <p>
     * <b>Note</b> the {@link NamedMap} must be a binary pass-thru instance as the code
     * in this store relies on the fact that {@link Binary} keys and values can be
     * passed-thru to the map unchanged.
     *
     * @param map  the {@link NamedMap} that holds the {@code double} vectors.
     */
    public DoubleVectorStore(NamedMap<Binary, Binary> map)
        {
        super(map);
        }

    @Override
    public List<QueryResult<double[], K, M>> query(SimilarityQuery<double[]> query)
        {
        List<BinaryQueryResult> list = queryInternal(query);
        return ConverterResult.listOfDoubleResults(list, f_converterValueFromBinary);
        }

    @Override
    public void add(K key, double[] vector, M metadata)
        {
        addDoubles(key, vector, metadata);
        }

    @Override
    public void add(double[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addDoubles(vectors, sequence, batch);
        }

    @Override
    public void add(Vector<double[], K, M> vector)
        {
        addDoubles(vector);
        }

    @Override
    public void addDoubles(Vector<double[], K, M> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addFloats(Vector<float[], K, M> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addInts(Vector<int[], K, M> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addLongs(Vector<long[], K, M> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addShorts(Vector<short[], K, M> vector)
        {
        addInternal(vector.asDoubles());
        }

    @Override
    public void addAll(Iterable<? extends Vector<?, K, M>> vectors, int batch)
        {
        addAllInternal(StreamSupport.stream(vectors.spliterator(), false)

                .map(Vector::asDoubles), batch);
        }

    @Override
    public void addAll(Stream<? extends Vector<?, K, M>> vectors, int batch)
        {
        addAllInternal(vectors.map(Vector::asDoubles), batch);
        }
    }
