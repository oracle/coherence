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
 * of {@code int} values.
 *
 * @param <KeyType>       the type of the key
 * @param <MetadataType>  the type of the metadata
 */
public class IntVectorStore<KeyType, MetadataType>
        extends PrimitiveVectorStore<int[], KeyType, MetadataType>
    {
    /**
     * Create a {@link IntVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public IntVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public List<QueryResult<int[], KeyType, MetadataType>> query(SimilarityQuery<int[]> query)
        {
        List<BinaryQueryResult> list = queryInternal(query);
        return ConverterResult.listOfIntResults(list, f_converterValueFromBinary);
        }

    @Override
    protected Vector<int[], KeyType, MetadataType> createVector(KeyType key, ReadBuffer vector, MetadataType metadata)
        {
        return Vector.ofInts(Converters.intsFromReadBuffer(vector), key, metadata);
        }

    @Override
    public void add(KeyType key, int[] vector, MetadataType metadata)
        {
        addInts(key, vector, metadata);
        }

    @Override
    public void add(int[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
        {
        addInts(vectors, sequence, batch);
        }

    @Override
    public void add(Vector<int[], KeyType, MetadataType> vector)
        {
        addInts(vector);
        }

    @Override
    public void addDoubles(Vector<double[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asInts());
        }

    @Override
    public void addFloats(Vector<float[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asInts());
        }

    @Override
    public void addInts(Vector<int[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asInts());
        }

    @Override
    public void addLongs(Vector<long[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asInts());
        }

    @Override
    public void addShorts(Vector<short[], KeyType, MetadataType> vector)
        {
        addInternal(vector.asInts());
        }

    @Override
    public void addAll(Iterable<? extends Vector<?, KeyType, MetadataType>> vectors, int batch)
        {
        addAllInternal(StreamSupport.stream(vectors.spliterator(), false)

                .map(Vector::asInts), batch);
        }

    @Override
    public void addAll(Stream<? extends Vector<?, KeyType, MetadataType>> vectors, int batch)
        {
        addAllInternal(vectors.map(Vector::asInts), batch);
        }
    }
