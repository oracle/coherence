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

import java.util.function.LongConsumer;
import javax.json.bind.annotation.JsonbProperty;

/**
 * An ordered bag of longs.
 *
 * @author mf  2014.12.10
 */
public class LongBag
        implements ExternalizableLite, PortableObject
    {
    /**
     * Construct an empty LongBag
     */
    public LongBag()
        {
        m_a = EMPTY_ARRAY;
        }

    /**
     * Construct an LongBag with the specified initial capacity
     *
     * @param nInitialCapacity  the initial capacity
     */
    public LongBag(int nInitialCapacity)
        {
        m_a = new long[nInitialCapacity];
        }


    /**
     * Add a value to the bag.
     *
     * @param l the value to add
     */
    public void add(long l)
        {
        int i = m_c++;
        ensureStorage(i + 1)[i] = l;
        }

    /**
     * Add all the values from the specified bag to this bag.
     *
     * @param that the bag of values to add
     */
    public void addAll(LongBag that)
        {
        long[] aThat = that.m_a;
        int    cThat = that.m_c;
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
    public void forEach(LongConsumer consumer)
        {
        long[] a = m_a;
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
    public long[] toArray()
        {
        long[] a     = m_a;
        int    c     = m_c;
        long[] aCopy = new long[c];
        System.arraycopy(a, 0, aCopy, 0, c);

        return aCopy;
        }


    // ----- Serialization methods ------------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_a = ExternalizableHelper.readLongArray(in);
        m_c = m_a.length;
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        long[] a = m_a;
        int    c = m_c;

        out.writeInt(c);
        for (int i = 0; i < c; ++i)
            {
            out.writeLong(a[i]);
            }
        }

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        long[] a = in.readLongArray(0);
        m_a = a;
        m_c = a.length;
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLongArray(0, toArray());
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public int hashCode()
        {
        long[] a = m_a;
        int    n = 31;
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

        if (!(oThat instanceof LongBag))
            {
            return false;
            }

        LongBag that = (LongBag) oThat;

        long[] aThat = that.m_a;
        int    cThat = that.m_c;

        long[] a = m_a;
        int    c = m_c;

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
    protected long[] ensureStorage(int c)
        {
        long[] a = m_a;
        if (a.length < c)
            {
            long[] aNew = new long[c * 2];
            System.arraycopy(a, 0, aNew, 0, a.length);
            m_a = a = aNew;
            }

        return a;
        }


    // ----- constants ------------------------------------------------------

    /**
     * Reusable empty long array.
     */
    protected static final long[] EMPTY_ARRAY = new long[0];


    // ----- data members ---------------------------------------------------

    /**
     * The storage array.
     */
    @JsonbProperty("array")
    protected long[] m_a;

    /**
     * The number of stored elements.
     */
    @JsonbProperty("size")
    protected int m_c;
    }
