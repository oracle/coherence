/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;

import com.tangosol.util.ConditionalIndex;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Comparator;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;


/**
* An IndexAwareExtractor implementation that is only used to create a
* {@link ConditionalIndex}.
* <p>
* Note: the underlying ValueExtractor is used for value extraction during
* index creation and is the extractor that is associated with the created
* {@link ConditionalIndex} in the given index map.  Using the
* ConditionalExtractor to extract values in not supported.
*
* @author tb 2010.02.08
* @since Coherence 3.6
*/
public class ConditionalExtractor<T, E>
        extends AbstractExtractor<T, E>
        implements IndexAwareExtractor<T, E>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the ConditionalExtractor.
    */
    public ConditionalExtractor()
        {
        }

    /**
    * Construct the ConditionalExtractor.
    *
    * @param filter         the filter used by this extractor to create a
    *                       {@link ConditionalIndex}; must not be null.
    * @param extractor      the extractor used by this extractor to create a
    *                       {@link ConditionalIndex}; Note that the created
    *                       index will be associated with this extractor in
    *                       the given index map; must not be null
    * @param fForwardIndex  specifies whether or not this extractor
    *                       will create a {@link ConditionalIndex}
    *                       that supports a forward map
    */
    public ConditionalExtractor(Filter filter, ValueExtractor<T, E> extractor,
            boolean fForwardIndex)
        {
        this(filter, extractor, fForwardIndex, true);
        }

    /**
    * Construct the ConditionalExtractor.
    *
    * @param filter         the filter used by this extractor to create a
    *                       {@link ConditionalIndex}; must not be null.
    * @param extractor      the extractor used by this extractor to create a
    *                       {@link ConditionalIndex}; Note that the created
    *                       index will be associated with this extractor in
    *                       the given index map; must not be null
    * @param fForwardIndex  specifies whether or not this extractor
    *                       will create a {@link ConditionalIndex}
    *                       that supports a forward map
    * @param fOptimizeMV    specifies whether an attempt should be made to
    *                       search the forward map for an existing reference
    *                       that is "equal" to the specified multi-value
    *                       and use it instead (if available) to reduce the
    *                       index memory footprint
    */
    public ConditionalExtractor(Filter filter, ValueExtractor<T, E> extractor,
            boolean fForwardIndex, boolean fOptimizeMV)
        {
        azzert(filter != null && extractor != null,
               "Filter and extractor must not be null");

        m_filter        = filter;
        m_extractor     = extractor;
        m_fForwardIndex = fForwardIndex;
        m_fOptimizeMV   = fOptimizeMV;
        }


    // ----- IndexAwareExtractor interface ----------------------------------

    /**
    * {@inheritDoc}
    */
    public MapIndex createIndex(boolean fOrdered, Comparator comparator,
            Map<ValueExtractor<T, E>, MapIndex> mapIndex, BackingMapContext ctx)
        {
        ValueExtractor extractor = m_extractor;
        MapIndex       index     = mapIndex.get(extractor);

        if (index != null)
            {
            if (index instanceof ConditionalIndex
              && equals(((ConditionalIndex) index).getFilter(), m_filter))
                {
                return null;
                }
            throw new IllegalArgumentException(
                    "Repetitive addIndex call for " + this);
            }

        ConditionalIndex indexNew = new ConditionalIndex(m_filter, extractor, fOrdered,
                comparator, m_fForwardIndex, ctx);

        indexNew.setOptimizeMV(m_fOptimizeMV);

        mapIndex.put(extractor, indexNew);
        return indexNew;
        }

    /**
    * {@inheritDoc}
    */
    public MapIndex destroyIndex(Map<ValueExtractor<T, E>, MapIndex>    mapIndex)
        {
        return mapIndex.remove(m_extractor);
        }

    // ---- accessors -------------------------------------------------------

    public ValueExtractor getExtractor()
        {
        return m_extractor;
        }

    // ----- ValueExtractor interface ---------------------------------------

    /**
    * Using a ConditionalExtractor to extract values in not supported.
    *
    * @throws UnsupportedOperationException always
    */
    public E extract(Object oTarget)
        {
        throw new UnsupportedOperationException(
            "ConditionalExtractor may not be used as an extractor.");
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter        = readObject(in);
        m_extractor     = readObject(in);
        m_fForwardIndex = in.readBoolean();
        m_fOptimizeMV   = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        writeObject(out, m_extractor);
        out.writeBoolean(m_fForwardIndex);
        out.writeBoolean(m_fOptimizeMV);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter        = in.readObject(0);
        m_extractor     = in.readObject(1);
        m_fForwardIndex = in.readBoolean(2);
        m_fOptimizeMV   = in.readBoolean(3);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeObject(1, m_extractor);
        out.writeBoolean(2, m_fForwardIndex);
        out.writeBoolean(3, m_fOptimizeMV);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        if (o instanceof ConditionalExtractor)
            {
            ConditionalExtractor that = (ConditionalExtractor) o;
            return equals(m_filter, that.m_filter) &&
                equals(m_extractor, that.m_extractor) &&
                m_fForwardIndex == that.m_fForwardIndex &&
                m_fOptimizeMV == that.m_fOptimizeMV;
            }

        return false;
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        return m_filter.hashCode() ^ m_extractor.hashCode();
        }

    /**
    * Return a human-readable description for this ConditionalExtractor.
    *
    * @return a String description of the ConditionalExtractor
    */
    public String toString()
        {
        return "ConditionalExtractor" +
            "(extractor=" + m_extractor + ", filter=" + m_filter + ")";
        }


    // ----- data members ---------------------------------------------------

    /**
    * The filter used by this extractor.
    */
    @JsonbProperty("filter")
    protected Filter m_filter;

    /**
    * The underlying extractor.
    */
    @JsonbProperty("extractor")
    protected ValueExtractor<T, E> m_extractor;

    /**
    * Specifies whether or not this extractor will create a
    * {@link ConditionalIndex} that supports a forward index.
    */
    @JsonbProperty("forwardIndex")
    protected boolean m_fForwardIndex;

    /**
    * For an indexed value that is a multi-value (Collection or array) this flag
    * specifies whether an attempt should be made to search the forward map
    * for an existing reference that is "equal" to the specified multi-value
    * and use it instead (if available) to reduce the index memory footprint.
    * <p>
    * Note, that even if this optimization is allowed, the full search could be
    * quite expensive and our algorithm will always limit the number of cycles
    * it spends on the search.
    */
    @JsonbProperty("optimizeMV")
    protected boolean m_fOptimizeMV;
    }
