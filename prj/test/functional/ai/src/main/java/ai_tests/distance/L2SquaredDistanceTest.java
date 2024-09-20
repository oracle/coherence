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
import com.oracle.coherence.ai.distance.L2SquaredDistance;
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

public class L2SquaredDistanceTest
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
        L2SquaredDistance<?> distance = new L2SquaredDistance<>();
        Binary               binary   = ExternalizableHelper.toBinary(distance, serializer);
        L2SquaredDistance<?> result   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        }

    
    @Test
    public void shouldCalculateFloat32DistanceForExactMatch()
        {
        L2SquaredDistance<float[]> algorithm = new L2SquaredDistance<>();
        float[]                    floats   = Vectors.normalize(new float[]{0.2f, 0.4f, 0.1f, 0.3f});  // already normalized
        Vector<float[]>            vector   = new Float32Vector(floats);
        double                     distance = algorithm.distance(vector, vector);
        double                     expected  = Vectors.l2squared(floats, floats);
        assertThat(distance, is(closeTo(expected, 0.0000001d)));
        }


    @Test
    public void shouldCalculateFloat32Distance()
        {
        L2SquaredDistance<float[]> algorithm = new L2SquaredDistance<>();
        float[]                    floats1   = new float[]{0.2f, 0.4f, 0.1f, 0.3f}; // already normalized
        Vector<float[]>            vector1   = new Float32Vector(floats1);
        float[]                    floats2   = new float[]{0.1f, 0.5f, 0.2f, 0.2f}; // already normalized
        Vector<float[]>            vector2   = new Float32Vector(floats2);
        double                     distance  = algorithm.distance(vector1, vector2);
        double                     expected  = Vectors.l2squared(floats1, floats2);
        assertThat(distance, is(closeTo(expected, 0.0000001d)));
        }

    @Test
    public void shouldNotCalculateForFloat32VectorsWithDifferentDimensions()
        {
        L2SquaredDistance<float[]> algorithm = new L2SquaredDistance<>();
        Vector<float[]>            vector1   = new Float32Vector(new float[]{1.0f, 2.0f});
        Vector<float[]>            vector2   = new Float32Vector(new float[]{1.0f, 2.0f, 3.0f});
        assertThrows(IllegalArgumentException.class, () -> algorithm.distance(vector1, vector2));
        }

    @Test
    public void shouldCalculateBitDistanceForExactMatch()
        {
        L2SquaredDistance<BitSet> algorithm = new L2SquaredDistance<>();
        BitSet                    bits      = BitSet.valueOf(new byte[]{1, 2, 3, 4});
        Vector<BitSet>            vector    = new BitVector(bits);
        double                    distance  = algorithm.distance(vector, vector);
        double                    expected  = Vectors.l2squared(bits, bits);
        assertThat(distance, is(closeTo(expected, 0.0000001d)));
        }


    @Test
    public void shouldCalculateBitDistance()
        {
        L2SquaredDistance<BitSet> algorithm = new L2SquaredDistance<>();
        BitSet                    bits1     = BitSet.valueOf(new byte[]{1, 2, 3, 4});
        Vector<BitSet>            vector1   = new BitVector(bits1);
        BitSet                    bits2     = BitSet.valueOf(new byte[]{2, 4, 6, 7});
        Vector<BitSet>            vector2   = new BitVector(bits2);
        double                    distance  = algorithm.distance(vector1, vector2);
        double                    expected  = Vectors.l2squared(bits1, bits2);
        assertThat(distance, is(closeTo(expected, 0.0000001d)));
        }

    @Test
    public void shouldNotCalculateForBitVectorsWithDifferentDimensions()
        {
        L2SquaredDistance<BitSet> algorithm = new L2SquaredDistance<>();
        Vector<BitSet>            vector1   = new BitVector(BitSet.valueOf(new long[]{1L, 2L}));
        Vector<BitSet>            vector2   = new BitVector(BitSet.valueOf(new long[]{1L, 2L, 3L, 4L}));
        assertThrows(IllegalArgumentException.class, () -> algorithm.distance(vector1, vector2));
        }

    @Test
    public void shouldCalculateInt8DistanceForExactMatch()
        {
        L2SquaredDistance<byte[]> algorithm = new L2SquaredDistance<>();
        byte[]                    bytes     = new byte[]{1, 2, 3, 4};
        Vector<byte[]>            vector    = new Int8Vector(bytes);
        double                    distance  = algorithm.distance(vector, vector);
        double                    expected  = Vectors.l2squared(bytes, bytes);
        assertThat(distance, is(closeTo(expected, 0.0000001d)));
        }


    @Test
    public void shouldCalculateInt8Distance()
        {
        L2SquaredDistance<byte[]> algorithm = new L2SquaredDistance<>();
        byte[]                       bytes1    = new byte[]{1, 2, 3, 4};
        Vector<byte[]>               vector1   = new Int8Vector(bytes1);
        byte[]                       bytes2    = new byte[]{2, 4, 6, 7};
        Vector<byte[]>               vector2   = new Int8Vector(bytes2);
        double                       distance  = algorithm.distance(vector1, vector2);
        double                       expected  = Vectors.l2squared(bytes1, bytes2);
        assertThat(distance, is(closeTo(expected, 0.0000001d)));
        } 

    @Test
    public void shouldNotCalculateForByteVectorsWithDifferentDimensions()
        {
        L2SquaredDistance<byte[]> algorithm = new L2SquaredDistance<>();
        Vector<byte[]>            vector1   = new Int8Vector(new byte[]{1, 2, 3, 4});
        Vector<byte[]>            vector2   = new Int8Vector(new byte[]{1, 2, 3, 4, 5, 6});
        assertThrows(IllegalArgumentException.class, () -> algorithm.distance(vector1, vector2));
        }
    }
