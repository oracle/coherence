/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

/**
 * Queue utilities.
 */
public abstract class Utils
    {

    /**
     * Add one to the specified int value. If the specified value
     * is equal to {@link Integer#MAX_VALUE} then return zero,
     * otherwise return the specified value plus one.
     *
     * @param i the int value to increment
     *
     * @return zero if the specified int value is {@link Integer#MAX_VALUE},
     *         otherwise the specified value plus one
     */
    public static int unsignedIncrement(int i)
        {
        return i == Integer.MAX_VALUE ? 0 : i + 1;
        }

    /**
     * Subtract one from the specified int value. If the specified value
     * is equal to zero then return {@link Integer#MAX_VALUE},
     * otherwise return the specified value minus one.
     *
     * @param i the int value to increment
     *
     * @return {@link Integer#MAX_VALUE} if the specified int value is
     *         zero, otherwise the specified value minus one
     */
    public static int unsignedDecrement(int i)
        {
        return i <= 0 ? Integer.MAX_VALUE : i - 1;
        }
    }
