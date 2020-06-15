/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.util.Arrays;

/**
 * Class for providing comparison functionality.
 *
 * @author cp  2000.08.02
 * @since 20.06
 */
public abstract class Objects
    {
    // ----- comparison support ----------------------------------------------

    /**
     * Compare two references for equality.
     *
     * @param o1  the first object reference
     * @param o2  the second object reference
     *
     * @return true if equal, false otherwise
     */
    public static boolean equals(Object o1, Object o2)
        {
        if (o1 == o2)
            {
            return true;
            }

        if (o1 == null || o2 == null)
            {
            return false;
            }

        try
            {
            return o1.equals(o2);
            }
        catch (RuntimeException e)
            {
            return false;
            }
        }

    /**
     * Deeply compare two references for equality. This dives down into
     * arrays, including nested arrays.
     *
     * @param o1  the first object reference
     * @param o2  the second object reference
     *
     * @return true if deeply equal, false otherwise
     */
    public static boolean equalsDeep(Object o1, Object o2)
        {
        if (o1 == o2)
            {
            return true;
            }

        if (o1 == null || o2 == null)
            {
            return false;
            }

        if (o1.getClass().isArray())
            {
            // the following are somewhat in order of likelihood

            if (o1 instanceof byte[])
                {
                return o2 instanceof byte[]
                               && Arrays.equals((byte[]) o1, (byte[]) o2);
                }

            if (o1 instanceof Object[])
                {
                if (o2 instanceof Object[])
                    {
                    Object[] ao1 = (Object[]) o1;
                    Object[] ao2 = (Object[]) o2;
                    int c = ao1.length;
                    if (c == ao2.length)
                        {
                        for (int i = 0; i < c; ++i)
                            {
                            if (!equalsDeep(ao1[i], ao2[i]))
                                {
                                return false;
                                }
                            }
                        return true;
                        }
                    }

                return false;
                }

            if (o1 instanceof int[])
                {
                return o2 instanceof int[]
                               && Arrays.equals((int[]) o1, (int[]) o2);
                }

            if (o1 instanceof char[])
                {
                return o2 instanceof char[]
                               && Arrays.equals((char[]) o1, (char[]) o2);
                }

            if (o1 instanceof long[])
                {
                return o2 instanceof long[]
                               && Arrays.equals((long[]) o1, (long[]) o2);
                }

            if (o1 instanceof double[])
                {
                return o2 instanceof double[]
                               && Arrays.equals((double[]) o1, (double[]) o2);
                }

            if (o1 instanceof boolean[])
                {
                return o2 instanceof boolean[]
                               && Arrays.equals((boolean[]) o1, (boolean[]) o2);
                }

            if (o1 instanceof short[])
                {
                return o2 instanceof short[]
                               && Arrays.equals((short[]) o1, (short[]) o2);
                }

            if (o1 instanceof float[])
                {
                return o2 instanceof float[]
                               && Arrays.equals((float[]) o1, (float[]) o2);
                }
            }

        try
            {
            return o1.equals(o2);
            }
        catch (RuntimeException e)
            {
            return false;
            }
        }


    }
