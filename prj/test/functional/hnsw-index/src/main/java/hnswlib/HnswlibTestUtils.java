/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package hnswlib;

import java.util.Random;

public final class HnswlibTestUtils
    {
    public static float[] getRandomFloatArray(int dimension)
        {
        float[] array  = new float[dimension];
        Random  random = new Random();
        for (int i = 0; i < dimension; i++)
            {
            array[i] = random.nextFloat();
            }
        return array;
        }
    }
