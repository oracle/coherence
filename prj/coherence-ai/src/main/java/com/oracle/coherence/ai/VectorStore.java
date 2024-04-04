/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.oracle.coherence.ai.config.VectorStoreSessionConfig;

import com.oracle.coherence.ai.stores.DoubleVectorStore;
import com.oracle.coherence.ai.stores.FloatVectorStore;
import com.oracle.coherence.ai.stores.IntVectorStore;
import com.oracle.coherence.ai.stores.LongVectorStore;
import com.oracle.coherence.ai.stores.ShortVectorStore;

import com.tangosol.net.Coherence;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
 * @param <VectorType>    the type of the store (this will always be a primitive array type)
 * @param <KeyType>       the type of the key
 * @param <MetadataType>  the type of the metadata
 */
public interface VectorStore<VectorType, KeyType, MetadataType>
    {
    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     */
    default void addAll(Iterable<? extends Vector<?, KeyType, MetadataType>> vectors)
        {
        addAll(vectors, DEFAULT_BATCH);
        }

    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     * @param batch    the size of the batches of vectors to store at one time
     */
    void addAll(Iterable<? extends Vector<?, KeyType, MetadataType>> vectors, int batch);

    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     */
    default void addAll(Stream<? extends Vector<?, KeyType, MetadataType>> vectors)
        {
        addAll(vectors, DEFAULT_BATCH);
        }

    /**
     * Add all the vector to this store.
     *
     * @param vectors  the vectors to add
     * @param batch    the size of the batches of vectors to store at one time
     */
    void addAll(Stream<? extends Vector<?, KeyType, MetadataType>> vectors, int batch);

    /**
     * Add a vector of {@code V} values to this store.
     *
     * @param key       the key of the vector
     * @param vector    the vector of values to add
     * @param metadata  optional metadata for the vector
     */
    void add(KeyType key, VectorType vector, MetadataType metadata);

    /**
     * Add a vector of {@code V} values to this store.
     *
     * @param key     the key of the vector
     * @param vector  the vector of values to add
     */
    default void add(KeyType key, VectorType vector)
        {
        add(key, vector, null);
        }

    /**
     * Add all the vector of {@code V} values to this store.
     *
     * @param vectors   the vectors of values to add
     * @param sequence  the key generator to use
     */
    default void add(VectorType[] vectors, Vector.KeySequence<KeyType> sequence)
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
    void add(VectorType[] vectors, Vector.KeySequence<KeyType> sequence, int batch);

    /**
     * Add a {@code double} vector to this store.
     *
     * @param vector  the vector to add
     */
    void add(Vector<VectorType, KeyType, MetadataType> vector);

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
    default void addDoubles(KeyType key, double[] vector, MetadataType metadata)
        {
        addDoubles(Vector.ofDoubles(vector, key, metadata));
        }

    /**
     * Add a vector of {@code double} values to this store.
     *
     * @param key     the key of the vector
     * @param vector  the vector of double values to add
     */
    default void addDoubles(KeyType key, double[] vector)
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
    default void addDoubles(double[][] vectors, Vector.KeySequence<KeyType> sequence)
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
    default void addDoubles(double[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
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
    void addDoubles(Vector<double[], KeyType, MetadataType> vector);

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
    default void addFloats(KeyType key, float[] vector, MetadataType metadata)
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
    default void addFloats(KeyType key, float[] vector)
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
    default void addFloats(float[][] vectors, Vector.KeySequence<KeyType> sequence)
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
    default void addFloats(float[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
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
    void addFloats(Vector<float[], KeyType, MetadataType> vector);

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
    default void addInts(KeyType key, int[] vector, MetadataType metadata)
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
    default void addInts(KeyType key, int[] vector)
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
    default void addInts(int[][] vectors, Vector.KeySequence<KeyType> sequence)
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
    default void addInts(int[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
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
    void addInts(Vector<int[], KeyType, MetadataType> vector);

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
    default void addLongs(KeyType key, long[] vector, MetadataType metadata)
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
    default void addLongs(KeyType key, long[] vector)
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
    default void addLongs(long[][] vectors, Vector.KeySequence<KeyType> sequence)
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
    default void addLongs(long[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
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
    void addLongs(Vector<long[], KeyType, MetadataType> vector);

    /**
     * Add a vector of {@code short} values to this store.
     *
     * @param key       the key of the vector
     * @param vector    the vector of short values to add
     * @param metadata  optional metadata for the vector
     */
    default void addShorts(KeyType key, short[] vector, MetadataType metadata)
        {
        addShorts(Vector.ofShorts(vector, key, metadata));
        }

    /**
     * Add a vector of {@code short} values to this store.
     *
     * @param key       the key of the vector
     * @param vector    the vector of short values to add
     */
    default void addShorts(KeyType key, short[] vector)
        {
        addShorts(key, vector, null);
        }

    /**
     * Add all the vector of {@code double} values to this store.
     *
     * @param vectors   the vectors of double values to add
     * @param sequence  the key generator to use
     */
    default void addShorts(short[][] vectors, Vector.KeySequence<KeyType> sequence)
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
    default void addShorts(short[][] vectors, Vector.KeySequence<KeyType> sequence, int batch)
        {
        addAll(Arrays.stream(vectors).map(v -> Vector.ofShorts(v, sequence.next())), batch);
        }

    /**
     * Add a {@code short} vector to this store.
     *
     * @param vector  the vector to add
     */
    void addShorts(Vector<short[], KeyType, MetadataType> vector);

    /**
     * Query this vector store.
     *
     * @param query  the {@link SimilarityQuery} to execute
     *
     * @return  the results of executing the query as a {@link List} of {@link QueryResult} instances
     */
    List<QueryResult<VectorType, KeyType, MetadataType>> query(SimilarityQuery<VectorType> query);

    /**
     * Return the {@link Vector} with the specified key.
     *
     * @param key  the key of the {@link Vector} to obtain
     *
     * @return an {@link Optional} containing the {@link Vector}, or an
     *         empty {@link Optional} if the store does not contain a
     *         {@link Vector} mapped to the key
     */
    Optional<Vector<VectorType, KeyType, MetadataType>> getVector(KeyType key);

    /**
     * Clear the contents of the store.
     */
    void clear();

    /**
     * Destroy the store.
     */
    void destroy();

    /**
     * Release local references to the store.
     */
    void release();

    /**
     * Return the service managing the underlying vector storage.
     *
     * @return  the service managing the underlying vector storage
     */
    Service getService();

    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link VectorStore} for vectors of {@code double} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code double} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<double[], KeyType, MetadataType> ofDoubles(String name)
        {
        return ofDoubles(name, session());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code double} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code double} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<double[], KeyType, MetadataType> ofDoubles(String name, Session session)
        {
        return new DoubleVectorStore<>(session, name);
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code float} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code float} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<float[], KeyType, MetadataType> ofFloats(String name)
        {
        return ofFloats(name, session());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code float} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code float} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<float[], KeyType, MetadataType> ofFloats(String name, Session session)
        {
        return new FloatVectorStore<>(session, name);
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code int} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code int} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<int[], KeyType, MetadataType> ofInts(String name)
        {
        return ofInts(name, session());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code int} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code int} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<int[], KeyType, MetadataType> ofInts(String name, Session session)
        {
        return new IntVectorStore<>(session, name);
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code long} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code long} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<long[], KeyType, MetadataType> ofLongs(String name)
        {
        return ofLongs(name, session());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code long} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code long} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<long[], KeyType, MetadataType> ofLongs(String name, Session session)
        {
        return new LongVectorStore<>(session, name);
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code short} instances.
     *
     * @param name  the name of the vector store
     *
     * @return a {@link VectorStore} for vectors of {@code short} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<short[], KeyType, MetadataType> ofShorts(String name)
        {
        return ofShorts(name, session());
        }

    /**
     * Create a {@link VectorStore} for vectors of {@code short} instances.
     *
     * @param name     the name of the vector store
     * @param session  the {@link Session} owning the store
     *
     * @return a {@link VectorStore} for vectors of {@code short} instances
     *
     * @param <KeyType>       the type of the keys for the vectors
     * @param <MetadataType>  the type of the optional metadata
     */
    static <KeyType, MetadataType> VectorStore<short[], KeyType, MetadataType> ofShorts(String name, Session session)
        {
        return new ShortVectorStore<>(session, name);
        }

    /**
     * Obtain the Coherence AI {@link Session}.
     * 
     * @return the Coherence AI {@link Session}
     */
    static Session session()
        {
        return Coherence.findSession(VectorStoreSessionConfig.SCOPE_NAME)
                .orElseThrow(() -> new IllegalStateException(String.format(
                    "The session '%s' has not been initialized, Coherence should be started using the boostrap API" +
                            " or by running Coherence.main()",
                            VectorStoreSessionConfig.SCOPE_NAME)));
        }
    
    /**
     * The default batch size for batch operations.
     */
    int DEFAULT_BATCH = 100;
    }
