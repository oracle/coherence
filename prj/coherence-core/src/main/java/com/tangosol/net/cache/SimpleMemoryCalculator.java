/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ClassHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


/**
* A UnitCalculator implementation that weighs a cache entry based upon the
* amount of physical memory (in bytes) required to store the entry.
* <p>
* This implementation can only determine an accurate entry size if both the
* entry key and value object types are one of the following classes (or an
* array thereof or of the primitive forms thereof):
* <ul>
* <li>BigInteger</li>
* <li>BigDecimal</li>
* <li>{@link Binary}</li>
* <li>Boolean</li>
* <li>Byte</li>
* <li>Character</li>
* <li>Date</li>
* <li>Double</li>
* <li>Float</li>
* <li>Integer</li>
* <li>Long</li>
* <li>Object</li>
* <li>String</li>
* <li>Time</li>
* <li>Timestamp</li>
* </ul>
* <p>
* If either the key or value object is not one of these types, an exception will
* be thrown during the unit calculation.
*
* @author jh  2005.12.20
*/
public class SimpleMemoryCalculator
        extends Base
        implements OldCache.UnitCalculator
    {
    // ----- UnitCalculator interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateUnits(Object oKey, Object oValue)
        {
        return oKey == null
            ? sizeOf(oValue)
            : getEntrySize() + sizeOf(oKey) + sizeOf(oValue);
        }

    /**
    * {@inheritDoc}
    */
    public String getName()
        {
        return ClassHelper.getSimpleName(getClass());
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Return the size of a map entry.
    *
    * @return the entry size
    */
    protected int getEntrySize()
        {
        return SIZE_ENTRY;
        }

    /**
    * Estimate the number of bytes of memory consumed by the given object.
    * <p>
    * The calculation is based on the class of the given object, with
    * reference types assumed to be aligned on a 8-byte boundary:
    * <blockquote>
    * <table border>
    * <caption>Size of Various Types</caption>
    * <tr><th>Class</th><th>Size</th></tr>
    * <tr><td>boolean</td><td>1</td></tr>
    * <tr><td>byte</td><td>1</td></tr>
    * <tr><td>short</td><td>2</td></tr>
    * <tr><td>char</td><td>2</td></tr>
    * <tr><td>int</td><td>4</td></tr>
    * <tr><td>long</td><td>8</td></tr>
    * <tr><td>float</td><td>4</td></tr>
    * <tr><td>double</td><td>8</td></tr>
    * <tr><td>Object</td><td>{@link #SIZE_OBJECT}</td></tr>
    * <tr><td>Boolean</td><td>{@link #SIZE_OBJECT} + 1</td></tr>
    * <tr><td>Byte</td><td>{@link #SIZE_OBJECT} + 1</td></tr>
    * <tr><td>Short</td><td>{@link #SIZE_OBJECT} + 2</td></tr>
    * <tr><td>Character</td><td>{@link #SIZE_OBJECT} + 2</td></tr>
    * <tr><td>Integer</td><td>{@link #SIZE_OBJECT} + 4</td></tr>
    * <tr><td>Long</td><td>{@link #SIZE_OBJECT} + 8</td></tr>
    * <tr><td>Float</td><td>{@link #SIZE_OBJECT} + 4</td></tr>
    * <tr><td>Double</td><td>{@link #SIZE_OBJECT} + 8</td></tr>
    * <tr><td>BigInteger</td><td>{@link #SIZE_OBJECT} + 48</td></tr>
    * <tr><td>BigDecimal</td><td>{@link #SIZE_OBJECT} + 4 +
    *     {@link #SIZE_OBJECT_REF} + {@link #SIZE_BIGINTEGER}</td></tr>
    * <tr><td>Date</td><td>{@link #SIZE_OBJECT} + {@link #SIZE_OBJECT_REF}
    *     + 8</td></tr>
    * <tr><td>Time</td><td>{@link #SIZE_OBJECT} + {@link #SIZE_OBJECT_REF}
    *     + 8</td></tr>
    * <tr><td>Timestamp</td><td>{@link #SIZE_OBJECT} +
    *     {@link #SIZE_OBJECT_REF} + 12</td></tr>
    * <tr><td>Binary</td><td>{@link #SIZE_BINARY} + <i>array</i></td></tr>
    * <tr><td>String</td><td>{@link #SIZE_STRING} + <i>array</i></td></tr>
    * <tr><td><i>array</i></td><td>{@link #SIZE_OBJECT} + 4 + [element size]*length()</td></tr>
    * </table>
    * </blockquote>
    *
    * @param o  the object to measure the size of
    *
    * @return an estimate of the number of bytes required to store the given
    *         object in memory
    *
    * @throws IllegalArgumentException if the type of the object is not one of
    *         the classes listed above
    */
    public int sizeOf(Object o)
        {
        if (o == null)
            {
            throw new IllegalArgumentException("value is null");
            }

        Class   clz    = o.getClass();
        Integer IBytes = (Integer) MAP_FIXED_SIZES.get(clz);
        int     cb;
        if (IBytes != null)
            {
            cb = IBytes.intValue();
            }
        else if (clz.equals(String.class))
            {
            cb = SIZE_STRING
               + padMemorySize(SIZE_BASIC_OBJECT + 4 + 2*((String) o).length());
            }
        else if (clz.equals(Binary.class))
            {
            cb = SIZE_BINARY
               + padMemorySize(SIZE_BASIC_OBJECT + 4 + ((Binary) o).length());
            }
        else if (clz.isArray())
            {
            int   cElements  = Array.getLength(o);
            Class clzElement = clz.getComponentType();

            IBytes = (Integer) MAP_FIXED_SIZES.get(clzElement);
            cb = padMemorySize(SIZE_BASIC_OBJECT + 4 + cElements *
                    (IBytes == null ? SIZE_OBJECT_REF : IBytes.intValue()));

            // for non-primitive types, calculate the size of the referenced
            // objects
            if (IBytes == null)
                {
                try
                    {
                    for (int i = 0; i < cElements; ++i)
                        {
                        cb += sizeOf(Array.get(o, i));
                        }
                    }
                catch (ArrayIndexOutOfBoundsException e) {}
                }
            }
        else
            {
            throw new IllegalArgumentException("Unsupported type: "
                    + clz.getName());
            }

        return cb;
        }

    /**
    * Determine the minimum number of bytes required to store an instance of
    * the given fixed-sized class in memory.
    * <p>
    * The calculation is based on the following table, with reference types
    * assumed to be aligned on an 16-byte boundary:
    * <blockquote>
    * <table border>
    * <caption>Size of Various Types</caption>
    * <tr><th>Class</th><th>Size</th></tr>
    * <tr><td>boolean</td><td>1</td></tr>
    * <tr><td>byte</td><td>1</td></tr>
    * <tr><td>short</td><td>2</td></tr>
    * <tr><td>char</td><td>2</td></tr>
    * <tr><td>int</td><td>4</td></tr>
    * <tr><td>long</td><td>8</td></tr>
    * <tr><td>float</td><td>4</td></tr>
    * <tr><td>double</td><td>8</td></tr>
    * <tr><td>Object</td><td>{@link #SIZE_OBJECT}</td></tr>
    * <tr><td>Boolean</td><td>{@link #SIZE_OBJECT} + 1</td></tr>
    * <tr><td>Byte</td><td>{@link #SIZE_OBJECT} + 1</td></tr>
    * <tr><td>Short</td><td>{@link #SIZE_OBJECT} + 2</td></tr>
    * <tr><td>Character</td><td>{@link #SIZE_OBJECT} + 2</td></tr>
    * <tr><td>Integer</td><td>{@link #SIZE_OBJECT} + 4</td></tr>
    * <tr><td>Long</td><td>{@link #SIZE_OBJECT} + 8</td></tr>
    * <tr><td>Float</td><td>{@link #SIZE_OBJECT} + 4</td></tr>
    * <tr><td>Double</td><td>{@link #SIZE_OBJECT} + 8</td></tr>
    * <tr><td>BigInteger</td><td>{@link #SIZE_OBJECT} + 48</td></tr>
    * <tr><td>BigDecimal</td><td>{@link #SIZE_OBJECT} + 4 +
    *     {@link #SIZE_OBJECT_REF} + {@link #SIZE_BIGINTEGER}</td></tr>
    * <tr><td>Date</td><td>{@link #SIZE_OBJECT} + {@link #SIZE_OBJECT_REF}
    *     + 8</td></tr>
    * <tr><td>Time</td><td>{@link #SIZE_OBJECT} + {@link #SIZE_OBJECT_REF}
    *     + 8</td></tr>
    * <tr><td>Timestamp</td><td>{@link #SIZE_OBJECT} +
    *     {@link #SIZE_OBJECT_REF} + 12</td></tr>
    * </table>
    * </blockquote>
    *
    * @param clz  the target class
    *
    * @return the minimum number of bytes required to store an instance of the
    *         given class in memory
    *
    * @throws IllegalArgumentException if the type of the object is not one of
    *         the classes listed above
    */
    protected int sizeOf(Class clz)
        {
        Base.azzert(clz != null);

        Integer cb = (Integer) MAP_FIXED_SIZES.get(clz);
        if (cb == null)
            {
            throw new IllegalArgumentException("Unsupported type: "
                    + clz.getName());
            }

        return cb;
        }

    /**
    * Round the given number of bytes to the next closest integer that is
    * divisible by 8.
    * <p>
    * This method is used to pad the result of a memory calculation assuming
    * that fields are ordered to minimize padding and that allocation
    * granularity is 8 bytes.
    *
    * @param cb  the number of bytes to round
    *
    * @return the input, rounded up to the nearest multiple of 8
    */
    protected static int padMemorySize(int cb)
        {
        return (int) pad(cb, 8L);
        }

    /**
    * Calculate the approximate number of bytes required to store an instance
    * of the given class and its non-static fields in memory.
    * <p>
    * The size calculation is shallow, in that fields that are references to
    * variable-sized classes are not included in the estimate.
    *
    * @param clz  the target class
    *
    * @return the number of bytes required to store an instance of the given
    *         class, including its non-static members
    */
    protected static int calculateShallowSize(Class clz)
        {
        if (clz == null)
            {
            throw new IllegalArgumentException("class is null");
            }

        int cb;

        Integer IBytes = (Integer) MAP_PRIMITIVE_SIZES.get(clz);
        if (IBytes == null)
            {
            // calculate the size of all non-static fields, including
            // inherited
            cb = SIZE_BASIC_OBJECT;
            try
                {
                do
                    {
                    // inner class has a reference to its containing class
                    if (isInnerClass(clz))
                        {
                        cb += SIZE_OBJECT_REF;
                        }

                    Field[] aField = clz.getDeclaredFields();
                    int     cField = aField == null ? 0 : aField.length;

                    for (int i = 0; i < cField; ++i)
                        {
                        Field field = aField[i];
                        if (!Modifier.isStatic(field.getModifiers()))
                            {
                            IBytes = (Integer) MAP_PRIMITIVE_SIZES.get(field.getType());
                            cb += IBytes == null
                                    ? SIZE_OBJECT_REF
                                    : IBytes.intValue();
                            }
                        }

                    clz = clz.getSuperclass();
                    }
                while (clz != null);
                }
            catch (SecurityException e)
                {
                throw ensureRuntimeException(e,
                        "Error calculating the shallow size of: " + clz);
                }
            }
        else
            {
            cb = IBytes.intValue();
            }
        return padMemorySize(cb);
        }

    /**
    * Return true if a given class is an inner class.
    *
    * @param clz  the class to check
    *
    * @return true if the provided class is an inner class
    */
    public static boolean isInnerClass(Class clz)
        {
        return clz.getEnclosingClass() != null && (clz.getModifiers() & Modifier.STATIC) == 0;
        }

    /**
    * Return the number of bytes required for an object reference.
    *
    * @param nBits  the JVM mode; 32 or 64 bit
    *
    * @return the number of bytes required for an object reference
    */
    private static int calculateObjectRefSize(int nBits)
        {
        if (nBits == 64)
            {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            try
                {
                // check JVM args
                for (String sParam :  bean.getInputArguments())
                    {
                    if (sParam.indexOf("+UseCompressedOops") > 0)
                        {
                        return 4;
                        }

                    if (sParam.indexOf("-UseCompressedOops") > 0)
                        {
                        return 8;
                        }
                    }

                // if the total memory exceeds 32GB, HotSpot
                // does not compress oops by default
                long cb = Runtime.getRuntime().maxMemory();
                if (cb >= 32L * 1024 * 1024 * 1024)
                    {
                    return 8;
                    }
                }
            catch (Exception e) {}
            }

        return 4;
        }

    // ----- unit test ------------------------------------------------------

    /**
    * Unit test.
    * <p>
    * Usage:
    * <pre>
    * java com.tangosol.net.cache.SimpleMemoryCalculator [class name]
    * </pre>
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        if (asArg.length > 0)
            {
            String sClass = asArg[0];
            try
                {
                log(sClass + "=" + calculateShallowSize(Class.forName(sClass)));
                }
            catch (Exception e)
                {
                log("Could not load class: " + sClass);
                }
            }
        else
            {
            SimpleMemoryCalculator calc = new SimpleMemoryCalculator();

            log("SIZE_OBJECT_REF=" + SIZE_OBJECT_REF);
            log("SIZE_BASIC_OBJECT=" + SIZE_BASIC_OBJECT);
            log("SIZE_OBJECT=" + SIZE_OBJECT);
            log("SIZE_BOOLEAN=" + SIZE_BOOLEAN);
            log("SIZE_BYTE=" + SIZE_BYTE);
            log("SIZE_SHORT=" + SIZE_SHORT);
            log("SIZE_CHARACTER=" + SIZE_CHARACTER);
            log("SIZE_INTEGER=" + SIZE_INTEGER);
            log("SIZE_FLOAT=" + SIZE_FLOAT);
            log("SIZE_LONG=" + SIZE_LONG);
            log("SIZE_DOUBLE=" + SIZE_DOUBLE);
            log("SIZE_DATE=" + SIZE_DATE);
            log("SIZE_TIMESTAMP=" + SIZE_TIMESTAMP);
            log("SIZE_BIGINTEGER=" + SIZE_BIGINTEGER);
            log("SIZE_BIGDECIMAL=" + SIZE_BIGDECIMAL);
            log("SIZE_STRING=" + SIZE_STRING);
            log("SIZE_BINARY=" + SIZE_BINARY);
            log("SIZE_ENTRY=" + SIZE_ENTRY);
            log("sizeof(\"hello world\")=" + calc.sizeOf("hello world"));
            log("sizeof(0x010203040506070809)=" + calc.sizeOf(new Binary(
                    new byte[] {1,2,3,4,5,6,7,8,9})));
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The size (in bytes) of an object reference.
    * <p>
    * On a 32 bit JVM, an object reference is 4 bytes. On a 64 bit JVM, an
    * object reference is 8 bytes.
    */
    public static final int SIZE_OBJECT_REF;

    /**
    * The size (in bytes) of an {@link Object}.
    */
    public static final int SIZE_OBJECT;

    /**
    * The size (in bytes) of a {@link Boolean} object.
    */
    public static final int SIZE_BOOLEAN;

    /**
    * The size (in bytes) of a {@link Byte} object.
    */
    public static final int SIZE_BYTE;

    /**
    * The size (in bytes) of a {@link Short} object.
    */
    public static final int SIZE_SHORT;

    /**
    * The size (in bytes) of a {@link Character} object.
    */
    public static final int SIZE_CHARACTER;

    /**
    * The size (in bytes) of a {@link Integer} object.
    */
    public static final int SIZE_INTEGER;

    /**
    * The size (in bytes) of a {@link Float} object.
    */
    public static final int SIZE_FLOAT;

    /**
    * The size (in bytes) of a {@link Long} object.
    */
    public static final int SIZE_LONG;

    /**
    * The size (in bytes) of a {@link Double} object.
    */
    public static final int SIZE_DOUBLE;

    /**
    * The size (in bytes) of a {@link java.util.Date}, {@link java.sql.Date}
    * or {@link java.sql.Time} object.
    */
    public static final int SIZE_DATE;

    /**
    * The size (in bytes) of a {@link java.sql.Timestamp} object.
    */
    public static final int SIZE_TIMESTAMP;

    /**
    * The size (in bytes) of a {@link java.math.BigInteger} object.
    */
    public static final int SIZE_BIGINTEGER;

    /**
    * The size (in bytes) of a {@link java.math.BigDecimal} object.
    */
    public static final int SIZE_BIGDECIMAL;

    /**
    * The minimum size (in bytes) of a {@link String} object.
    */
    public static final int SIZE_STRING;

    /**
    * The minimum size (in bytes) of a {@link Binary} object.
    */
    public static final int SIZE_BINARY;

    /**
    * The minimum size (in bytes) of an {@link LocalCache.Entry} object.
    */
    public static final int SIZE_ENTRY;

    /**
    * An immutable map of fixed-sized classes to instance size (in bytes).
    */
    public static final Map MAP_FIXED_SIZES;

    /**
    * An immutable map of primitive types to their size (in bytes).
    */
    public static final Map MAP_PRIMITIVE_SIZES;

    /**
    * The unaligned size of the simplest object.
    */
    protected static final int SIZE_BASIC_OBJECT;

    static
        {
        // determine the word size
        // Note: the sun.arch.data.model system property returns either 32 (for
        //       a 32-bit JVM) or 64 (for a 64-bit JVM); assume 32 if the
        //       property is not available
        String sValue = System.getProperty("sun.arch.data.model");
        int    nBits  = 32;
        try
            {
            nBits = Integer.parseInt(sValue);
            }
        catch (RuntimeException e) {}

        SIZE_OBJECT_REF   = calculateObjectRefSize(nBits);
        SIZE_BASIC_OBJECT = SIZE_OBJECT_REF + nBits / 8;
        SIZE_OBJECT       = padMemorySize(SIZE_BASIC_OBJECT);
        SIZE_BOOLEAN      = padMemorySize(SIZE_BASIC_OBJECT + 1);
        SIZE_BYTE         = padMemorySize(SIZE_BASIC_OBJECT + 1);
        SIZE_SHORT        = padMemorySize(SIZE_BASIC_OBJECT + 2);
        SIZE_CHARACTER    = padMemorySize(SIZE_BASIC_OBJECT + 2);
        SIZE_INTEGER      = padMemorySize(SIZE_BASIC_OBJECT + 4);
        SIZE_FLOAT        = padMemorySize(SIZE_BASIC_OBJECT + 4);
        SIZE_LONG         = padMemorySize(SIZE_BASIC_OBJECT + 8);
        SIZE_DOUBLE       = padMemorySize(SIZE_BASIC_OBJECT + 8);
        SIZE_DATE         = padMemorySize(SIZE_BASIC_OBJECT + SIZE_OBJECT_REF + 8);
        SIZE_TIMESTAMP    = padMemorySize(SIZE_BASIC_OBJECT + SIZE_OBJECT_REF + 12);
        SIZE_BIGINTEGER   = padMemorySize(SIZE_BASIC_OBJECT + 48);
        SIZE_BIGDECIMAL   = padMemorySize(SIZE_BASIC_OBJECT + 4 + SIZE_OBJECT_REF) +
                            SIZE_BIGINTEGER;

        Map map = new HashMap();
        map.put(Boolean.TYPE,               Integer.valueOf(1));
        map.put(Byte.TYPE,                  Integer.valueOf(1));
        map.put(Short.TYPE,                 Integer.valueOf(2));
        map.put(Character.TYPE,             Integer.valueOf(2));
        map.put(Integer.TYPE,               Integer.valueOf(4));
        map.put(Float.TYPE,                 Integer.valueOf(4));
        map.put(Long.TYPE,                  Integer.valueOf(8));
        map.put(Double.TYPE,                Integer.valueOf(8));

        MAP_PRIMITIVE_SIZES = Collections.unmodifiableMap(map);

        map = new HashMap(map);

        map.put(Object.class,               Integer.valueOf(SIZE_OBJECT));
        map.put(Enum.class,                 Integer.valueOf(SIZE_OBJECT + SIZE_INTEGER));
        map.put(Boolean.class,              Integer.valueOf(SIZE_BOOLEAN));
        map.put(Byte.class,                 Integer.valueOf(SIZE_BYTE));
        map.put(Short.class,                Integer.valueOf(SIZE_SHORT));
        map.put(Character.class,            Integer.valueOf(SIZE_CHARACTER));
        map.put(Integer.class,              Integer.valueOf(SIZE_INTEGER));
        map.put(Float.class,                Integer.valueOf(SIZE_FLOAT));
        map.put(Long.class,                 Integer.valueOf(SIZE_LONG));
        map.put(Double.class,               Integer.valueOf(SIZE_DOUBLE));
        map.put(java.util.Date.class,       Integer.valueOf(SIZE_DATE));
        map.put(java.sql.Date.class,        Integer.valueOf(SIZE_DATE));
        map.put(java.sql.Time.class,        Integer.valueOf(SIZE_DATE));
        map.put(java.sql.Timestamp.class,   Integer.valueOf(SIZE_TIMESTAMP));
        map.put(java.math.BigInteger.class, Integer.valueOf(SIZE_BIGINTEGER));
        map.put(java.math.BigDecimal.class, Integer.valueOf(SIZE_BIGDECIMAL));

        MAP_FIXED_SIZES = Collections.unmodifiableMap(map);

        SIZE_STRING = calculateShallowSize(String.class);
        SIZE_BINARY = calculateShallowSize(Binary.class);
        SIZE_ENTRY  = calculateShallowSize(LocalCache.Entry.class);
        }
    }
