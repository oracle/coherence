/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Comparator;
import java.util.Objects;

import jakarta.json.bind.annotation.JsonbProperty;


/**
* Abstract aggregator that processes values extracted from a set of entries
* in a Map, with knowledge of how to compare those values. There are two way
* to use the AbstractComparableAggregator:
* <ul>
* <li>All the extracted objects must implement {@link Comparable}, or</li>
* <li>The AbstractComparableAggregator has to be provided with a
* {@link Comparator} object.</li>
* </ul>
* If the set of entries passed to {@link #accumulate} is empty, a
* <tt>null</tt> result is returned.
*
* @param <T>  the type of the value to extract from
* @param <R>  the type of the aggregation result
*
* @author gg  2006.02.13
* @since Coherence 3.2
*/
public abstract class AbstractComparableAggregator<T, R>
        extends AbstractAggregator<Object, Object, T, R, R>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public AbstractComparableAggregator()
        {
        super();
        }

    /**
    * Construct an AbstractComparableAggregator object.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any object that implements the {@link Comparable}
    *                   interface
    */
    public <E extends Comparable<? super E>> AbstractComparableAggregator(ValueExtractor<? super T, ? extends E> extractor)
        {
        super((ValueExtractor<? super T, ? extends R>) extractor);
        }

    /**
    * Construct an AbstractComparableAggregator object.
    *
    * @param extractor  the extractor that provides an object to be compared
    * @param comparator the comparator used to compare the extracted object
    */
    public AbstractComparableAggregator(ValueExtractor<? super T, ? extends R> extractor, Comparator<? super R> comparator)
        {
        super(extractor);

        m_comparator = comparator;
        }

    /**
    * Construct an AbstractComparableAggregator object.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any object that implements the {@link Comparable}
    *                 interface
    */
    public AbstractComparableAggregator(String sMethod)
        {
        super(sMethod);
        }


    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        m_count    = 0;
        m_cSupport = 0;
        m_oResult  = null;
        }

    /**
    * {@inheritDoc}
    */
    protected R finalizeResult(boolean fFinal)
        {
        return m_count == 0 ? null : m_oResult;
        }

    @Override
    public Object snapshotState()
        {
        ensureInitialized(false);
        return new Object[] {Integer.valueOf(m_count), Integer.valueOf(m_cSupport), m_oResult};
        }

    @Override
    public void restoreState(Object state)
        {
        if (!(state instanceof Object[]) || ((Object[]) state).length != 3)
            {
            throw new IllegalArgumentException("invalid comparable aggregator state snapshot");
            }

        Object[] aoState  = (Object[]) state;
        int      cValues  = ((Number) aoState[0]).intValue();
        int      cSupport = ((Number) aoState[1]).intValue();
        if (cValues < 0 || cSupport < 0 || cSupport > cValues
                || cValues == 0 && aoState[2] != null)
            {
            throw new IllegalArgumentException("invalid comparable aggregator state values");
            }

        ensureInitialized(false);
        m_count    = cValues;
        m_cSupport = cSupport;
        m_oResult  = (R) aoState[2];
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the comparator used to compare extracted values.
    *
    * @return the comparator, or {@code null}
    */
    public Comparator<? super R> getComparator()
        {
        return m_comparator;
        }

    // ----- Object methods ------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        return super.equals(o)
                && Objects.equals(m_comparator,
                        ((AbstractComparableAggregator<?, ?>) o).m_comparator);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(super.hashCode(), m_comparator);
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);
        m_comparator = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);
        writeObject(out, m_comparator);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);
        m_comparator = in.readObject(2);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);
        out.writeObject(2, m_comparator);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The count of processed entries.
    */
    protected transient int m_count;

    /**
    * The number of current entries equal to the retained extremum.
    */
    protected transient int m_cSupport;

    /**
    * The running result value.
    */
    protected transient R m_oResult;

    /**
    * The comparator to use for comparing extracted values.
    */
    @JsonbProperty("comparator")
    protected Comparator<? super R> m_comparator;
    }
