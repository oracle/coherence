/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.function.IntConsumer;
import javax.json.bind.annotation.JsonbProperty;

/**
 * An ordered bag of ints.
 *
 * @author mf  2014.12.10
 */
public class IntBag
        implements ExternalizableLite, PortableObject
    {
    /**
     * Construct an empty IntBag
     */
    public IntBag()
        {
        m_a = EMPTY_ARRAY;
        }

    /**
     * Construct an IntBag with the specified initial capacity
     *
     * @param nInitialCapacity  the initial capacity
     */
    public IntBag(int nInitialCapacity)
        {
        m_a = new int[nInitialCapacity];
        }


    /**
     * Add a value to the bag.
     *
     * @param l the value to add
     */
    public void add(int l)
        {
        int i = m_c++;
        ensureStorage(i + 1)[i] = l;
        }

    /**
     * Add all the values from the specified bag to this bag.
     *
     * @param that the bag of values to add
     */
    public void addAll(IntBag that)
        {
        int[] aThat = that.m_a;
        int   cThat = that.m_c;
        ensureStorage(m_c + cThat);
        for (int i = 0; i < cThat; ++i)
            {
            add(aThat[i]);
            }
        }

    /**
     * Apply a function across each element in the bag.
     *
     * @param consumer  the consumer
     */
    public void forEach(IntConsumer consumer)
        {
        int[] a = m_a;
        for (int i = 0, c = m_c; i < c; ++i)
            {
            consumer.accept(a[i]);
            }
        }

    /**
     * Return the number of elements in the bag.
     *
     * @return the number of elements in the bag.
     */
    public int size()
        {
        return m_c;
        }

    /**
     * Return a new array containing the bags values.
     *
     * @return an array of the bag's values
     */
    public int[] toArray()
        {
        int[] a     = m_a;
        int   c     = m_c;
        int[] aCopy = new int[c];
        System.arraycopy(a, 0, aCopy, 0, c);

        return aCopy;
        }


    // ----- Serialization methods ------------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_a = ExternalizableHelper.readIntArray(in);
        m_c = m_a.length;
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        int[] a = m_a;
        int   c = m_c;

        out.writeInt(c);
        for (int i = 0; i < c; ++i)
            {
            out.writeInt(a[i]);
            }
        }

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        int[] a = in.readIntArray(0);
        m_a = a;
        m_c = a.length;
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeIntArray(0, toArray());
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public int hashCode()
        {
        int[] a = m_a;
        int   n = 31;
        for (int i = 0, c = m_c; i < c; ++i)
            {
            n = HashHelper.hash(a[i], n);
            }

        return n;
        }

    @Override
    public String toString()
        {
        final StringBuilder sb = new StringBuilder();

        forEach((n) -> sb.append(sb.length() == 0 ? "" : ", ").append(n));

        return "[" + sb + "]";
        }

    @Override
    public boolean equals(Object oThat)
        {
        if (oThat == this)
            {
            return true;
            }

        if (!(oThat instanceof IntBag))
            {
            return false;
            }

        IntBag that = (IntBag) oThat;

        int[] aThat = that.m_a;
        int   cThat = that.m_c;

        int[] a = m_a;
        int   c = m_c;

        if (c != cThat)
            {
            return false;
            }

        for (int i = 0; i < c; ++i)
            {
            if (a[i] != aThat[i])
                {
                return false;
                }
            }

        return true;
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Ensure that the storage array has at least the specified capacity.
     *
     * @param c the required capacity
     *
     * @return the array
     */
    protected int[] ensureStorage(int c)
        {
        int[] a = m_a;
        if (a.length < c)
            {
            int[] aNew = new int[c * 2];
            System.arraycopy(a, 0, aNew, 0, a.length);
            m_a = a = aNew;
            }

        return a;
        }


    // ----- constants ------------------------------------------------------

    /**
     * Reusable empty int array.
     */
    protected static final int[] EMPTY_ARRAY = new int[0];


    // ----- data members ---------------------------------------------------

    /**
     * The storage array.
     */
    @JsonbProperty("array")
    protected int[] m_a;

    /**
     * The number of stored elements.
     */
    @JsonbProperty("size")
    protected int m_c;
    }
