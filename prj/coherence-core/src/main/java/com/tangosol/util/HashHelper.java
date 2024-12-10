/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.Collection;
import java.util.Iterator;


/**
* This abstract class contains helper functions for
* calculating hash code values for any group of
* java intrinsics.
*
* @author pm
* @version 1.00, 04/25/00
*/
public abstract class HashHelper
    {
    /**
    * Calculate a running hash using the boolean value.
    *
    * @param value the boolean value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(boolean value, int hash)
        {
        // This mimics Boolean.hashCode
        return swizzle(hash) ^ (value ? 1231 : 1237);
        }

    /**
    * Calculate a running hash using the byte value.
    *
    * @param value the byte value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(byte value, int hash)
        {
        // This mimics Byte.hashCode
        return swizzle(hash) ^ value;
        }

    /**
    * Calculate a running hash using the char value.
    *
    * @param value the char value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(char value, int hash)
        {
        // Character.hashCode uses Object.hashCode, so
        // we are instead using this.
        return swizzle(hash) ^ value;
        }

    /**
    * Calculate a running hash using the double value.
    *
    * @param value the double value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(double value, int hash)
        {
        // This mimics Double.hashCode
        long bits = Double.doubleToLongBits(value);
        return swizzle(hash) ^ (int)(bits ^ (bits >> 32));
        }

    /**
    * Calculate a running hash using the float value.
    *
    * @param value the float value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(float value, int hash)
        {
        // This mimics Float.hashCode
        return swizzle(hash) ^ Float.floatToIntBits(value);
        }

    /**
    * Calculate a running hash using the int value.
    *
    * @param value the int value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(int value, int hash)
        {
        // This mimics Integer.hashCode
        return swizzle(hash) ^ value;
        }

    /**
    * Calculate a running hash using the long value.
    *
    * @param value the long value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(long value, int hash)
        {
        // This mimics Long.hashCode
        return swizzle(hash) ^ (int)(value ^ (value >> 32));
        }

    /**
    * Calculate a running hash using the short value.
    *
    * @param value the short value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(short value, int hash)
        {
        // This mimics Short.hashCode
        return swizzle(hash) ^ value;
        }

    /**
    * Calculate a running hash using the Object value.
    *
    * @param value the Object value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(Object value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        if (value instanceof boolean[])
            {
            return hash ^ hash((boolean[]) value, hash);
            }
        if (value instanceof byte[])
            {
            return hash ^ hash((byte[]) value, hash);
            }
        if (value instanceof char[])
            {
            return hash ^ hash((char[]) value, hash);
            }
        if (value instanceof double[])
            {
            return hash ^ hash((double[]) value, hash);
            }
        if (value instanceof float[])
            {
            return hash ^ hash((float[]) value, hash);
            }
        if (value instanceof int[])
            {
            return hash ^ hash((int[]) value, hash);
            }
        if (value instanceof long[])
            {
            return hash ^ hash((long[]) value, hash);
            }
        if (value instanceof short[])
            {
            return hash ^ hash((short[]) value, hash);
            }
        if (value instanceof Object[])
            {
            return hash ^ hash((Object[]) value, hash);
            }
        if (value instanceof Collection)
            {
            return hash ^ hash((Collection) value, hash);
            }

        return hash ^ value.hashCode();
        }

    /**
    * Calculate a running hash using the boolean array value.
    *
    * @param value the boolean array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(boolean[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the byte array value.
    *
    * @param value the byte array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(byte[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the char array value.
    *
    * @param value the char array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(char[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the double array value.
    *
    * @param value the double array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(double[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
         return hash;
        }

    /**
    * Calculate a running hash using the float array value.
    *
    * @param value the float array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(float[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the int array value.
    *
    * @param value the int array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(int[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the long array value.
    *
    * @param value the long array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(long[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the short array value.
    *
    * @param value the short array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(short[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the Object array value.
    *
    * @param value the Object array value for use in the hash
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(Object[] value, int hash)
        {
        hash = swizzle(hash);
        if (value == null)
            {
            return hash;
            }
        for (int i = 0; i < value.length; ++i)
            {
            hash = hash(value[i], hash);
            }
        return hash;
        }

    /**
    * Calculate a running hash using the Collection value.  The hash
    * computed over the Collection's entries is order-independent.
    *
    * @param col   the Collection value for use in the hash
    * @param hash  the running hash value
    *
    * @return the resulting running hash value
    */
    public static int hash(Collection col, int hash)
        {
        hash = swizzle(hash);
        for (Iterator iter = col.iterator(); iter.hasNext(); )
            {
            hash ^= iter.next().hashCode();
            }
        return hash;
        }

    /**
    * Shift the running hash value to try and help with
    * generating unique values given the same input, but
    * in a different order.
    *
    * @param hash the running hash value
    *
    * @return the resulting running hash value
    */
    private static int swizzle(int hash)
        {
        // rotate the current hash value 4 bits to the left
        return (hash << 4) | ((hash >> 28) & 0xf);
        }

    /**
    * Shift the value into a different charset order.
    *
    * @param s  a String
    *
    * @return a String with a different charset ordering
    */
    public static String hash(String s)
        {
        if (s == null || s.length() == 0)
            {
            return s;
            }

        char[] ach = s.toCharArray();
        for (int of = 0, cch = ach.length; of < cch; ++of)
            {
            char ch = ach[of];
            if (ch >= 32 && ch <= 127)
                {
                ach[of] = (char) (32 + (127 - ch));
                }
            }
        return new String(ach);
        }
    }
