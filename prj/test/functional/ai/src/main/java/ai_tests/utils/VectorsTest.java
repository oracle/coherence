/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.utils;

import com.oracle.coherence.ai.util.Vectors;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static com.oracle.coherence.ai.util.Vectors.hammingDistance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class VectorsTest
    {
    @Test
    public void testBinaryQuantization()
        {
        float[] array = new float[] {-1.0f, -0.25f, 0.87f, 0f, 1.65f, 2.87f, -6.76f, 3.33f};
        BitSet bits = Vectors.binaryQuant(array).get();

        assertThat(bits.cardinality(), is(4));
        assertThat(bits.toByteArray().length, is(1));
        assertThat(bits.toByteArray()[0], is((byte) 0xB4));
        }

    @Test
    public void testDistanceSame()
        {
        BitSet x = BitSet.valueOf(new long[] {0x5555555555555555L});
        BitSet y = BitSet.valueOf(new long[] {0x5555555555555555L});

        assertThat(hammingDistance(x, y), is(0));
        }

    @Test
    public void testDistanceComplement()
        {
        BitSet x = BitSet.valueOf(new long[] {0x5555555555555555L});
        BitSet y = BitSet.valueOf(new long[] {0x5555555555555555L << 1});

        assertThat(hammingDistance(x, y), is(64));
        }

    @Test
    public void testDistance()
        {
        BitSet x = BitSet.valueOf(new long[] {0x5555555555555555L});
        BitSet y = BitSet.valueOf(new long[] {0x0F0F0F0F0F0F0F0FL});

        assertThat(hammingDistance(x, y), is(32));
        }
    }
