/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.comparator;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.QueryMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbProperty;


/**
* Composite comparator implementation based on a collection of comparators.
* The comparators in the array assumed to be sorted according to their
* priorities; only in a case when the n-th comparator cannot determine the
* order of the passed objects:
* <pre>
*   aComparator[n].compare(o1, o2) == 0
* </pre>
* the (n+1)-th comparator will be applied to calculate the value.
*
* @author gg 2002.11.14
*/
public class ChainedComparator<T>
        extends Base
        implements Comparator<T>, QueryMapComparator<T>, EntryAwareComparator<T>,
                   Serializable, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (for ExternalizableLite and PortableObject).
    */
    public ChainedComparator()
        {
        }

    /**
    * Construct a ChainedComparator.
    *
    * @param aComparator  the comparator array
    */
    @SafeVarargs
    public ChainedComparator(Comparator<T>... aComparator)
        {
        azzert(aComparator != null);
        m_aComparator = aComparator;
        }


    // ----- Comparator interface -------------------------------------------

    /**
    * Compares its two arguments for order.  Returns a negative integer,
    * zero, or a positive integer as the first argument is less than, equal
    * to, or greater than the second.
    *
    * @param o1  the first object to be compared
    * @param o2  the second object to be compared
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         argument is less than, equal to, or greater than the second
    *
    * @throws ClassCastException if the arguments' types prevent them from
    *         being compared by this Comparator.
    */
    public int compare(T o1, T o2)
        {
        Comparator<T>[] aComparator = getComparators();
        for (int i = 0, c = aComparator.length; i < c; i++)
            {
            int nResult = aComparator[i].compare(o1, o2);
            if (nResult != 0)
                {
                return nResult;
                }
            }
        return 0;
        }


    // ----- QueryMapComparator interface -----------------------------------

    /**
    * Compare two entries based on the rules specified by {@link Comparator}.
    * <p>
    * This implementation simply passes on this invocation to the wrapped
    * Comparator objects if they too implement this interface, or invokes
    * their default compare method passing the values extracted from the
    * passed entries.
    */
    public int compareEntries(QueryMap.Entry<?, T> entry1, QueryMap.Entry<?, T> entry2)
        {
        Comparator[] aComparator = getComparators();
        for (int i = 0, c = aComparator.length; i < c; i++)
            {
            Comparator comparator = aComparator[i];
            int        nResult    = comparator instanceof QueryMapComparator
                ? ((QueryMapComparator<T>) comparator).compareEntries(entry1, entry2)
                : compare(entry1.getValue(), entry2.getValue());

            if (nResult != 0)
                {
                return nResult;
                }
            }
        return 0;
        }


    // ----- EntryAwareComparator interface ---------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * @return true iff all the underlying comparators implement the
    *         EntryAwareComparator interface and all <tt>isKeyComparator()</tt>
    *         calls return true
    */
    public boolean isKeyComparator()
        {
        Comparator<T>[] aComparator = getComparators();
        for (int i = 0, c = aComparator.length; i < c; i++)
            {
            if (!(SafeComparator.isKeyComparator(aComparator[i])))
                {
                return false;
                }
            }
        return true;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this ChainedComparator.
    *
    * @return a String description of the ChainedComparator
    */
    public String toString()
        {
        StringBuilder sb = new StringBuilder("ChainedComparator(");

        Comparator[] aComparator = m_aComparator;
        for (int i = 0, c = aComparator.length; i < c; i++)
            {
            if (i > 0)
                {
                sb.append(", ");
                }
            sb.append(aComparator[i]);
            }

        sb.append(')');

        return sb.toString();
        }

    /**
    * Determine if two ChainedComparator objects are equal.
    *
    * @param o  the other comparator
    *
    * @return true if the passed object is equal to this ChainedComparator
    */
    public boolean equals(Object o)
        {
        return o instanceof ChainedComparator &&
            equalsDeep(m_aComparator, ((ChainedComparator) o).m_aComparator);
        }

    /**
    * Return the hash code for this comparator.
    *
    * @return the hash code value for this comparator
    */
    public int hashCode()
        {
        Comparator[] aComparator = m_aComparator;
        int          nHash       = 0;
        for (int i = 0, c = aComparator.length; i < c; i++)
            {
            nHash += aComparator[i].hashCode();
            }
        return nHash;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying Comparator array.
    *
    * @return the Comparator array
    */
    public Comparator<T>[] getComparators()
        {
        return m_aComparator;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        int cComparators = ExternalizableHelper.readInt(in);

        azzert(cComparators < 16384, "Unexpected number of chained comparators");


        Comparator<T>[] aComparator  = new Comparator[cComparators];

        for (int i = 0; i < cComparators; i++)
            {
            aComparator[i] = (Comparator<T>) ExternalizableHelper.readObject(in);
            }
        m_aComparator = aComparator;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        Comparator<T>[] aComparator  = m_aComparator;
        int             cComparators = aComparator.length;

        ExternalizableHelper.writeInt(out, cComparators);
        for (int i = 0; i < cComparators; i++)
            {
            ExternalizableHelper.writeObject(out, aComparator[i]);
            }
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aComparator = in.readArray(0, Comparator[]::new);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_aComparator);
        }


    // ----- constants ------------------------------------------------------

    /**
    * Empty array of Comparators.
    */
    private static final Comparator[] EMPTY_COMPARATOR_ARRAY = new Comparator[0];


    // ----- data members ---------------------------------------------------

    /**
    * The Comparator array.
    */
    @JsonbProperty("comparators")
    protected Comparator<T>[] m_aComparator;
    }
