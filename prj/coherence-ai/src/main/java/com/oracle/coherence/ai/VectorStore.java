/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.oracle.coherence.ai.stores.DoubleVectorStore;
import com.oracle.coherence.ai.stores.FloatVectorStore;
import com.oracle.coherence.ai.stores.IntVectorStore;
import com.oracle.coherence.ai.stores.LongVectorStore;
import com.oracle.coherence.ai.stores.ShortVectorStore;

import com.tangosol.net.Session;

import com.tangosol.net.options.WithClassLoader;

import com.tangosol.util.NullImplementation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * A store of vectors.
 * <p>
 * Each vector is identified by a key and has optional metadata.
 * <p>
 * Coherence vector stores use primitive array values for the vector data.
 * This is so that the vectors can be stored and operated on more efficiently
 * than using Object number values and Java collections. The array of vector data
 * is stored in Coherence as a binary blob of data containing the memory
 * representation of the vector array.
 * <p>
 * <b>Note</b> it is up to the application developer to ensure that the values
 * added match the vector type of the store.
 * <p>
 * For example, if vector of {@code double} values
 * can be added to a vector of lower precision values the {@code double}
 * values will be downcast to the lower precision. For example adding
 * {@code double} values to a vector store of {@code float} values will
 * work if all the {@code double} values fit into a {@code float}.
 * Any {@code double} value larger than a {@code float} would be downcast
 * to the wrong value.
 *
 * @param <V>  the type of the store (this will always be a primitive array type)
 * @param <K>  the type of the key
 * @param <M>  the type of the metadata
 */
public interface VectorStore<V, K, M>
    {
    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     */
    default void addAll(Iterable<? extends Vector<?, K, M>> vectors)
        {
        addAll(vectors, DEFAULT_BATCH);
        }

    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     * @param batch    the size of the batches of vectors to store at one time
     */
    void addAll(Iterable<? extends Vector<?, K, M>> vectors, int batch);

    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     */
    default void addAll(Stream<? extends Vector<?, K, M>> vectors)
        {
        addAll(vectors, DEFAULT_BATCH);
        }

    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     * @param batch    the size of the batches of vectors to store at one time
     */
    void addAll(Stream<? extends Vector<?, K, M>> vectors, int batch);

    /**
     * Add a vector of {@code V} values to this store.
     *
     * @param key       the key of the vector
     * @param vector    the vector of values to add
     * @param metadata  optional metadata for the vector
     */
    void add(K key, V vector, M metadata);

    /**
     * Add a vector of {@code V} values to this store.
     *
     * @param key     the key of the vector
     * @param vector  the vector of values to add
     */
    default void add(K key, V vector)
        {
        add(key, vector, null);
        }

    /**
     * Add all the vector of {@code V} values to this store.
     *
     * @param vectors   the vectors of values to add
     * @param sequence  the key generator to use
     */
    default void add(V[] vectors, Vector.KeySequence<K> sequence)
        {
        add(vectors, sequence, DEFAULT_BATCH);
        }

    /**
     * Add all the vector of {@code V} values to this store.
     *
     * @param vectors   the vectors of values to add
     * @param sequence  the key generator to use
     * @param batch     the size of the batches of vectors to store at one time
     */
    void add(V[] vectors, Vector.KeySequence<K> sequence, int batch);

    /**
     * Add a {@code double} vector to this store.
     *
     * @param vector  the vector to add
     */
    void add(Vector<V, K, M> vector);

    /**
     * Add a vector of {@code double} values to this store.
     * <p>
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param key       the key of the vector
     * @param vector    the vector of double values to add
     * @param metadata  optional metadata for the vector
     */
    default void addDoubles(K key, double[] vector, M metadata)
        {
        addDoubles(Vector.ofDoubles(vector, key, metadata));
        }

    /**
     * Add a vector of {@code double} values to this store.
     *
     * @param key     the key of the vector
     * @param vector  the vector of double values to add
     */
    default void addDoubles(K key, double[] vector)
        {
        addDoubles(key, vector, null);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     */
    default void addDoubles(double[][] vectors, Vector.KeySequence<K> sequence)
        {
        addDoubles(vectors, sequence, DEFAULT_BATCH);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     * @param batch     the size of the batches of vectors to store at one time
     */
    default void addDoubles(double[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addAll(Arrays.stream(vectors).map(v -> Vector.ofDoubles(v, sequence.next())), batch);
        }

    /**
     * Add a {@code double} vector to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vector  the vector to add
     */
    void addDoubles(Vector<double[], K, M> vector);

    /**
     * Add a vector of {@code float} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param key       the key of the vector
     * @param vector    the vector of float values to add
     * @param metadata  optional metadata for the vector
     */
    default void addFloats(K key, float[] vector, M metadata)
        {
        addFloats(Vector.ofFloats(vector, key, metadata));
        }

    /**
     * Add a vector of {@code float} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param key     the key of the vector
     * @param vector  the vector of float values to add
     */
    default void addFloats(K key, float[] vector)
        {
        addFloats(key, vector, null);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     */
    default void addFloats(float[][] vectors, Vector.KeySequence<K> sequence)
        {
        addFloats(vectors, sequence, DEFAULT_BATCH);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     * @param batch     the size of the batches of vectors to store at one time
     */
    default void addFloats(float[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addAll(Arrays.stream(vectors).map(v -> Vector.ofFloats(v, sequence.next())), batch);
        }

    /**
     * Add a {@code float} vector to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vector  the vector to add
     */
    void addFloats(Vector<float[], K, M> vector);

    /**
     * Add a vector of {@code int} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param key       the key of the vector
     * @param vector    the vector of int values to add
     * @param metadata  optional metadata for the vector
     */
    default void addInts(K key, int[] vector, M metadata)
        {
        addInts(Vector.ofInts(vector, key, metadata));
        }

    /**
     * Add a vector of {@code int} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param key     the key of the vector
     * @param vector  the vector of int values to add
     */
    default void addInts(K key, int[] vector)
        {
        addInts(key, vector, null);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     */
    default void addInts(int[][] vectors, Vector.KeySequence<K> sequence)
        {
        addInts(vectors, sequence, DEFAULT_BATCH);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     * @param batch     the size of the batches of vectors to store at one time
     */
    default void addInts(int[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addAll(Arrays.stream(vectors).map(v -> Vector.ofInts(v, sequence.next())), batch);
        }

    /**
     * Add a {@code int} vector to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vector  the vector to add
     */
    void addInts(Vector<int[], K, M> vector);

    /**
     * Add a vector of {@code long} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param key       the key of the vector
     * @param vector    the vector of long values to add
     * @param metadata  optional metadata for the vector
     */
    default void addLongs(K key, long[] vector, M metadata)
        {
        addLongs(Vector.ofLongs(vector, key, metadata));
        }

    /**
     * Add a vector of {@code long} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param key       the key of the vector
     * @param vector    the vector of long values to add
     */
    default void addLongs(K key, long[] vector)
        {
        addLongs(key, vector, null);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     */
    default void addLongs(long[][] vectors, Vector.KeySequence<K> sequence)
        {
        addLongs(vectors, sequence, DEFAULT_BATCH);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     * @param batch     the size of the batches of vectors to store at one time
     */
    default void addLongs(long[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addAll(Arrays.stream(vectors).map(v -> Vector.ofLongs(v, sequence.next())), batch);
        }

    /**
     * Add a {@code long} vector to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     *
     * @param vector  the vector to add
     */
    void addLongs(Vector<long[], K, M> vector);

    /**
     * Add a vector of {@code short} values to this store.
     *
     * @param key       the key of the vector
     * @param vector    the vector of short values to add
     * @param metadata  optional metadata for the vector
     */
    default void addShorts(K key, short[] vector, M metadata)
        {
        addShorts(Vector.ofShorts(vector, key, metadata));
        }

    /**
     * Add a vector of {@code short} values to this store.
     *
     * @param key       the key of the vector
     * @param vector    the vector of short values to add
     */
    default void addShorts(K key, short[] vector)
        {
        addShorts(key, vector, null);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     */
    default void addShorts(short[][] vectors, Vector.KeySequence<K> sequence)
        {
        addShorts(vectors, sequence, DEFAULT_BATCH);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     * @param batch     the size of the batches of vectors to store at one time
     */
    default void addShorts(short[][] vectors, Vector.KeySequence<K> sequence, int batch)
        {
        addAll(Arrays.stream(vectors).map(v -> Vector.ofShorts(v, sequence.next())), batch);
        }

    /**
     * Add a {@code short} vector to this store.
     *
     * @param vector  the vector to add
     */
    void addShorts(Vector<short[], K, M> vector);

    /**
     * Query this vector store.
     *
     * @param query  the {@link SimilarityQuery} to execute
     *
     * @return  the results of executing the query as a {@link List} of {@link QueryResult} instances
     */
    List<QueryResult<V, K, M>> query(SimilarityQuery<V> query);

    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link VectorStore} for vectors of {@code double} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code double} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<double[], K, M> ofDoubles(String name)
        {
        return ofDoubles(name, Session.create());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code double} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code double} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<double[], K, M> ofDoubles(String name, Session session)
        {
        return new DoubleVectorStore<>(session.getCache(name,
                WithClassLoader.using(NullImplementation.getClassLoader())));
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code float} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code float} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<float[], K, M> ofFloats(String name)
        {
        return ofFloats(name, Session.create());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code float} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code float} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<float[], K, M> ofFloats(String name, Session session)
        {
        return new FloatVectorStore<>(session.getCache(name,
                WithClassLoader.using(NullImplementation.getClassLoader())));
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code int} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code int} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<int[], K, M> ofInts(String name)
        {
        return ofInts(name, Session.create());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code int} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code int} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<int[], K, M> ofInts(String name, Session session)
        {
        return new IntVectorStore<>(session.getCache(name,
                WithClassLoader.using(NullImplementation.getClassLoader())));
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code long} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code long} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<long[], K, M> ofLongs(String name)
        {
        return ofLongs(name, Session.create());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code long} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code long} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<long[], K, M> ofLongs(String name, Session session)
        {
        return new LongVectorStore<>(session.getCache(name,
                WithClassLoader.using(NullImplementation.getClassLoader())));
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code short} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code short} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<short[], K, M> ofShorts(String name)
        {
        return ofShorts(name, Session.create());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code short} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code short} instances
     *
     * @param <K>  the type of the keys for the vectors
     * @param <M>  the type of the optional metadata
     */
    static <K, M> VectorStore<short[], K, M> ofShorts(String name, Session session)
        {
        return new ShortVectorStore<>(session.getCache(name,
                WithClassLoader.using(NullImplementation.getClassLoader())));
        }

    /**
     * The default batch size for batch operations.
      */
    int DEFAULT_BATCH = 100;
    }
