/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.processors;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

/**
 * An entry processor to test whether an index is present
 * on a cache.
 *
 * For example
 *
 * boolean present = cache.invoke(null, new HasIndexProcessor(extractor, sorted, comparator);
 *
 * @author jk  2013.12.20
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class HasIndexProcessor<K,V>
        extends AbstractProcessor<K,V,Boolean>
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructor for serialization
     */
    public HasIndexProcessor()
        {
        }

    /**
     * Create a new HasIndexProcessor that checks for an index
     * on a cache with the specified attributes.
     *
     * @param valueExtractor the {@link ValueExtractor} used to create the index
     * @param sorted         a flag indicating whether the index is sorted
     * @param comparator     a {@link Comparator} used to sort the index
     */
    public HasIndexProcessor(ValueExtractor valueExtractor, boolean sorted,
                             Comparator comparator)
        {
        m_valueExtractor = valueExtractor;
        m_sorted         = sorted;
        m_comparator     = new SafeComparator(comparator);
        }


    // ----- AbstractProcessor methods --------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean process(InvocableMap.Entry entry)
        {
        BinaryEntry binaryEntry = (BinaryEntry) entry;

        Map         indexes     = binaryEntry.getBackingMapContext().getIndexMap();
        MapIndex    index       = (MapIndex) indexes.get(m_valueExtractor);

        return index != null && index.isOrdered() == m_sorted
                && Base.equals(index.getComparator(), m_comparator);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_valueExtractor = (ValueExtractor) in.readObject(1);
        m_sorted         = in.readBoolean(2);
        m_comparator     = (Comparator) in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(1, m_valueExtractor);
        out.writeBoolean(2, m_sorted);
        out.writeObject(3, m_comparator);
        }


    // ----- data members ---------------------------------------------------

    /** the {@link ValueExtractor} used to create the index */
    protected ValueExtractor m_valueExtractor;

    /** a flag indicating whether the index is sorted */
    protected boolean        m_sorted;

    /** a {@link Comparator} used to sort the index */
    protected Comparator     m_comparator;
    }
