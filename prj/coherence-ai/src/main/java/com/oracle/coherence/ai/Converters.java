/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import static com.tangosol.util.ExternalizableHelper.DECO_VECTOR;


/**
 * Utility methods to convert from various binary representations of
 * vectors into concrete primitive arrays.
 */
public interface Converters
    {
    /**
     * Return a {@code double[]} from the data in a {@link ReadBuffer}.
     *
     * @param buffer  the {@link ReadBuffer} containing the {@code double} array
     *
     * @return  a {@code double[]} from the data in a {@link ReadBuffer}
     */
    static double[] doublesFromReadBuffer(ReadBuffer buffer)
        {
        return doublesFromByteBuffer(buffer.toByteBuffer());
        }

    /**
     * Return a {@code double[]} from the data in a {@link ByteBuffer}.
     *
     * @param buffer  the {@link ByteBuffer} containing the {@code double} array
     *
     * @return  a {@code double[]} from the data in a {@link ByteBuffer}
     */
    static double[] doublesFromByteBuffer(ByteBuffer buffer)
        {
        return doublesFromDoubleBuffer(buffer.asDoubleBuffer());
        }

    /**
     * Return a {@code double[]} from the data in a {@link DoubleBuffer}.
     *
     * @param buffer  the {@link DoubleBuffer} containing the {@code double} array
     *
     * @return  a {@code double[]} from the data in a {@link DoubleBuffer}
     */
    static double[] doublesFromDoubleBuffer(DoubleBuffer buffer)
        {
        if (buffer.hasArray())
            {
            return buffer.array();
            }
        double[] array = new double[buffer.limit()];
        for (int i = 0; i < array.length; i++)
            {
            array[i] = buffer.get();
            }
        return array;
        }

    /**
     * Return a {@code float[]} from the data in a {@link ReadBuffer}.
     *
     * @param buffer  the {@link ReadBuffer} containing the {@code float} array
     *
     * @return  a {@code float[]} from the data in a {@link ReadBuffer}
     */
    static float[] floatsFromReadBuffer(ReadBuffer buffer)
        {
        return floatsFromByteBuffer(buffer.toByteBuffer());
        }

    /**
     * Return a {@code float[]} from the data in a {@link ByteBuffer}.
     *
     * @param buffer  the {@link ByteBuffer} containing the {@code float} array
     *
     * @return  a {@code float[]} from the data in a {@link ByteBuffer}
     */
    static float[] floatsFromByteBuffer(ByteBuffer buffer)
        {
        return floatsFromFloatBuffer(buffer.asFloatBuffer());
        }

    /**
     * Return a {@code float[]} from the data in a {@link FloatBuffer}.
     *
     * @param buffer  the {@link FloatBuffer} containing the {@code float} array
     *
     * @return  a {@code float[]} from the data in a {@link FloatBuffer}
     */
    static float[] floatsFromFloatBuffer(FloatBuffer buffer)
        {
        if (buffer.hasArray())
            {
            return buffer.array();
            }
        float[] array = new float[buffer.limit()];
        for (int i = 0; i < array.length; i++)
            {
            array[i] = buffer.get();
            }
        return array;
        }

    /**
     * Return a {@code int[]} from the data in a {@link ReadBuffer}.
     *
     * @param buffer  the {@link ReadBuffer} containing the {@code int} array
     *
     * @return  a {@code int[]} from the data in a {@link ReadBuffer}
     */
    static int[] intsFromReadBuffer(ReadBuffer buffer)
        {
        return intsFromByteBuffer(buffer.toByteBuffer());
        }

    /**
     * Return a {@code int[]} from the data in a {@link ByteBuffer}.
     *
     * @param buffer  the {@link ByteBuffer} containing the {@code int} array
     *
     * @return  a {@code int[]} from the data in a {@link ByteBuffer}
     */
    static int[] intsFromByteBuffer(ByteBuffer buffer)
        {
        return intsFromIntBuffer(buffer.asIntBuffer());
        }

    /**
     * Return a {@code int[]} from the data in a {@link IntBuffer}.
     *
     * @param buffer  the {@link IntBuffer} containing the {@code int} array
     *
     * @return  a {@code int[]} from the data in a {@link IntBuffer}
     */
    static int[] intsFromIntBuffer(IntBuffer buffer)
        {
        if (buffer.hasArray())
            {
            return buffer.array();
            }
        int[] array = new int[buffer.limit()];
        for (int i = 0; i < array.length; i++)
            {
            array[i] = buffer.get();
            }
        return array;
        }

    /**
     * Return a {@code long[]} from the data in a {@link ReadBuffer}.
     *
     * @param buffer  the {@link ReadBuffer} containing the {@code long} array
     *
     * @return  a {@code long[]} from the data in a {@link ReadBuffer}
     */
    static long[] longsFromReadBuffer(ReadBuffer buffer)
        {
        return longsFromByteBuffer(buffer.toByteBuffer());
        }

    /**
     * Return a {@code long[]} from the data in a {@link ByteBuffer}.
     *
     * @param buffer  the {@link ByteBuffer} containing the {@code long} array
     *
     * @return  a {@code long[]} from the data in a {@link ByteBuffer}
     */
    static long[] longsFromByteBuffer(ByteBuffer buffer)
        {
        return longsFromLongBuffer(buffer.asLongBuffer());
        }

    /**
     * Return a {@code long[]} from the data in a {@link LongBuffer}.
     *
     * @param buffer  the {@link LongBuffer} containing the {@code long} array
     *
     * @return  a {@code long[]} from the data in a {@link LongBuffer}
     */
    static long[] longsFromLongBuffer(LongBuffer buffer)
        {
        if (buffer.hasArray())
            {
            return buffer.array();
            }
        long[] array = new long[buffer.limit()];
        for (int i = 0; i < array.length; i++)
            {
            array[i] = buffer.get();
            }
        return array;
        }

    /**
     * Return a {@code short[]} from the data in a {@link ReadBuffer}.
     *
     * @param buffer  the {@link ReadBuffer} containing the {@code short} array
     *
     * @return  a {@code short[]} from the data in a {@link ReadBuffer}
     */
    static short[] shortsFromReadBuffer(ReadBuffer buffer)
        {
        return shortsFromByteBuffer(buffer.toByteBuffer());
        }

    /**
     * Return a {@code short[]} from the data in a {@link ByteBuffer}.
     *
     * @param buffer  the {@link ByteBuffer} containing the {@code short} array
     *
     * @return  a {@code short[]} from the data in a {@link ByteBuffer}
     */
    static short[] shortsFromByteBuffer(ByteBuffer buffer)
        {
        return shortsFromShortBuffer(buffer.asShortBuffer());
        }

    /**
     * Return a {@code short[]} from the data in a {@link ShortBuffer}.
     *
     * @param buffer  the {@link ShortBuffer} containing the {@code short} array
     *
     * @return  a {@code short[]} from the data in a {@link ShortBuffer}
     */
    static short[] shortsFromShortBuffer(ShortBuffer buffer)
        {
        if (buffer.hasArray())
            {
            return buffer.array();
            }
        short[] array = new short[buffer.limit()];
        for (int i = 0; i < array.length; i++)
            {
            array[i] = buffer.get();
            }
        return array;
        }

    /**
     * Create a {@link ReadBuffer} that contains the specified
     * vector of {@code double} values.
     *
     * @param vector  the vector of {@code double} values
     *
     * @return a {@link ReadBuffer} that contains the specified
     *         vector of {@code double} values
     */
    static ReadBuffer readBufferFromDoubles(double... vector)
        {
        return new ByteBufferReadBuffer(bufferFromDoubles(vector));
        }

    /**
     * Create a {@link ByteBuffer} that contains the specified
     * vector of {@code double} values.
     *
     * @param vector  the vector of {@code double} values
     *
     * @return a {@link ByteBuffer} that contains the specified
     *         vector of {@code double} values
     */
    static ByteBuffer bufferFromDoubles(double... vector)
        {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Double.BYTES);
        buffer.asDoubleBuffer().put(vector);
        return buffer;
        }

    /**
     * Create a {@link ReadBuffer} that contains the specified
     * vector of {@code float} values.
     *
     * @param vector  the vector of {@code float} values
     *
     * @return a {@link ReadBuffer} that contains the specified
     *         vector of {@code float} values
     */
    static ReadBuffer readBufferFromFloats(float... vector)
        {
        return new ByteBufferReadBuffer(bufferFromFloats(vector));
        }

    /**
     * Create a {@link ByteBuffer} that contains the specified
     * vector of {@code float} values.
     *
     * @param vector  the vector of {@code float} values
     *
     * @return a {@link ByteBuffer} that contains the specified
     *         vector of {@code float} values
     */
    static ByteBuffer bufferFromFloats(float... vector)
        {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES);
        buffer.asFloatBuffer().put(vector);
        return buffer;
        }

    /**
     * Create a {@link ReadBuffer} that contains the specified
     * vector of {@code int} values.
     *
     * @param vector  the vector of {@code int} values
     *
     * @return a {@link ReadBuffer} that contains the specified
     *         vector of {@code int} values
     */
    static ReadBuffer readBufferFromInts(int... vector)
        {
        return new ByteBufferReadBuffer(bufferFromInts(vector));
        }

    /**
     * Create a {@link ByteBuffer} that contains the specified
     * vector of {@code int} values.
     *
     * @param vector  the vector of {@code int} values
     *
     * @return a {@link ByteBuffer} that contains the specified
     *         vector of {@code int} values
     */
    static ByteBuffer bufferFromInts(int... vector)
        {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Integer.BYTES);
        buffer.asIntBuffer().put(vector);
        return buffer;
        }

    /**
     * Create a {@link ReadBuffer} that contains the specified
     * vector of {@code long} values.
     *
     * @param vector  the vector of {@code long} values
     *
     * @return a {@link ReadBuffer} that contains the specified
     *         vector of {@code long} values
     */
    static ReadBuffer readBufferFromLongs(long... vector)
        {
        return new ByteBufferReadBuffer(bufferFromLongs(vector));
        }

    /**
     * Create a {@link ByteBuffer} that contains the specified
     * vector of {@code long} values.
     *
     * @param vector  the vector of {@code long} values
     *
     * @return a {@link ByteBuffer} that contains the specified
     *         vector of {@code long} values
     */
    static ByteBuffer bufferFromLongs(long... vector)
        {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Long.BYTES);
        buffer.asLongBuffer().put(vector);
        return buffer;
        }

    /**
     * Create a {@link ReadBuffer} that contains the specified
     * vector of {@code short} values.
     *
     * @param vector  the vector of {@code short} values
     *
     * @return a {@link ReadBuffer} that contains the specified
     *         vector of {@code short} values
     */
    static ReadBuffer readBufferFromShorts(short... vector)
        {
        return new ByteBufferReadBuffer(bufferFromShorts(vector));
        }

    /**
     * Create a {@link ByteBuffer} that contains the specified
     * vector of {@code short} values.
     *
     * @param vector  the vector of {@code short} values
     *
     * @return a {@link ByteBuffer} that contains the specified
     *         vector of {@code short} values
     */
    static ByteBuffer bufferFromShorts(short... vector)
        {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Short.BYTES);
        buffer.asShortBuffer().put(vector);
        return buffer;
        }

    /**
     * Decorate a binary value with its binary metadata.
     *
     * @param binVector    a {@link ReadBuffer} containing the vector data
     * @param binMetadata  a {@link ReadBuffer} containing the metadata
     *
     * @return a decorated binary containing both the vector and metadata
     */
    static ReadBuffer combineMetadata(ReadBuffer binVector, ReadBuffer binMetadata)
        {
        if (binMetadata == null)
            {
            binMetadata = Binary.NO_BINARY;
            }
        return ExternalizableHelper.decorate(binMetadata, DECO_VECTOR, binVector);
        }

    /**
     * Extract the metadata from a decorated binary vector.
     *
     * @param binaryValue  the decorated binary vector data
     *
     * @return  the binary metadata or {@code null} if there was no metadata
     */
    static Binary extractVector(Binary binaryValue)
        {
        ReadBuffer buffer = ExternalizableHelper.getDecoration((ReadBuffer) binaryValue, DECO_VECTOR);
        return buffer == null ? null : buffer.toBinary();
        }

    static ReadBuffer extractMetadata(Binary binary)
        {
        return ExternalizableHelper.getUndecorated((ReadBuffer) binary);
        }
    }
