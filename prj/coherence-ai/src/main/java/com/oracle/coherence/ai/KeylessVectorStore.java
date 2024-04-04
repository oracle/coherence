/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.oracle.coherence.ai.stores.DoubleKeylessVectorStore;
import com.oracle.coherence.ai.stores.FloatKeylessVectorStore;
import com.oracle.coherence.ai.stores.IntKeylessVectorStore;
import com.oracle.coherence.ai.stores.LongKeylessVectorStore;
import com.oracle.coherence.ai.stores.ShortKeylessVectorStore;

import com.tangosol.net.Session;

import java.util.Arrays;

/**
 * A {@link VectorStore} that uses generated unique keys.
 *
 * @param <VectorType>    the type of the store (this will always be a primitive array type)
 * @param <MetadataType>  the type of the metadata
 */
public interface KeylessVectorStore<VectorType, MetadataType>
        extends VectorStore<VectorType, Vector.Key, MetadataType>
    {
    /**
     * Add a vector of {@code V} values to this store.
     *
     * @param vector    the vector of values to add
     * @param metadata  optional metadata for the vector
     */
    void addVector(VectorType vector, MetadataType metadata);

    /**
     * Add a vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vector  the vector of double values to add
     */
    void addVector(VectorType vector);

    /**
     * Add an array of vectors of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code double} values to add
     */
    void addVectors(VectorType[] vectors);

    /**
     * Add an array of vectors of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code double} values to add
     */
    void addVectors(VectorType[] vectors, int batch);

    /**
     * Add a vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vector    the vector of double values to add
     * @param metadata  optional metadata for the vector
     */
    default void addDoubles(double[] vector, MetadataType metadata)
        {
        addDoubles(Vector.Key.withId(0), vector, metadata);
        }

    /**
     * Add a vector of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vector  the vector of double values to add
     */
    default void addDoubles(double[] vector)
        {
        addDoubles(Vector.Key.withId(0), vector, null);
        }

    /**
     * Add an array of vectors of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code double} values to add
     */
    default void addDoubles(double[][] vectors)
        {
        addDoubles(vectors, DEFAULT_BATCH);
        }

    /**
     * Add an array of vectors of {@code double} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code double} values to add
     */
    default void addDoubles(double[][] vectors, int batch)
        {
        Vector.KeySequence<Vector.Key> keySequence = Vector.Key.newSequence();
        addAll(Arrays.stream(vectors).map(v -> Vector.ofDoubles(v, keySequence.next())), batch);
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
     * @param vector    the vector of float values to add
     * @param metadata  optional metadata for the vector
     */
    default void addFloats(float[] vector, MetadataType metadata)
        {
        addFloats(Vector.Key.withId(0), vector, metadata);
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
     * @param vector  the vector of float values to add
     */
    default void addFloats(float[] vector)
        {
        addFloats(Vector.Key.withId(0), vector, null);
        }

    /**
     * Add an array of vectors of {@code float} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code float} values to add
     */
    default void addFloats(float[][] vectors)
        {
        addFloats(vectors, DEFAULT_BATCH);
        }

    /**
     * Add an array of vectors of {@code float} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code float} values to add
     */
    default void addFloats(float[][] vectors, int batch)
        {
        Vector.KeySequence<Vector.Key> keySequence = Vector.Key.newSequence();
        addAll(Arrays.stream(vectors).map(v -> Vector.ofFloats(v, keySequence.next())), batch);
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
     * @param vector    the vector of int values to add
     * @param metadata  optional metadata for the vector
     */
    default void addInts(int[] vector, MetadataType metadata)
        {
        addInts(Vector.Key.withId(0), vector, metadata);
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
     * @param vector  the vector of int values to add
     */
    default void addInts(int[] vector)
        {
        addInts(Vector.Key.withId(0), vector, null);
        }

    /**
     * Add an array of vectors of {@code int} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code int} values to add
     */
    default void addInts(int[][] vectors)
        {
        addInts(vectors, DEFAULT_BATCH);
        }

    /**
     * Add an array of vectors of {@code int} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code int} values to add
     */
    default void addInts(int[][] vectors, int batch)
        {
        Vector.KeySequence<Vector.Key> keySequence = Vector.Key.newSequence();
        addAll(Arrays.stream(vectors).map(v -> Vector.ofInts(v, keySequence.next())), batch);
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
     * @param vector    the vector of long values to add
     * @param metadata  optional metadata for the vector
     */
    default void addLongs(long[] vector, MetadataType metadata)
        {
        addLongs(Vector.Key.withId(0), vector, metadata);
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
     * @param vector  the vector of long values to add
     */
    default void addLongs(long[] vector)
        {
        addLongs(Vector.Key.withId(0), vector, null);
        }

    /**
     * Add an array of vectors of {@code long} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code long} values to add
     */
    default void addLongs(long[][] vectors)
        {
        addLongs(vectors, DEFAULT_BATCH);
        }

    /**
     * Add an array of vectors of {@code long} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vectors  the vectors of {@code long} values to add
     * @param batch    the size of the batches of vectors to store at one time
     */
    default void addLongs(long[][] vectors, int batch)
        {
        Vector.KeySequence<Vector.Key> keySequence = Vector.Key.newSequence();
        addAll(Arrays.stream(vectors).map(v -> Vector.ofLongs(v, keySequence.next())), batch);
        }

    /**
     * Add a vector of {@code short} values to this store.
     * <em>
     * Note: adding a vector of different types than the underlying vector type will cause
     * the vector being added to be up-cast or down-cast to the required type.
     * This is fine if the values in the vector being added are all withing the range of
     * the type required by this vector store. If not the types will lose fidelity as they
     * would with a normal Java down-cast.
     * </em>
     *
     * @param vector    the vector of short values to add
     * @param metadata  optional metadata for the vector
     */
    default void addShorts(short[] vector, MetadataType metadata)
        {
        addShorts(Vector.Key.withId(0), vector, metadata);
        }

    /**
     * Add a vector of {@code short} values to this store.
     *
     * @param vector  the vector of short values to add
     */
    default void addShorts(short[] vector)
        {
        addShorts(Vector.Key.withId(0), vector, null);
        }

    /**
     * Add an array of vectors of {@code short} values to this store.
     *
     * @param vectors  the vectors of {@code short} values to add
     */
    default void addShorts(short[][] vectors)
        {
        addShorts(vectors, DEFAULT_BATCH);
        }

    /**
     * Add an array of vectors of {@code short} values to this store.
     *
     * @param vectors  the vectors of {@code short} values to add
     * @param batch    the size of the batches of vectors to store at one time
     */
    default void addShorts(short[][] vectors, int batch)
        {
        Vector.KeySequence<Vector.Key> keySequence = Vector.Key.newSequence();
        addAll(Arrays.stream(vectors).map(v -> Vector.ofShorts(v, keySequence.next())), batch);
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code double} instances.
     *
     * @param name            the name of the vector store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code double} instances
     *
     */
    static <MetadataType> KeylessVectorStore<double[], MetadataType> ofDoubles(String name)
        {
        return ofDoubles(name, Session.create());
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code double} instances.
     *
     * @param name            the name of the vector store
     * @param session         the {@link Session} owning the store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code double} instances
     */
    static <MetadataType> KeylessVectorStore<double[], MetadataType> ofDoubles(String name, Session session)
        {
        return new DoubleKeylessVectorStore<>(session, name);
        }


    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code float} instances.
     *
     * @param name            the name of the vector store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code float} instances
     *
     */
    static <MetadataType> KeylessVectorStore<float[], MetadataType> ofFloats(String name)
        {
        return ofFloats(name, Session.create());
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code float} instances.
     *
     * @param name            the name of the vector store
     * @param session         the {@link Session} owning the store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code float} instances
     */
    static <MetadataType> KeylessVectorStore<float[], MetadataType> ofFloats(String name, Session session)
        {
        return new FloatKeylessVectorStore<>(session, name);
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code int} instances.
     *
     * @param name            the name of the vector store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code int} instances
     *
     */
    static <MetadataType> KeylessVectorStore<int[], MetadataType> ofInts(String name)
        {
        return ofInts(name, Session.create());
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code int} instances.
     *
     * @param name            the name of the vector store
     * @param session         the {@link Session} owning the store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code int} instances
     */
    static <MetadataType> KeylessVectorStore<int[], MetadataType> ofInts(String name, Session session)
        {
        return new IntKeylessVectorStore<>(session, name);
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code long} instances.
     *
     * @param name            the name of the vector store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code long} instances
     */
    static <MetadataType> KeylessVectorStore<long[], MetadataType> ofLongs(String name)
        {
        return ofLongs(name, Session.create());
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code long} instances.
     *
     * @param name            the name of the vector store
     * @param session         the {@link Session} owning the store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code long} instances
     */
    static <MetadataType> KeylessVectorStore<long[], MetadataType> ofLongs(String name, Session session)
        {
        return new LongKeylessVectorStore<>(session, name);
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code short} instances.
     *
     * @param name            the name of the vector store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code short} instances
     */
    static <MetadataType> KeylessVectorStore<short[], MetadataType> ofShorts(String name)
        {
        return ofShorts(name, Session.create());
        }

    /**
     * Create a {@link KeylessVectorStore} for vectors of {@code short} instances.
     *
     * @param name            the name of the vector store
     * @param session         the {@link Session} owning the store
     * @param <MetadataType>  the type of the optional metadata
     *
     * @return a {@link KeylessVectorStore} for vectors of {@code short} instances
     */
    static <MetadataType> KeylessVectorStore<short[], MetadataType> ofShorts(String name, Session session)
        {
        return new ShortKeylessVectorStore<>(session, name);
        }
    }
