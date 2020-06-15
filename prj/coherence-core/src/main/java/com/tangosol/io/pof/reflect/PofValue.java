/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.PofContext;

import com.tangosol.util.Binary;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Collection;
import java.util.Date;
import java.util.Map;


/**
* PofValue represents the POF data structure in a POF stream, or any
* sub-structure or value thereof.
*
* @see PofValueParser#parse(ReadBuffer, PofContext)
*
* @author as  2009.02.12
* @since Coherence 3.5
*/
public interface PofValue
    {
    /**
    * Obtain the POF type identifier for this value.
    *
    * @return POF type identifier for this value
    */
    public int getTypeId();

    /**
    * Return the root of the hierarchy this value belongs to.
    *
    * @return the root value
    */
    public PofValue getRoot();

    /**
    * Return the parent of this value.
    *
    * @return the parent value, or <tt>null</tt> if this is root value
    */
    public PofValue getParent();

    /**
    * Locate a child PofValue contained within this PofValue.
    * <p>
    * Note: the returned PofValue could represent a non-existent (null) value.
    *
    * @param nIndex  index of the child value
    *
    * @return the child PofValue
    *
    * @throws PofNavigationException  if this value is a "terminal" or the child
    *         value cannot be located for any other reason
    */
    public PofValue getChild(int nIndex);

    /**
    * Return the deserialized value which this PofValue represents.
    * <p>
    * Note: For primitive types such as int or boolean, the POF type
    * is not stored in the POF stream. Therefore, for primitive types, the
    * type or class must be explicitly specified via {@link #getValue(int)}
    * or {@link #getValue(Class)}.
    *
    * @return the deserialized value
    */
    public Object getValue();

    /**
    * Return the deserialized value which this PofValue represents.
    * <p>
    * Note: For primitive types such as int or boolean, the POF type
    * is not stored in the POF stream. Therefore, for primitive types, the
    * clz parameter must not be null.
    *
    * @param clz  the required class of the returned value or null if the class
    *             is to be inferred from the serialized state
    *
    * @return the deserialized value
    *
    * @throws ClassCastException if the value is incompatible with the
    *                            specified class
    */
    public Object getValue(Class clz);

    /**
    * Return the deserialized value which this PofValue represents.
    * <p>
    * Note: For primitive types such as int or boolean, the POF type
    * is not stored in the POF stream. Therefore, for primitive types, the
    * type must be explicitly specified with the nType parameter.
    *
    * @param nType  the required POF type of the returned value or
    *               {@link PofConstants#T_UNKNOWN} if the type is to be
    *               inferred from the serialized state
    *
    * @return the deserialized value
    *
    * @throws ClassCastException if the value is incompatible with the
    *                            specified type
    */
    public Object getValue(int nType);

    /**
    * Update this PofValue.
    * <p>
    * The changes made using this method will be immediately reflected in the
    * result of {@link #getValue()} method, but will not be applied to the
    * underlying POF stream until the {@link #applyChanges()} method is invoked
    * on the root PofValue.
    *
    * @param oValue  new deserialized value for this PofValue
    */
    public void setValue(Object oValue);

    /**
    * Apply all the changes that were made to this value and return a binary
    * representation of the new value.
    * <p>
    * Any format prefixes and/or decorations that were present in the original
    * buffer this value orginated from will be preserved.
    * <p>
    * Note: this method can only be called on the root PofValue.
    *
    * @return new Binary object that contains modified PofValue
    *
    * @throws UnsupportedOperationException  if called on a non-root PofValue
    */
    public Binary applyChanges();

    /**
    * Return a buffer containing changes made to this PofValue in the format
    * defined by the {@link com.tangosol.io.BinaryDeltaCompressor}.
    * <p>
    * Note: this method can only be called on the root PofValue
    *
    * @return a buffer containing changes made to this PofValue
    *
    * @throws UnsupportedOperationException  if called on a non-root PofValue
    */
    public ReadBuffer getChanges();

    /**
    * Return the <tt>boolean</tt> which this PofValue represents.
    *
    * @return the <tt>boolean</tt> value
    */
    public boolean getBoolean();

    /**
    * Return the <tt>byte</tt> which this PofValue represents.
    *
    * @return the <tt>byte</tt> value
    */
    public byte getByte();

    /**
    * Return the <tt>char</tt> which this PofValue represents.
    *
    * @return the <tt>char</tt> value
    */
    public char getChar();

    /**
    * Return the <tt>short</tt> which this PofValue represents.
    *
    * @return the <tt>short</tt> value
    */
    public short getShort();

    /**
    * Return the <tt>int</tt> which this PofValue represents.
    *
    * @return the <tt>int</tt> value
    */
    public int getInt();

    /**
    * Return the <tt>long</tt> which this PofValue represents.
    *
    * @return the <tt>long</tt> value
    */
    public long getLong();

    /**
    * Return the <tt>float</tt> which this PofValue represents.
    *
    * @return the <tt>float</tt> value
    */
    public float getFloat();

    /**
    * Return the <tt>double</tt> which this PofValue represents.
    *
    * @return the <tt>double</tt> value
    */
    public double getDouble();

    /**
    * Return the <tt>boolean[]</tt> which this PofValue represents.
    *
    * @return the <tt>boolean[]</tt> value
    */
    public boolean[] getBooleanArray();

    /**
    * Return the <tt>byte[]</tt> which this PofValue represents.
    *
    * @return the <tt>byte[]</tt> value
    */
    public byte[] getByteArray();

    /**
    * Return the <tt>char[]</tt> which this PofValue represents.
    *
    * @return the <tt>char[]</tt> value
    */
    public char[] getCharArray();

    /**
    * Return the <tt>short[]</tt> which this PofValue represents.
    *
    * @return the <tt>short[]</tt> value
    */
    public short[] getShortArray();

    /**
    * Return the <tt>int[]</tt> which this PofValue represents.
    *
    * @return the <tt>int[]</tt> value
    */
    public int[] getIntArray();

    /**
    * Return the <tt>long[]</tt> which this PofValue represents.
    *
    * @return the <tt>long[]</tt> value
    */
    public long[] getLongArray();

    /**
    * Return the <tt>float[]</tt> which this PofValue represents.
    *
    * @return the <tt>float[]</tt> value
    */
    public float[] getFloatArray();

    /**
    * Return the <tt>double[]</tt> which this PofValue represents.
    *
    * @return the <tt>double[]</tt> value
    */
    public double[] getDoubleArray();

    /**
    * Return the <tt>BigInteger</tt> which this PofValue represents.
    *
    * @return the <tt>BigInteger</tt> value
    */
    public BigInteger getBigInteger();

    /**
    * Return the <tt>BigDecimal</tt> which this PofValue represents.
    *
    * @return the <tt>BigDecimal</tt> value
    */
    public BigDecimal getBigDecimal();

    /**
    * Return the <tt>String</tt> which this PofValue represents.
    *
    * @return the <tt>String</tt> value
    */
    public String getString();

    /**
    * Return the <tt>Date</tt> which this PofValue represents.
    *
    * @return the <tt>Date</tt> value
    */
    public Date getDate();

    /**
    * Return the <tt>Object[]</tt> which this PofValue represents.
    *
    * @return the <tt>Object[]</tt> value
    */
    public Object[] getObjectArray();

    /**
    * Return the <tt>Collection</tt> which this PofValue represents.
    *
    * @param coll  the optional Collection to use to store the values
    *
    * @return the <tt>Collection</tt> value
    */
    public Collection getCollection(Collection coll);

    /**
    * Return the <tt>Map</tt> which this PofValue represents.
    *
    * @param map  the optional Map to use to store the values
    *
    * @return the <tt>Map</tt> value
    */
    public Map getMap(Map map);
    }
