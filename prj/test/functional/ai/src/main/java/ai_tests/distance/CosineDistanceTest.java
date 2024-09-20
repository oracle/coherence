/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.distance;

import com.oracle.coherence.ai.BitVector;
import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.ai.Int8Vector;
import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.distance.CosineDistance;
import com.oracle.coherence.ai.util.Vectors;

import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CosineDistanceTest
    {
    @Test
    public void shouldSerializeUsingJava()
        {
        shouldSerialize(new DefaultSerializer());
        }

    @Test
    public void shouldSerializeUsingPof()
        {
        shouldSerialize(new ConfigurablePofContext());
        }

    @Test
    public void shouldSerializeUsingJson()
        {
        shouldSerialize(new JsonSerializer());
        }

    public void shouldSerialize(Serializer serializer)
        {
        CosineDistance<?> distance = new CosineDistance<>();
        Binary            binary   = ExternalizableHelper.toBinary(distance, serializer);
        CosineDistance<?> result   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        }

    @Test
    public void shouldCalculateFloat32DistanceForExactMatch()
        {
        CosineDistance<float[]> algorithm = new CosineDistance<>();
        float[]                 floats    = Vectors.normalize(new float[]{0.2f, 0.4f, 0.1f, 0.3f});  // already normalized
        Vector<float[]>         vector    = new Float32Vector(floats);
        double                  distance  = algorithm.distance(vector, vector);
        assertThat(distance, is(0.0d));
        }

    @Test
    public void shouldCalculateFloat32Distance()
        {
        CosineDistance<float[]> algorithm = new CosineDistance<>();
        float[]                 floats1   = new float[]{0.2f, 0.4f, 0.1f, 0.3f}; // already normalized
        Vector<float[]>         vector1   = new Float32Vector(floats1);
        float[]                 floats2   = new float[]{0.1f, 0.5f, 0.2f, 0.2f}; // already normalized
        Vector<float[]>         vector2   = new Float32Vector(floats2);
        double                  distance  = algorithm.distance(vector1, vector2);
        assertThat(distance, is(closeTo(0.699999988079071d, 0.0000001d)));
        }

    @Test
    public void shouldNotCalculateForFloat32VectorsWithDifferentDimensions()
        {
        CosineDistance<float[]> algorithm = new CosineDistance<>();
        Vector<float[]>         vector1   = new Float32Vector(new float[]{1.0f, 2.0f});
        Vector<float[]>         vector2   = new Float32Vector(new float[]{1.0f, 2.0f, 3.0f});
        assertThrows(IllegalArgumentException.class, () -> algorithm.distance(vector1, vector2));
        }

    @Test
    public void shouldCalculateBitVectorDistanceForExactMatch()
        {
        CosineDistance<BitSet> algorithm = new CosineDistance<>();
        BitSet                 bits      = BitSet.valueOf(new byte[]{1, 2, 3, 4});
        Vector<BitSet>         vector    = new BitVector(bits);
        double                 distance  = algorithm.distance(vector, vector);
        assertThat(distance, is(0.0d));
        }

    @Test
    public void shouldCalculateBitVectorDistance()
        {
        CosineDistance<BitSet> algorithm = new CosineDistance<>();
        BitSet                 bits1     = BitSet.valueOf(new byte[]{1, 2, 3, 4});
        Vector<BitSet>         vector1   = new BitVector(bits1);
        BitSet                 bits2     = BitSet.valueOf(new byte[]{2, 4, 6, 7});
        Vector<BitSet>         vector2   = new BitVector(bits2);
        double                 distance  = algorithm.distance(vector1, vector2);
        assertThat(distance, is(closeTo(0.6619383096694946d, 0.0000001d)));
        }

    @Test
    public void shouldNotCalculateForBitVectorsWithDifferentDimensions()
        {
        CosineDistance<BitSet> algorithm = new CosineDistance<>();
        Vector<BitSet>         vector1   = new BitVector(BitSet.valueOf(new long[]{1L, 2L}));
        Vector<BitSet>         vector2   = new BitVector(BitSet.valueOf(new long[]{1L, 2L, 3L, 4L}));
        assertThrows(IllegalArgumentException.class, () -> algorithm.distance(vector1, vector2));
        }

    @Test
    public void shouldCalculateInt8VectorDistanceForExactMatch()
        {
        CosineDistance<byte[]> algorithm = new CosineDistance<>();
        byte[]                 bytes     = new byte[]{1, 2, 3, 4};
        Vector<byte[]>         vector    = new Int8Vector(bytes);
        double                 distance  = algorithm.distance(vector, vector);
        assertThat(distance, is(0.0d));
        }

    @Test
    public void shouldCalculateInt8VectorDistance()
        {
        CosineDistance<byte[]> algorithm = new CosineDistance<>();
        byte[]                 bytes1    = new byte[]{1, 2, 3, 4};
        Vector<byte[]>         vector1   = new Int8Vector(bytes1);
        byte[]                 bytes2    = new byte[]{2, 4, 6, 7};
        Vector<byte[]>         vector2   = new Int8Vector(bytes2);
        double                 distance  = algorithm.distance(vector1, vector2);
        assertThat(distance, is(closeTo(0.0022246837615966797, 0.0000001d)));
        }

    @Test
    public void shouldNotCalculateForByteVectorsWithDifferentDimensions()
        {
        CosineDistance<byte[]> algorithm = new CosineDistance<>();
        Vector<byte[]>         vector1   = new Int8Vector(new byte[]{1, 2, 3, 4});
        Vector<byte[]>         vector2   = new Int8Vector(new byte[]{1, 2, 3, 4, 5, 6});
        assertThrows(IllegalArgumentException.class, () -> algorithm.distance(vector1, vector2));
        }
    }
