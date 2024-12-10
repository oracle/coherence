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

import com.tangosol.util.Base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.function.DoubleConsumer;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An ordered bag of doubles.
 *
 * @author mf  2014.12.10
 */
public class DoubleBag
        implements ExternalizableLite, PortableObject
    {
    /**
     * Construct an empty DoubleBag
     */
    public DoubleBag()
        {
        m_a = EMPTY_ARRAY;
        }

    /**
     * Construct an DoubleBag with the specified initial capacity
     *
     * @param nInitialCapacity  the initial capacity
     */
    public DoubleBag(int nInitialCapacity)
        {
        m_a = new double[nInitialCapacity];
        }

    /**
     * Add a value to the bag.
     *
     * @param dfl the value to add
     */
    public void add(double dfl)
        {
        int i = m_c++;
        ensureStorage(i + 1)[i] = dfl;
        }

    /**
     * Add all the values from the specified bag to this bag.
     *
     * @param that the bag of values to add
     */
    public void addAll(DoubleBag that)
        {
        double[] aThat = that.m_a;
        int      cThat = that.m_c;
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
    public void forEach(DoubleConsumer consumer)
        {
        double[] a = m_a;
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
    public double[] toArray()
        {
        double[] a     = m_a;
        int      c     = m_c;
        double[] aCopy = new double[c];
        System.arraycopy(a, 0, aCopy, 0, c);

        return aCopy;
        }


    // ----- Serialization methods ------------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        int c = m_c = in.readInt();

        m_a = c <= 0 ? new double[0] :
                    c < 0x7FFFFFF >> 3
                        ? readDoubleArray(in, c)
                        : readLargeDoubleArray(in, c);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        double[] a = m_a;
        int      c = m_c;

        out.writeInt(c);
        for (int i = 0; i < c; ++i)
            {
            out.writeDouble(a[i]);
            }
        }

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        double[] a = in.readDoubleArray(0);
        m_a = a;
        m_c = a.length;
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeDoubleArray(0, toArray());
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public int hashCode()
        {
        double[] a = m_a;
        int      n = 0;
        for (int i = 0, c = m_c; i < c; ++i)
            {
            long l = Double.doubleToLongBits(a[i]);
            n = n * 31 + (int)(l ^ (l >>> 32));
            }

        return n;
        }

    @Override
    public String toString()
        {
        final StringBuilder sb = new StringBuilder();

        forEach((n) ->  sb.append(sb.length() == 0 ? "" : ", ").append(n));

        return "[" + sb + "]";
        }

    @Override
    public boolean equals(Object oThat)
        {
        if (oThat == this)
            {
            return true;
            }

        if (!(oThat instanceof DoubleBag))
            {
            return false;
            }

        DoubleBag that = (DoubleBag) oThat;

        double[] aThat = that.m_a;
        int      cThat = that.m_c;

        double[] a = m_a;
        int      c = m_c;

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
    protected double[] ensureStorage(int c)
        {
        double[] a = m_a;
        if (a.length < c)
            {
            double[] aNew = new double[c * 2];
            System.arraycopy(a, 0, aNew, 0, a.length);
            m_a = a = aNew;
            }

        return a;
        }

    /**
     * Read an array of doubles for the specified length from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     * @param c   length to read
     *
     * @return an array of ints
     *
     * @throws IOException  if an I/O exception occurs
     */
    private double[] readDoubleArray(DataInput in, int c)
            throws IOException
        {
        double[] ad = new double[c];
        for (int i = 0; i < c; i++)
            {
            ad[i] = in.readDouble();
            }

        return ad;
        }

    /**
     * Read an array of doubles with larger length.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of doubles
     *
     * @throws IOException  if an I/O exception occurs
     */
    private double[] readLargeDoubleArray(DataInput in, int cLength)
            throws IOException
        {
        int      cBatchMax = 0x3FFFFFF >> 3;
        int      cBatch    = cLength / cBatchMax + 1;
        double[] aMerged   = null;
        int      cRead     = 0;
        int      cAllocate = cBatchMax;

        double[] ad;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            ad      = readDoubleArray(in, cAllocate);
            aMerged = Base.mergeDoubleArray(aMerged, ad);
            cRead   += ad.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Reusable empty double array.
     */
    protected static final double[] EMPTY_ARRAY = new double[0];


    // ----- data members ---------------------------------------------------

    /**
     * The storage array.
     */
    @JsonbProperty("array")
    protected double[] m_a;

    /**
     * The number of stored elements.
     */
    @JsonbProperty("size")
    protected int m_c;
    }
