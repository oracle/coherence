/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.tangosol.util.Binary;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import java.util.concurrent.ThreadLocalRandom;

import static com.oracle.coherence.common.base.TimeHelper.getSafeTimeMillis;

/**
 * Class for providing random functionality.
 *
 * @author cp  2000.08.02
 * @since 20.06
 */
public abstract class Randoms
    {
    // ----- random routines ----------------------------------------------

    /**
     * Return a random number assigned to this process.
     * <p>
     * This value will remain the same across invocations, but is generally different across JVMs.
     *
     * @return the process's random number.
     */
    public static int getProcessRandom()
        {
        return s_procRand;
        }

    /**
     * Obtain a Random object that can be used to get random values.
     *
     * @return a random number generator
     * @since Coherence 3.2
     */
    public static Random getRandom()
        {
        Random rnd = s_rnd;

        if (rnd == null)
            {
            // double-check locking is not required to work; the worst that
            // can happen is that we create a couple extra Random objects
            synchronized (Random.class)
                {
                rnd = s_rnd;
                if (rnd == null)
                    {
                    rnd = new Random();

                    // spin the seed a bit
                    long lStop = getSafeTimeMillis() + 31 + rnd.nextInt(31);
                    long cMin = 1021 + rnd.nextInt(Math.max(1, (int) (lStop % 1021)));
                    while (getSafeTimeMillis() < lStop || --cMin > 0)
                        {
                        cMin += rnd.nextBoolean() ? 1 : -1;
                        rnd.setSeed(rnd.nextLong());
                        }

                    // spin the random until the clock ticks again
                    long lStart = getSafeTimeMillis();
                    do
                        {
                        if (rnd.nextBoolean())
                            {
                            if ((rnd.nextLong() & 0x01L) == (getSafeTimeMillis() & 0x01L))
                                {
                                rnd.nextBoolean();
                                }
                            }
                        }
                    while (getSafeTimeMillis() == lStart);

                    s_rnd = rnd;
                    }
                }
            }

        return rnd;
        }

    /**
     * Randomize the order of the elements within the passed collection.
     *
     * @param coll the Collection to randomize; the passed Collection is not
     *             altered
     * @return a new and immutable List whose contents are identical to those
     * of the passed collection except for the order in which they appear
     * @since Coherence 3.2
     */
    @SuppressWarnings("rawtypes")
    public static List randomize(Collection coll)
        {
        return Collections.unmodifiableList(Arrays.asList(randomize(coll.toArray())));
        }

    /**
     * Randomize the order of the elements within the passed array.
     *
     * @param a an array of objects to randomize
     * @return the array that was passed in, and with its contents unchanged
     * except for the order in which they appear
     * @since Coherence 3.2
     */
    public static Object[] randomize(Object[] a)
        {
        int c;
        if (a == null || (c = a.length) <= 1)
            {
            return a;
            }

        Random rnd = getRandom();
        for (int i1 = 0; i1 < c; ++i1)
            {
            int i2 = rnd.nextInt(c);

            // swap i1, i2
            Object o = a[i2];
            a[i2] = a[i1];
            a[i1] = o;
            }

        return a;
        }

    /**
     * Randomize the order of the elements within the passed array.
     *
     * @param a an array of <tt>int</tt> values to randomize
     * @return the array that was passed in, and with its contents unchanged
     * except for the order in which they appear
     * @since Coherence 3.2
     */
    public static int[] randomize(int[] a)
        {
        int c;
        if (a == null || (c = a.length) <= 1)
            {
            return a;
            }

        Random rnd = getRandom();
        for (int i1 = 0; i1 < c; ++i1)
            {
            int i2 = rnd.nextInt(c);

            // swap i1, i2
            int n = a[i2];
            a[i2] = a[i1];
            a[i1] = n;
            }

        return a;
        }

    /**
     * Randomize the order of the elements within the passed array.
     *
     * @param a  an array of <tt>long</tt> values to randomize
     *
     * @return the array that was passed in, and with its contents unchanged
     *         except for the order in which they appear
     *
     * @since Coherence 12.2.1.4
     */
    public static long[] randomize(long[] a)
        {
        int c;
        if (a == null || (c = a.length) <= 1)
            {
            return a;
            }

        Random rnd = getRandom();
        for (int i1 = 0; i1 < c; ++i1)
            {
            int i2 = rnd.nextInt(c);

            // swap i1, i2
            long l = a[i2];
            a[i2]  = a[i1];
            a[i1]  = l;
            }

        return a;
        }

    /**
     * Generates a random-length Binary within the length bounds provided
     * whose contents are random bytes.
     *
     * @param cbMin  the minimum number of bytes in the resulting Binary
     * @param cbMax  the maximum number of bytes in the resulting Binary
     *
     * @return a randomly generated Binary object
     *
     * @since Coherence 3.2
     */
    public static Binary getRandomBinary(int cbMin, int cbMax)
        {
        return getRandomBinary(cbMin, cbMax, (byte[]) null);
        }

    /**
     * Generates a random-length Binary including {@code abHead} at the head of
     * the Binary, in addition to random bytes within the length bounds provided.
     *
     * @param cbMin   the minimum number of bytes in the resulting Binary
     * @param cbMax   the maximum number of bytes in the resulting Binary
     * @param abHead  the head of the returned Binary
     *
     * @return a randomly generated Binary object with a length of {@code
     *         [len(abHead) + cbMin, cbMax]}
     *
     * @since Coherence 12.1.3
     */
    public static Binary getRandomBinary(int cbMin, int cbMax, byte...abHead)
        {
        assert cbMin >= 0;
        assert cbMax >= cbMin;

        Random rnd    = getRandom();
        int    cbDif  = cbMax - cbMin;
        int    cbHead = abHead == null ? 0 : abHead.length;
        int    cb     = (cbDif <= 0 ? cbMax : cbMin + rnd.nextInt(cbDif)) + cbHead;
        byte[] ab     = new byte[cb];

        rnd.nextBytes(ab);

        if (cbHead > 0)
            {
            System.arraycopy(abHead, 0, ab, 0, cbHead);
            }
        return new Binary(ab);
        }

    /**
     * Generates a random-length String within the length bounds provided.
     * If the ASCII option is indicated, the characters will be in the range
     * [32-127], otherwise the characters will be in the range
     * [0x0000-0xFFFF].
     *
     * @param cchMin the minimum length of the resulting String
     * @param cchMax the maximum length of the resulting String
     * @param fAscii true if the resulting String should contain only ASCII
     *               values
     * @return a randomly generated String object
     * @since Coherence 3.2
     */
    @SuppressWarnings("deprecation")
    public static String getRandomString(int cchMin, int cchMax, boolean fAscii)
        {
        assert cchMin >= 0;
        assert cchMax >= cchMin;

        Random rnd = getRandom();
        int cchDif = cchMax - cchMin;
        int cch = cchDif <= 0 ? cchMax : cchMin + rnd.nextInt(cchDif);

        if (fAscii)
            {
            byte[] ab = new byte[cch];
            rnd.nextBytes(ab);
            for (int of = 0; of < cch; ++of)
                {
                int b = ab[of] & 0x7F;
                if (b < 0x20)
                    {
                    b = 0x20 + rnd.nextInt(0x7F - 0x20);
                    }
                ab[of] = (byte) b;
                }
            return new String(ab, 0);
            }
        else
            {
            char[] ach = new char[cch];
            int nLimit = Character.MAX_VALUE + 1;
            for (int of = 0; of < cch; ++of)
                {
                ach[of] = (char) rnd.nextInt(nLimit);
                }

            return new String(ach);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Single random value held for the life of the process.
     */
    private static final int s_procRand = ThreadLocalRandom.current().nextInt();

    /**
     * A lazily-instantiated shared Random object.
     */
    private static Random s_rnd;
    }
