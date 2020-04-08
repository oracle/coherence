/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

import java.util.concurrent.ThreadLocalRandom;

import java.util.Random;


/**
 * Arrays is a series of Array based helper methods.
 *
 * @author mf  2014.01.28
 */
public final class Arrays
    {
    /**
     * Shuffle a portion of the specified array's content.
     *
     * @param ao  the array to shuffle
     * @param of  the start index
     * @param c   the number of elements to shuffle
     */
    public static void shuffle(Object[] ao, int of, int c)
        {
        // from Collections.shuffle
        Random rand = ThreadLocalRandom.current();
        for (int i = c; i > 1; i--)
            {
            int    r = rand.nextInt(i);
            Object o = ao[of + i - 1];

            ao[of + i - 1] = ao[of + r];
            ao[of + r]     = o;
            }
        }

    /**
     * Shuffle the specified array's content.
     *
     * @param ao  the array to shuffle
     */
    public static void shuffle(Object[] ao)
        {
        shuffle(ao, 0, ao.length);
        }

    /**
     * Perform an insertion of the specified value into the sorted array.
     *
     * If the value is already present in the array then no insertion will be performed and the
     * same array will be returned.  If the value is not present then a new array will be created
     * and will contain the specified value in the appropriate location.
     *
     * @param an  the array to insert into, may be null
     * @param n   the value to insert
     *
     * @return the resulting array.
     */
    public static int[] binaryInsert(int[] an, int n)
        {
        if (an == null)
            {
            return new int[]{n};
            }

        int of = java.util.Arrays.binarySearch(an, n);
        if (of < 0)
            {
            // insertion required
            of = -of; // correct offset to represent the insertion point, or rather point just after it

            int[] anNew = new int[an.length + 1];
            System.arraycopy(an, 0, anNew, 0, of - 1);
            anNew[of - 1] = n;
            System.arraycopy(an, of - 1, anNew, of, an.length - of + 1);

            return anNew;
            }
        // else; already present

        return an;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Random number generator.
     */
    private static final Random s_rand = new Random();
    }
