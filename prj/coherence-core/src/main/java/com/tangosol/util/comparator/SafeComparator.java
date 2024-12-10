/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.comparator;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.QueryMap;

import com.tangosol.util.extractor.KeyExtractor;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbProperty;


/**
* Null-safe delegating comparator. Null values are evaluated as "less then"
* any non-null value. If the wrapped comparator is not specified then all
* non-null values must implement the {@link java.lang.Comparable} interface.
* <p>
* Use SafeComparator.INSTANCE to obtain an instance of non-delegating
* SafeComparator.
*
* @author gg 2002.12.10
*/
public class SafeComparator<T>
        extends Base
        implements Remote.Comparator<T>, QueryMapComparator<T>, EntryAwareComparator<T>,
                   Serializable, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (for ExternalizableLite and PortableObject).
    */
    public SafeComparator()
        {
        }

    /**
    * Construct a SafeComparator delegating to the specified (wrapped)
    * comparator.
    *
    * @param comparator  Comparator object to delegate comparison of
    *                    non-null values (optional)
    */
    public SafeComparator(Comparator<? super T> comparator)
        {
        this(comparator, true);
        }

    /**
    * Construct a SafeComparator delegating to the specified (wrapped)
    * comparator.
    *
    * @param comparator  Comparator object to delegate comparison of
    *                    non-null values (optional)
    * @param fNullFirst  flag specifying if null values should be treated as
    *                   "less than" (true) or "greater than" (false) any
    *                    non-null value
    */
    public SafeComparator(Comparator<? super T> comparator, boolean fNullFirst)
        {
        m_comparator = comparator;
        m_fNullFirst = fNullFirst;
        }

    /**
     * Ensure that the specified comparator is safe, by wrapping it if it isn't.
     *
     * @param comparator  the comparator to wrap, if necessary
     *
     * @return a {@code SafeComparator} for the specified comparator
     *
     * @param <V>  the type of objects to compare
     */
    public static <V> Comparator<? super V> ensureSafe(Comparator<? super V> comparator)
        {
        return comparator == null
               ? SafeComparator.INSTANCE()
               : comparator instanceof SafeComparator
                 ? comparator
                 : new SafeComparator<>(comparator);
        }

    // ----- Comparator interface -------------------------------------------

    /**
    * Compares its two arguments for order.  Returns a negative integer,
    * zero, or a positive integer as the first argument is less than, equal
    * to, or greater than the second. Null values are evaluated as "less
    * then" any non-null value. If the wrapped comparator is not specified,
    * all non-null values must implement the {@link java.lang.Comparable}
    * interface.
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
        return compareSafe(getComparator(), o1, o2, m_fNullFirst);
        }


    // ----- QueryMapComparator interface -----------------------------------

    /**
    * Compare two entries using the underlying comparator. If the wrapped
    * comparator does not implement the QueryMapComparator interface, revert
    * to the entry values comparison.
    */
    public int compareEntries(QueryMap.Entry entry1, QueryMap.Entry entry2)
        {
        Comparator comparator = getComparator();
        return comparator instanceof QueryMapComparator
            ? ((QueryMapComparator) comparator).compareEntries(entry1, entry2)
            : compareSafe(comparator, entry1.getValue(), entry2.getValue());
        }


    // ----- EntryAwareComparator interface ---------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean isKeyComparator()
        {
        return isKeyComparator(getComparator());
        }

    /**
    * Check whether the specified comparator expects to compare keys or values.
    *
    * @param comparator a Comparator to check
    *
    * @return true if the comparator expects keys; false otherwise
    */
    public static boolean isKeyComparator(Comparator comparator)
        {
        return comparator instanceof KeyExtractor ||
               comparator instanceof EntryAwareComparator &&
                ((EntryAwareComparator) comparator).isKeyComparator();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the wrapped Comparator.
    *
    * @return the wrapped Comparator
    */
    public Comparator<? super T> getComparator()
        {
        return m_comparator;
        }

    /**
     * Obtain the m_fNullFirst flag.
     *
     * @return the m_fNullFirst flag
     */
    public boolean isNullFirst()
        {
        return m_fNullFirst;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Compares its two arguments for order.  Returns a negative integer,
    * zero, or a positive integer as the first argument is less than, equal
    * to, or greater than the second. Null values are evaluated as
    * "less then" any non-null value. Non-null values must implement the
    * {@link java.lang.Comparable} interface.
    *
    * @param comparator  a comparator to use for the comparison (optional)
    * @param o1          the first object to be compared
    * @param o2          the second object to be compared
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         argument is less than, equal to, or greater than the second
    *
    * @throws ClassCastException if the arguments are not Comparable
    */
    public static <T> int compareSafe(Comparator comparator, Object o1, Object o2)
        {
        return compareSafe(comparator, o1, o2, true);
        }

    /**
    * Compares its two arguments for order.  Returns a negative integer,
    * zero, or a positive integer as the first argument is less than, equal
    * to, or greater than the second. Null values are evaluated based on the
    * value of a {@code fNullFirst} flag as either "less than" or "greater than"
    * any non-null value. Non-null values must implement the
    * {@link java.lang.Comparable} interface.
    *
    * @param comparator  a comparator to use for the comparison (optional)
    * @param o1          the first object to be compared
    * @param o2          the second object to be compared
    * @param fNullFirst  flag specifying if null values should be treated as
    *                   "less than" (true) or "greater than" (false) any
    *                    non-null value
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         argument is less than, equal to, or greater than the second
    *
    * @throws ClassCastException if the arguments are not Comparable
    */
    public static int compareSafe(Comparator comparator, Object o1, Object o2, boolean fNullFirst)
        {
        if (comparator != null)
            {
            try
                {
                return comparator.compare(o1, o2);
                }
            catch (NullPointerException e) {}
            }

        if (o1 == null)
            {
            return o2 == null ? 0 : (fNullFirst ? -1 : +1);
            }

        if (o2 == null)
            {
            return (fNullFirst ? +1 : -1);
            }

        return ((Comparable) o1).compareTo(o2);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this Comparator.
    *
    * @return a String description of the Comparator
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + " (" + m_comparator + ')';
        }

    /**
    * Determine if two comparators are equal.
    *
    * @param o  the other comparator
    *
    * @return true if the passed object is equal to this
    */
    public boolean equals(Object o)
        {
        return o instanceof SafeComparator &&
            m_fNullFirst == ((SafeComparator) o).m_fNullFirst &&
            equals(m_comparator, ((SafeComparator) o).m_comparator);
        }

    /**
    * Return the hash code for this comparator.
    *
    * @return the hash code value for this comparator
    */
    public int hashCode()
        {
        Comparator<? super T> comparator = m_comparator;
        return (comparator == null ? 17 : comparator.hashCode())
                + (m_fNullFirst ? 1 : 0);
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_comparator = ExternalizableHelper.readObject(in);
        try
            {
            m_fNullFirst = in.readBoolean();
            }
        catch (EOFException e)
            {
            // from older release (<= 12.1.3)
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_comparator);
        out.writeBoolean(m_fNullFirst);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_comparator = in.readObject(0);
        try
            {
            m_fNullFirst = in.readBoolean(1);
            }
        catch (EOFException e)
            {
            // from older release (<= 12.1.3)
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_comparator);
        out.writeBoolean(1, m_fNullFirst);
        }


    // ----- constants ------------------------------------------------------

    /**
    * The trivial SafeComparator.
    */
    public static final SafeComparator INSTANCE = new SafeComparator();

    /**
    * Return the trivial SafeComparator.
    */
    public static <T> SafeComparator<T> INSTANCE()
        {
        return INSTANCE;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The wrapped Comparator. Could be null.
    */
    @JsonbProperty("comparator")
    protected Comparator<? super T> m_comparator;

    /**
     * Flag specifying if nulls should be sorted as less (first) or greater
     * (last) than all non-null elements. Default is "less than" (first).
     */
    @JsonbProperty("nullFirst")
    protected boolean m_fNullFirst = true;
    }
