/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.util.ValueExtractor;


/**
* Abstract aggregator that processes numeric values extracted from a set of
* entries in a Map. All the extracted Number objects will be treated as Java
* <tt>double</tt> values and the result of the aggregator is a Double.
* If the set of entries is empty, a <tt>null</tt> result is returned.
*
* @param <T>  the type of the value to extract from
*
* @author cp/gg/jh  2005.07.19
* @since Coherence 3.1
*/
public abstract class AbstractDoubleAggregator<T>
        extends AbstractAggregator<Object, Object, T, Number, Double>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public AbstractDoubleAggregator()
        {
        super();
        }

    /**
    * Construct an AbstractDoubleAggregator object.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public AbstractDoubleAggregator(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct an AbstractDoubleAggregator object.
    *
    * @param sMethod  the name of the method that returns a value in
    *                 the form of any Java object that is a {@link Number}
    */
    public AbstractDoubleAggregator(String sMethod)
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
    protected Object finalizeResult(boolean fFinal)
        {
        return m_count == 0 ? null : m_dflResult;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The count of processed entries.
    */
    protected transient int m_count;

    /**
    * The running result value.
    */
    protected transient double m_dflResult;
    }