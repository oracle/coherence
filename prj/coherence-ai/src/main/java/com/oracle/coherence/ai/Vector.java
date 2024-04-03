/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.util.UUID;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * A holder for a Java primitive vector, it's key and it's metadata.
 * <p>
 * Supplying metadata for a vector is optional.
 *
 * @param <V>  the type of the vector (this will be a primitive array type)
 * @param <K>  the type of the vector key
 * @param <M>  the type of the optional metadata
 */
public abstract class Vector<V,K,M>
    {
    /**
     * Create a {@link Vector}.
     * <p>
     * This constructor is package private as this is the only real way
     * to enforce the data types allowed for a {@link Vector}.
     * The only way to create a {@link Vector} is to use one
     * of the factory methods.
     *
     * @param vector    the vector primitive array
     * @param key       the vector's key
     * @param metadata  the vector's optional metadata
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    private Vector(V vector, K key, M metadata)
        {
        m_vector   = Objects.requireNonNull(vector);
        m_key      = Objects.requireNonNull(key);
        m_metadata = metadata;
        }

    /**
     * Return the vector value.
     * <p>
     * The vector will never be {@code null}.
     *
     * @return the vector value
     */
    public V getVector()
        {
        return m_vector;
        }

    /**
     * Return the vector key.
     * <p>
     * The vector key will never be {@code null}.
     *
     * @return the vector key
     */
    public K getKey()
        {
        return m_key;
        }

    /**
     * Return the optional vector metadata.
     *
     * @return the optional vector metadata
     */
    public Optional<M> getMetadata()
        {
        return Optional.ofNullable(m_metadata);
        }

    // ----- converter methods ----------------------------------------------

    /**
     * Return the contents of this vector as a read-only {@link ByteBuffer}.
     *
     * @return the contents of this vector as a read-only {@link ByteBuffer}
     */
    public abstract ByteBuffer asBuffer();

    /**
     * Convert this {@link Vector} to a vector of {@code double} values.
     *
     * @return this {@link Vector} converted to a vector of {@code double} values
     */
    public abstract Vector<double[], K, M> asDoubles();

    /**
     * Convert this {@link Vector} to a vector of {@code float} values.
     *
     * @return this {@link Vector} converted to a vector of {@code float} values
     */
    public abstract Vector<float[], K, M> asFloats();

    /**
     * Convert this {@link Vector} to a vector of {@code int} values.
     *
     * @return this {@link Vector} converted to a vector of {@code int} values
     */
    public abstract Vector<int[], K, M> asInts();

    /**
     * Convert this {@link Vector} to a vector of {@code long} values.
     *
     * @return this {@link Vector} converted to a vector of {@code long} values
     */
    public abstract Vector<long[], K, M> asLongs();

    /**
     * Convert this {@link Vector} to a vector of {@code short} values.
     *
     * @return this {@link Vector} converted to a vector of {@code short} values
     */
    public abstract Vector<short[], K, M> asShorts();

    // ----- factory methods ------------------------------------------------

    /**
     * Create a vector from a {@code double} array.
     * <p>
     * The vector wraps the {@code double} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector    the {@link double} array vector data
     * @param key       the vector's key
     * @param metadata  the optional vector's metadata
     * @param <K>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code double} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<double[], K, M> ofDoubles(double[] vector, K key, M metadata)
        {
        return new DoubleVector<>(vector, key, metadata);
        }

    /**
     * Create a vector from a {@code double} array.
     * <p>
     * The vector wraps the {@code double} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector  the {@link double} array vector data
     * @param key     the vector's key
     * @param <K>     the type of the vector key
     *
     * @return a vector created from the {@code double} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<double[], K, M> ofDoubles(double[] vector, K key)
        {
        return ofDoubles(vector, key, null);
        }

    /**
     * Create a vector from a {@code float} array.
     * <p>
     * The vector wraps the {@code float} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector    the {@link float} array vector data
     * @param key       the vector's key
     * @param metadata  the optional vector's metadata
     * @param <K>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code float} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<float[], K, M> ofFloats(float[] vector, K key, M metadata)
        {
        return new FloatVector<>(vector, key, metadata);
        }

    /**
     * Create a vector from a {@code float} array.
     * <p>
     * The vector wraps the {@code float} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector  the {@link float} array vector data
     * @param key     the vector's key
     * @param <K>     the type of the vector key
     *
     * @return a vector created from the {@code float} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<float[], K, M> ofFloats(float[] vector, K key)
        {
        return ofFloats(vector, key, null);
        }

    /**
     * Create a vector from a {@code int} array.
     * <p>
     * The vector wraps the {@code int} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector    the {@link int} array vector data
     * @param key       the vector's key
     * @param metadata  the optional vector's metadata
     * @param <K>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code int} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<int[], K, M> ofInts(int[] vector, K key, M metadata)
        {
        return new IntVector<>(vector, key, metadata);
        }

    /**
     * Create a vector from a {@code int} array.
     * <p>
     * The vector wraps the {@code int} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector  the {@link int} array vector data
     * @param key     the vector's key
     * @param <K>     the type of the vector key
     *
     * @return a vector created from the {@code int} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<int[], K, M> ofInts(int[] vector, K key)
        {
        return ofInts(vector, key, null);
        }

    /**
     * Create a vector from a {@code long} array.
     * <p>
     * The vector wraps the {@code long} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector    the {@link long} array vector data
     * @param key       the vector's key
     * @param metadata  the optional vector's metadata
     * @param <K>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code long} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<long[], K, M> ofLongs(long[] vector, K key, M metadata)
        {
        return new LongVector<>(vector, key, metadata);
        }

    /**
     * Create a vector from a {@code long} array.
     * <p>
     * The vector wraps the {@code long} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector  the {@link long} array vector data
     * @param key     the vector's key
     * @param <K>     the type of the vector key
     *
     * @return a vector created from the {@code long} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<long[], K, M> ofLongs(long[] vector, K key)
        {
        return ofLongs(vector, key, null);
        }

    /**
     * Create a vector from a {@code short} array.
     * <p>
     * The vector wraps the {@code short} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector    the {@link short} array vector data
     * @param key       the vector's key
     * @param metadata  the optional vector's metadata
     * @param <K>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code short} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<short[], K, M> ofShorts(short[] vector, K key, M metadata)
        {
        return new ShortVector<>(vector, key, metadata);
        }

    /**
     * Create a vector from a {@code short} array.
     * <p>
     * The vector wraps the {@code short} array, any changes to the
     * array will be reflected in the state of this vector.
     *
     * @param vector  the {@link short} array vector data
     * @param key     the vector's key
     * @param <K>     the type of the vector key
     *
     * @return a vector created from the {@code short} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    public static <K, M> Vector<short[], K, M> ofShorts(short[] vector, K key)
        {
        return ofShorts(vector, key, null);
        }

    // ----- inner class: DoubleVector --------------------------------------

    /**
     * A vector of {@code double} values
     *
     * @param <K>  the type of the vector key
     * @param <M>  the type of the optional metadata
     */
    public static class DoubleVector<K, M>
            extends Vector<double[], K, M>
        {
        /**
         * Create a {@link DoubleVector}.
         * <p>
         * This constructor is private as this is the only real way
         * to enforce the data types allowed for a {@link Vector}.
         * The only way to create a {@link Vector} is to use one
         * of the factory methods.
         *
         * @param vector    the vector values
         * @param key       the vector's key
         * @param metadata  the vector's optional metadata
         *
         * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
         */
        private DoubleVector(double[] vector, K key, M metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromDoubles(m_vector);
            }

        @Override
        public Vector<double[], K, M> asDoubles()
            {
            return this;
            }

        @Override
        public Vector<float[], K, M> asFloats()
            {
            double[] vector = m_vector;
            int      size   = vector.length;
            float[]  array  = new float[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (float) vector[i];
                }
            return new FloatVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<int[], K, M> asInts()
            {
            double[] vector = m_vector;
            int      size   = vector.length;
            int[]    array  = new int[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (int) vector[i];
                }
            return new IntVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<long[], K, M> asLongs()
            {
            double[] vector = m_vector;
            int      size   = vector.length;
            long[]   array  = new long[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (long) vector[i];
                }
            return new LongVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<short[], K, M> asShorts()
            {
            double[] vector = m_vector;
            int      size   = vector.length;
            short[]  array  = new short[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (short) vector[i];
                }
            return new ShortVector<>(array, m_key, m_metadata);
            }
        }

    // ----- inner class: FloatVector ---------------------------------------

    /**
     * A vector of {@code float} values
     *
     * @param <K>  the type of the vector key
     * @param <M>  the type of the optional metadata
     */
    public static class FloatVector<K, M>
            extends Vector<float[], K, M>
        {
        /**
         * Create a {@link FloatVector}.
         * <p>
         * This constructor is private as this is the only real way
         * to enforce the data types allowed for a {@link Vector}.
         * The only way to create a {@link Vector} is to use one
         * of the factory methods.
         *
         * @param vector    the vector values
         * @param key       the vector's key
         * @param metadata  the vector's optional metadata
         *
         * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
         */
        private FloatVector(float[] vector, K key, M metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromFloats(m_vector);
            }

        @Override
        public Vector<double[], K, M> asDoubles()
            {
            float[]  vector = m_vector;
            int      size   = vector.length;
            double[] array  = new double[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = vector[i];
                }
            return new DoubleVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<float[], K, M> asFloats()
            {
            return this;
            }

        @Override
        public Vector<int[], K, M> asInts()
            {
            float[] vector = m_vector;
            int     size   = vector.length;
            int[]   array  = new int[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (int) vector[i];
                }
            return new IntVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<long[], K, M> asLongs()
            {
            float[] vector = m_vector;
            int     size   = vector.length;
            long[]  array  = new long[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (long) vector[i];
                }
            return new LongVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<short[], K, M> asShorts()
            {
            float[] vector = m_vector;
            int     size   = vector.length;
            short[] array  = new short[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (short) vector[i];
                }
            return new ShortVector<>(array, m_key, m_metadata);
            }
        }

    // ----- inner class: IntVector -----------------------------------------

    /**
     * A vector of {@code int} values
     *
     * @param <K>  the type of the vector key
     * @param <M>  the type of the optional metadata
     */
    public static class IntVector<K, M>
            extends Vector<int[], K, M>
        {
        /**
         * Create a {@link IntVector}.
         * <p>
         * This constructor is private as this is the only real way
         * to enforce the data types allowed for a {@link Vector}.
         * The only way to create a {@link Vector} is to use one
         * of the factory methods.
         *
         * @param vector    the vector values
         * @param key       the vector's key
         * @param metadata  the vector's optional metadata
         *
         * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
         */
        public IntVector(int[] vector, K key, M metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromInts(m_vector);
            }

        @Override
        public Vector<double[], K, M> asDoubles()
            {
            int[]    vector = m_vector;
            int      size   = vector.length;
            double[] array  = new double[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (float) vector[i];
                }
            return new DoubleVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<float[], K, M> asFloats()
            {
            int[]    vector = m_vector;
            int      size   = vector.length;
            float[]  array  = new float[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (float) vector[i];
                }
            return new FloatVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<int[], K, M> asInts()
            {
            return this;
            }

        @Override
        public Vector<long[], K, M> asLongs()
            {
            int[]  vector = m_vector;
            int    size   = vector.length;
            long[] array  = new long[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = vector[i];
                }
            return new LongVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<short[], K, M> asShorts()
            {
            int[]   vector = m_vector;
            int     size   = vector.length;
            short[] array  = new short[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (short) vector[i];
                }
            return new ShortVector<>(array, m_key, m_metadata);
            }
        }

    // ----- inner class: LongVector ----------------------------------------

    /**
     * A vector of {@code long} values
     *
     * @param <K>  the type of the vector key
     * @param <M>  the type of the optional metadata
     */
    public static class LongVector<K, M>
            extends Vector<long[], K, M>
        {
        /**
         * Create a {@link LongVector}.
         * <p>
         * This constructor is private as this is the only real way
         * to enforce the data types allowed for a {@link Vector}.
         * The only way to create a {@link Vector} is to use one
         * of the factory methods.
         *
         * @param vector    the vector values
         * @param key       the vector's key
         * @param metadata  the vector's optional metadata
         *
         * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
         */
        private LongVector(long[] vector, K key, M metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromLongs(m_vector);
            }

        @Override
        public Vector<double[], K, M> asDoubles()
            {
            long[]   vector = m_vector;
            int      size   = vector.length;
            double[] array  = new double[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (float) vector[i];
                }
            return new DoubleVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<float[], K, M> asFloats()
            {
            long[]  vector = m_vector;
            int     size   = vector.length;
            float[] array  = new float[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (float) vector[i];
                }
            return new FloatVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<int[], K, M> asInts()
            {
            long[] vector = m_vector;
            int    size   = vector.length;
            int[]  array  = new int[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (int) vector[i];
                }
            return new IntVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<long[], K, M> asLongs()
            {
            return this;
            }

        @Override
        public Vector<short[], K, M> asShorts()
            {
            long[]  vector = m_vector;
            int     size   = vector.length;
            short[] array  = new short[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (short) vector[i];
                }
            return new ShortVector<>(array, m_key, m_metadata);
            }
        }

    // ----- inner class: ShortVector ---------------------------------------

    /**
     * A vector of {@code short} values
     *
     * @param <K>  the type of the vector key
     * @param <M>  the type of the optional metadata
     */
    public static class ShortVector<K, M>
            extends Vector<short[], K, M>
        {
        /**
         * Create a {@link ShortVector}.
         * <p>
         * This constructor is private as this is the only real way
         * to enforce the data types allowed for a {@link Vector}.
         * The only way to create a {@link Vector} is to use one
         * of the factory methods.
         *
         * @param vector    the vector values
         * @param key       the vector's key
         * @param metadata  the vector's optional metadata
         *
         * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
         */
        private ShortVector(short[] vector, K key, M metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromShorts(m_vector);
            }

        @Override
        public Vector<double[], K, M> asDoubles()
            {
            short[]  vector = m_vector;
            int       size   = vector.length;
            double[]  array  = new double[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (float) vector[i];
                }
            return new DoubleVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<float[], K, M> asFloats()
            {
            short[] vector = m_vector;
            int      size   = vector.length;
            float[]  array  = new float[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (float) vector[i];
                }
            return new FloatVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<int[], K, M> asInts()
            {
            short[] vector = m_vector;
            int      size   = vector.length;
            int[]    array  = new int[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (int) vector[i];
                }
            return new IntVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<long[], K, M> asLongs()
            {
            short[] vector = m_vector;
            int      size   = vector.length;
            long[]   array  = new long[size];
            for (int i = 0; i < size; i++)
                {
                array[i] = (long) vector[i];
                }
            return new LongVector<>(array, m_key, m_metadata);
            }

        @Override
        public Vector<short[], K, M> asShorts()
            {
            return this;
            }
        }

    // ----- inner class: Key -----------------------------------------------

    /**
     * A unique key for a vector.
     *
     * @param uuid  the unique {@link UUID} for the vector
     * @param id    a numeric identifier for the vector
     */
    public record Key(UUID uuid, long id)
        {
        /**
         * Create a unique {@link Key}.
         *
         * @param id  the identifier for the key
         *
         * @return a unique {@link Key}
         */
        static Key withId(long id)
            {
            return new Key(new UUID(), id);
            }

        static KeySequence<Key> newSequence()
            {
            return new SimpleKeySequence(0L);
            }

        static KeySequence<Key> newSequenceFrom(long start)
            {
            return new SimpleKeySequence(start);
            }
        }

    // ----- inner interface: KeySequence -----------------------------------

    /**
     * A generator of vector keys.
     */
    public interface KeySequence<K>
        {
        /**
         * Return the next {@link Key}.
         *
         * @return the next {@link Key}
         */
        K next();

        /**
         * Return a key sequence of integer values.
         *
         * @return a key sequence of integer values
         */
        static KeySequence<Integer> ofInts()
            {
            return ofInts(0);
            }

        /**
         * Return a key sequence of integer values.
         *
         * @param start  the value to start the sequence at
         *
         * @return a key sequence of integer values
         */
        static KeySequence<Integer> ofInts(int start)
            {
            return new IntKeySequence(start);
            }

        /**
         * Return a key sequence of integer values.
         *
         * @return a key sequence of integer values
         */
        static KeySequence<Long> ofLongs()
            {
            return ofLongs(0L);
            }

        /**
         * Return a key sequence of integer values.
         *
         * @param start  the value to start the sequence at
         *
         * @return a key sequence of integer values
         */
        static KeySequence<Long> ofLongs(long start)
            {
            return new LongKeySequence(start);
            }
        }

    // ----- inner class: SimpleKeySequence ---------------------------------

    /**
     * A simple vector key sequence.
     */
    public static class SimpleKeySequence
            implements KeySequence<Key>
        {
        public SimpleKeySequence(long id)
            {
            m_id = id;
            }

        public Key next()
            {
            return new Key(new UUID(), m_id++);
            }

        private long m_id;
        }

    // ----- inner class: IntKeySequence ------------------------------------

    /**
     * A simple vector integer key sequence.
     */
    public static class IntKeySequence
            implements KeySequence<Integer>
        {
        public IntKeySequence(int id)
            {
            m_id = id;
            }

        public Integer next()
            {
            return m_id++;
            }

        private int m_id;
        }

    // ----- inner class: IntKeySequence ------------------------------------

    /**
     * A simple vector long key sequence.
     */
    public static class LongKeySequence
            implements KeySequence<Long>
        {
        public LongKeySequence(long id)
            {
            m_id = id;
            }

        public Long next()
            {
            return m_id++;
            }

        private long m_id;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The vector values.
     */
    protected final V m_vector;

    /**
     * The vector key.
     */
    protected final K m_key;

    /**
     * The vector metadata.
     */
    protected final M m_metadata;
    }
