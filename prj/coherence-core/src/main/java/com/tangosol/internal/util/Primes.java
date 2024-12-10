/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import java.util.Arrays;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Primes is a helper class for working with prime numbers.
 *
 * @author mf  2016.05.24
 * @since Coherence 14.1.1
 */
public class Primes
    {
    /**
     * Return a prime larger then the specified value.
     *
     * @param n the value
     *
     * @return the prime
     *
     * @throws IllegalArgumentException if n == Integer.MAX_VALUE
     */
    public static int next(int n)
        {
        if (n == Integer.MAX_VALUE)
            {
            // we cannot represent a larger prime.
            throw new IllegalArgumentException();
            }

        int i = Arrays.binarySearch(PRIMES, n);
        if (i < 0)
            {
            i = -(i + 1);
            }
        else
            {
            ++i;
            }

        return PRIMES[Math.min(PRIMES.length - 1, i)];
        }

    /**
     * Return a prime which is at least <tt>c</tt> values larger then the specified value or Integer.MAX_VALUE
     *
     * @param n the value
     * @param c the minimum difference between the returned value and <tt>n</tt>
     *
     * @return the prime
     *
     * @throws IllegalArgumentException if Integer.MAX_VALUE - n < c
     */
    public static int next(int n, int c)
        {
        if (Integer.MAX_VALUE - n < c)
            {
            // we cannot represent a larger prime.
            throw new IllegalArgumentException();
            }

        int i = Arrays.binarySearch(PRIMES, n);
        if (i < 0)
            {
            i = -(i + 1);
            if (c > 0)
                {
                --c; // Primes[i] > n
                }
            }

        return PRIMES[Math.min(PRIMES.length - 1, i + c)];
        }

    /**
     * Return a random prime which is larger the specified value.
     *
     * @param n the value
     *
     * @return the prime
     *
     * @throws IllegalArgumentException if n == Integer.MAX_VALUE
     */
    public static int random(int n)
        {
        if (n == Integer.MAX_VALUE)
            {
            // we cannot represent a larger prime.
            throw new IllegalArgumentException();
            }

        int i = Arrays.binarySearch(PRIMES, n);
        if (i < 0)
            {
            i = -(i + 1);
            }
        else
            {
            ++i;
            }
        return PRIMES[i + ThreadLocalRandom.current().nextInt(PRIMES.length - i)];
        }

    /**
     * Return the minimum prime known to this helper.
     *
     * @return the minimum prime.
     */
    public static int min()
        {
        return PRIMES[0];
        }

    /**
     * Return the largest prime known to this helper.
     *
     * @return the largest prime
     */
    public static int max()
        {
        return PRIMES[PRIMES.length - 1];
        }

    // ----- constants ------------------------------------------------------

    /**
    * An ordered list of some primes to work with.
    *
    * NOTE: changing this list across interoperable product versions can break
    * compatibility.  Minimally Topics depends upon different members computing
    * consistent results from things like Primes.next(n, nChannel).
    */
    private static final int[] PRIMES =
        {
        2,7,17,31,47,61,79,103,127,149,173,197,229,277,347,397,457,509,587,641,
        701,761,827,883,953,1019,1129,1279,1427,1543,1733,1951,2143,2371,
        2671,2927,3253,3539,3907,4211,4591,4973,5393,5743,6143,6619,6997,
        7529,8009,8423,8819,9311,9929,10069,11087,12203,13003,14051,15017,
        16007,17027,18061,19013,20063,23011,27011,30011,35023,40009,45007,
        50021,60013,70001,80021,90001,100003,120011,140009,160001,180001,
        200003,233021,266003,300007,350003,400009,450001,500009,550007,
        600011,650011,700001,800011,850009,900001,950009,1000003,1100009,
        1200007,1300021,1400017,1500007,1600033,1700021,1800017,1900009,
        2000003,2500009,3000017,3500017,4000037,4500007,5000011,6000011,
        7000003,8000009,9000011,10000019,12000017,14000029,16000057,
        18000041,20000003,25000009,30000001,35000011,40000003,45000017,
        50000017,60000011,70000027,80000023,90000049,100000007,150000001,
        200000033,300000007,400000009,500000003,600000001,700000001,
        800000011,900000011,1000000007,1100000009,1200000041,1300000003,
        1400000023,1500000001,1600000009,1700000009,1800000011,1900000043,
        2147483647 // Integer.MAX_VALUE is a prime!
        };
    }
