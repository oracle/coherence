/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import jakarta.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.Objects;
import java.util.Optional;

/**
 * A holder for a Java primitive vector, it's key and it's metadata.
 * <p>
 * Supplying metadata for a vector is optional.
 *
 * @param <VectorType>    the type of the vector (this will be a primitive array type)
 * @param <KeyType>       the type of the vector key
 * @param <MetadataType>  the type of the optional metadata
 */
public interface Vector<VectorType, KeyType, MetadataType>
    {
    /**
     * Return the vector value.
     * <p>
     * The vector will never be {@code null}.
     *
     * @return the vector value
     */
    VectorType getVector();

    /**
     * Return the vector key.
     * <p>
     * The vector key will never be {@code null}.
     *
     * @return the vector key
     */
    KeyType getKey();

    /**
     * Return the optional vector metadata.
     *
     * @return the optional vector metadata
     */
    Optional<MetadataType> getMetadata();

    // ----- converter methods ----------------------------------------------

    /**
     * Return the contents of this vector as a read-only {@link ByteBuffer}.
     *
     * @return the contents of this vector as a read-only {@link ByteBuffer}
     */
    ByteBuffer asBuffer();

    /**
     * Convert this {@link Vector} to a vector of {@code double} values.
     *
     * @return this {@link Vector} converted to a vector of {@code double} values
     */
    Vector<double[], KeyType, MetadataType> asDoubles();

    /**
     * Convert this {@link Vector} to a vector of {@code float} values.
     *
     * @return this {@link Vector} converted to a vector of {@code float} values
     */
    Vector<float[], KeyType, MetadataType> asFloats();

    /**
     * Convert this {@link Vector} to a vector of {@code int} values.
     *
     * @return this {@link Vector} converted to a vector of {@code int} values
     */
    Vector<int[], KeyType, MetadataType> asInts();

    /**
     * Convert this {@link Vector} to a vector of {@code long} values.
     *
     * @return this {@link Vector} converted to a vector of {@code long} values
     */
    Vector<long[], KeyType, MetadataType> asLongs();

    /**
     * Convert this {@link Vector} to a vector of {@code short} values.
     *
     * @return this {@link Vector} converted to a vector of {@code short} values
     */
    Vector<short[], KeyType, MetadataType> asShorts();

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
     * @param <KeyType>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code double} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<double[], KeyType, M> ofDoubles(double[] vector, KeyType key, M metadata)
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
     * @param <KeyType>     the type of the vector key
     *
     * @return a vector created from the {@code double} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<double[], KeyType, M> ofDoubles(double[] vector, KeyType key)
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
     * @param <KeyType>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code float} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<float[], KeyType, M> ofFloats(float[] vector, KeyType key, M metadata)
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
     * @param <KeyType>     the type of the vector key
     *
     * @return a vector created from the {@code float} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<float[], KeyType, M> ofFloats(float[] vector, KeyType key)
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
     * @param <KeyType>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code int} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<int[], KeyType, M> ofInts(int[] vector, KeyType key, M metadata)
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
     * @param <KeyType>     the type of the vector key
     *
     * @return a vector created from the {@code int} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<int[], KeyType, M> ofInts(int[] vector, KeyType key)
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
     * @param <KeyType>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code long} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<long[], KeyType, M> ofLongs(long[] vector, KeyType key, M metadata)
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
     * @param <KeyType>     the type of the vector key
     *
     * @return a vector created from the {@code long} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<long[], KeyType, M> ofLongs(long[] vector, KeyType key)
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
     * @param <KeyType>       the type of the vector key
     * @param <M>       the type of the optional metadata
     *
     * @return a vector created from the {@code short} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<short[], KeyType, M> ofShorts(short[] vector, KeyType key, M metadata)
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
     * @param <KeyType>     the type of the vector key
     *
     * @return a vector created from the {@code short} array
     *
     * @throws NullPointerException if either the {@code vector} or {@code key} are {@code null}
     */
    static <KeyType, M> Vector<short[], KeyType, M> ofShorts(short[] vector, KeyType key)
        {
        return ofShorts(vector, key, null);
        }

    // ----- inner class: BaseVector ----------------------------------------

    abstract class BaseVector<VectorType, KeyType, MetadataType>
            implements Vector<VectorType, KeyType, MetadataType>
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
        protected BaseVector(VectorType vector, KeyType key, MetadataType metadata)
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
        @Override
        public VectorType getVector()
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
        @Override
        public KeyType getKey()
            {
            return m_key;
            }

        /**
         * Return the optional vector metadata.
         *
         * @return the optional vector metadata
         */
        @Override
        public Optional<MetadataType> getMetadata()
            {
            return Optional.ofNullable(m_metadata);
            }

        // ----- data members -----------------------------------------------

        /**
         * The vector values.
         */
        @JsonbProperty("vector")
        protected final VectorType m_vector;

        /**
         * The vector key.
         */
        @JsonbProperty("key")
        protected final KeyType m_key;

        /**
         * The vector metadata.
         */
        @JsonbProperty("metadata")
        protected final MetadataType m_metadata;
        }

    // ----- inner class: DoubleVector --------------------------------------

    /**
     * A vector of {@code double} values
     *
     * @param <KeyType>       the type of the vector key
     * @param <MetadataType>  the type of the optional metadata
     */
    class DoubleVector<KeyType, MetadataType>
            extends BaseVector<double[], KeyType, MetadataType>
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
        private DoubleVector(double[] vector, KeyType key, MetadataType metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromDoubles(m_vector);
            }

        @Override
        public Vector<double[], KeyType, MetadataType> asDoubles()
            {
            return this;
            }

        @Override
        public Vector<float[], KeyType, MetadataType> asFloats()
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
        public Vector<int[], KeyType, MetadataType> asInts()
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
        public Vector<long[], KeyType, MetadataType> asLongs()
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
        public Vector<short[], KeyType, MetadataType> asShorts()
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
     * @param <KeyType>       the type of the vector key
     * @param <MetadataType>  the type of the optional metadata
     */
    class FloatVector<KeyType, MetadataType >
            extends BaseVector<float[], KeyType, MetadataType>
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
        private FloatVector(float[] vector, KeyType key, MetadataType metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromFloats(m_vector);
            }

        @Override
        public Vector<double[], KeyType, MetadataType> asDoubles()
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
        public Vector<float[], KeyType, MetadataType> asFloats()
            {
            return this;
            }

        @Override
        public Vector<int[], KeyType, MetadataType> asInts()
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
        public Vector<long[], KeyType, MetadataType> asLongs()
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
        public Vector<short[], KeyType, MetadataType> asShorts()
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
     * @param <KeyType>       the type of the vector key
     * @param <MetadataType>  the type of the optional metadata
     */
    class IntVector<KeyType, MetadataType>
            extends BaseVector<int[], KeyType, MetadataType>
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
        public IntVector(int[] vector, KeyType key, MetadataType metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromInts(m_vector);
            }

        @Override
        public Vector<double[], KeyType, MetadataType> asDoubles()
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
        public Vector<float[], KeyType, MetadataType> asFloats()
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
        public Vector<int[], KeyType, MetadataType> asInts()
            {
            return this;
            }

        @Override
        public Vector<long[], KeyType, MetadataType> asLongs()
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
        public Vector<short[], KeyType, MetadataType> asShorts()
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
     * @param <KeyType>       the type of the vector key
     * @param <MetadataType>  the type of the optional metadata
     */
    class LongVector<KeyType, MetadataType>
            extends BaseVector<long[], KeyType, MetadataType>
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
        private LongVector(long[] vector, KeyType key, MetadataType metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromLongs(m_vector);
            }

        @Override
        public Vector<double[], KeyType, MetadataType> asDoubles()
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
        public Vector<float[], KeyType, MetadataType> asFloats()
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
        public Vector<int[], KeyType, MetadataType> asInts()
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
        public Vector<long[], KeyType, MetadataType> asLongs()
            {
            return this;
            }

        @Override
        public Vector<short[], KeyType, MetadataType> asShorts()
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
     * @param <KeyType>  the type of the vector key
     * @param <MetadataType>  the type of the optional metadata
     */
    class ShortVector<KeyType, MetadataType>
            extends BaseVector<short[], KeyType, MetadataType>
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
        private ShortVector(short[] vector, KeyType key, MetadataType metadata)
            {
            super(vector, key, metadata);
            }

        @Override
        public ByteBuffer asBuffer()
            {
            return Converters.bufferFromShorts(m_vector);
            }

        @Override
        public Vector<double[], KeyType, MetadataType> asDoubles()
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
        public Vector<float[], KeyType, MetadataType> asFloats()
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
        public Vector<int[], KeyType, MetadataType> asInts()
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
        public Vector<long[], KeyType, MetadataType> asLongs()
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
        public Vector<short[], KeyType, MetadataType> asShorts()
            {
            return this;
            }
        }

    // ----- inner class: Key -----------------------------------------------

    /**
     * A unique key for a vector.
     */
    class Key
            implements ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Key()
            {
            }

        /**
         * @param uuid the unique {@link UUID} for the vector
         * @param id   a numeric identifier for the vector
         */
        public Key(UUID uuid, long id)
            {
            this.m_uuid = uuid;
            this.m_id   = id;
            }

        /**
         * Create a unique {@link Key}.
         *
         * @param id the identifier for the key
         * @return a unique {@link Key}
         */
        public static Key withId(long id)
            {
            return new Key(new UUID(), id);
            }

        /**
         * Create a new key sequence generator.
         *
         * @return a new key sequence generator
         */
        public static KeySequence<Key> newSequence()
            {
            return new SimpleKeySequence(0L);
            }

        /**
         * Create a new key sequence generator.
         *
         * @param start   the identifier to start the sequence from
         *
         * @return a new key sequence generator
         */
        public static KeySequence<Key> newSequenceFrom(long start)
            {
            return new SimpleKeySequence(start);
            }

        /**
         * Return the {@link UUID} used by this key.
         *
         * @return the {@link UUID} used by this key
         */
        public UUID uuid()
            {
            return m_uuid;
            }

        /**
         * Return the identifier for this key.
         *
         * @return the identifier for this key
         */
        public long id()
            {
            return m_id;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_uuid = ExternalizableHelper.readObject(in);
            m_id   = ExternalizableHelper.readLong(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_uuid);
            ExternalizableHelper.writeLong(out, m_id);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_uuid = in.readObject(0);
            m_id   = in.readLong(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_uuid);
            out.writeLong(1, m_id);
            }

        @Override
        public boolean equals(Object obj)
            {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Key) obj;
            return Objects.equals(this.m_uuid, that.m_uuid) &&
                    this.m_id == that.m_id;
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_uuid, m_id);
            }

        @Override
        public String toString()
            {
            return "Vector.Key(" +
                    "uuid=" + m_uuid + ", " +
                    "id=" + m_id + ')';
            }

        // ----- data members ---------------------------------------------------

        private UUID m_uuid;

        private long m_id;
        }

    // ----- inner interface: KeySequence -----------------------------------

    /**
     * A generator of vector keys.
     */
    interface KeySequence<KeyType>
        {
        /**
         * Return the next {@link Key}.
         *
         * @return the next {@link Key}
         */
        KeyType next();

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
    class SimpleKeySequence
            implements KeySequence<Key>
        {
        public SimpleKeySequence(long id)
            {
            f_uuid = new UUID();
            m_id   = id;
            }

        public Key next()
            {
            return new Key(f_uuid, m_id++);
            }

        public UUID uuid()
            {
            return f_uuid;
            }

        private final UUID f_uuid;

        private long m_id;
        }

    // ----- inner class: IntKeySequence ------------------------------------

    /**
     * A simple vector integer key sequence.
     */
    class IntKeySequence
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
    }
