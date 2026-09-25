/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.util.ValueExtractor;


/**
* Abstract aggregator that processes numeric values extracted from a set of
* entries in a Map. All the extracted Number objects will be treated as Java
* <tt>long</tt> values and the result of the aggregator is a Long.
* If the set of entries is empty, a <tt>null</tt> result is returned.
*
* @param <T>  the type of the value to extract from
*
* @author cp/gg/jh  2005.07.19
* @since Coherence 3.1
*/
public abstract class AbstractLongAggregator<T>
        extends AbstractAggregator<Object, Object, T, Number, Long>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public AbstractLongAggregator()
        {
        super();
        }

    /**
    * Construct an AbstractLongAggregator object.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public AbstractLongAggregator(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct an AbstractLongAggregator object.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public AbstractLongAggregator(String sMethod)
        {
        super(sMethod);
        }


    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        m_count = 0;
        }

    /**
    * {@inheritDoc}
    */
    protected Long finalizeResult(boolean fFinal)
        {
        return m_count == 0 ? null : m_lResult;
        }

    @Override
    public Object snapshotState()
        {
        ensureInitialized(false);
        return new Object[] {Integer.valueOf(m_count), Long.valueOf(m_lResult)};
        }

    @Override
    public void restoreState(Object state)
        {
        if (!(state instanceof Object[]) || ((Object[]) state).length != 2)
            {
            throw new IllegalArgumentException("invalid long aggregator state snapshot");
            }

        Object[] aoState = (Object[]) state;
        int      cValues = ((Number) aoState[0]).intValue();
        if (cValues < 0)
            {
            throw new IllegalArgumentException("negative long aggregator value count");
            }

        ensureInitialized(false);
        m_count   = cValues;
        m_lResult = ((Number) aoState[1]).longValue();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The count of processed entries.
    */
    protected transient int m_count;

    /**
    * The running result value.
    */
    protected transient long m_lResult;
    }
