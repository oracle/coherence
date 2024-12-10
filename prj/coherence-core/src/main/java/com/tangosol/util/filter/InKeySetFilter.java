/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter that limits the underlying filter evaluation only to the specified
* set of keys.
* <p>
* When used in the context of
* {@link com.tangosol.net.NamedMap#addMapListener(com.tangosol.util.MapListener, Filter, boolean)
* NamedMap.addMapListener},
* InKeySetFilter is used as a vessel to portray an intent to listen to the
* provided set of keys, and as such, should not be wrapped by another filter.
* InKeySetFilter should also not be used as the view filter for a
* {@link com.tangosol.net.cache.ContinuousQueryCache} or a View Cache. For
* these use cases, equivalent functionality is possible by using an
* {@link InFilter} with a {@link com.tangosol.util.extractor.KeyExtractor}.
* <p>
* For example:
* <pre>
* new InKeySetFilter(Filters.equal(Person::getLastName, "Rabbit"), setKeys)
* </pre>
* or
* <pre>
* Filters.equal(Person::getLastName, "Rabbit").forKeys(setKeys);
* </pre>
* can be converted to:
* <pre>
* Filters.in(ValueExtractor.identity().fromKey(), setKeys).and(Filters.equal(Person::getLastName, "Rabbit"))
* </pre>
*
* @author gg 2006.06.12
*/
public class InKeySetFilter<T>
        extends    AbstractQueryRecorderFilter<T>
        implements EntryFilter<Object, T>, IndexAwareFilter<Object, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public InKeySetFilter()
        {
        }

    /**
    * Construct an InFilter for testing "In" condition.
    *
    * @param <K>      the key type
    * @param filter   the underlying filter
    * @param setKeys  the set of keys to limit the filter evaluation to
    */
    public <K> InKeySetFilter(Filter<T> filter, Set<K> setKeys)
        {
        m_filter  = filter;
        m_setKeys = setKeys;
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T o)
        {
        throw new UnsupportedOperationException();
        }

    public String toExpression()
        {
        return "IN KEYSET " + m_setKeys;
        }

// ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        Filter<T> filter = m_filter;
        return m_setKeys.contains(entry instanceof BinaryEntry ? ((BinaryEntry<?, ?>) entry).getBinaryKey() : entry.getKey()) &&
               (filter == null || InvocableMapHelper.evaluateEntry(filter, entry));
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        Filter<T> filter = m_filter;
        if (filter == null)
            {
            return 1;
            }

        return filter instanceof IndexAwareFilter
                ? ((IndexAwareFilter) filter).calculateEffectiveness(mapIndexes, m_setKeys)
                : ExtractorFilter.calculateIteratorEffectiveness(setKeys.size());
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        setKeys.retainAll(m_setKeys);
        if (setKeys.isEmpty())
            {
            return null;
            }
            
        Filter<T> filter = m_filter;
        return filter instanceof IndexAwareFilter
                ? ((IndexAwareFilter) filter).applyIndex(mapIndexes, setKeys)
                : filter;
        }


    // ----- accessors and helpers ------------------------------------------

    /**
    * Obtain the underlying Filter.
    *
    * @return the underlying filter
    */
    public Filter<T> getFilter()
        {
        return m_filter;
        }

    /**
    * Obtain the underlying key set.
    *
    * @return the underlying key set
    */
    public Set<?> getKeys()
        {
        return m_setKeys;
        }

    /**
    * Ensure that the underlying keys are converted using the specified
    * converter.
    * <p>
    * This method must be called prior to index application or evaluation when
    * the keys being evaluated exist in an internal form.
    * <p>
    * Note: for convenience, this method will be automatically called by the
    *       partitioned cache service when this filter appears as the outermost
    *       filter in a query.
    *
    * @param converter  the converter that should be used for key conversions
    */
    public synchronized void ensureConverted(Converter converter)
        {
        if (!m_fConverted)
            {
            Set setConv = new HashSet(m_setKeys.size());
            for (Iterator iter = m_setKeys.iterator(); iter.hasNext();)
                {
                setConv.add(converter.convert(iter.next()));
                }
            m_setKeys    = setConv;
            m_fConverted = true;
            }
        }

    /**
    * Ensure that the underlying keys are un-converted using the specified
    * converter.
    *
    * @param converter  the converter that should be used for key un-conversions
    *
    * @since Coherence 12.2.1.1.0
    */
    public synchronized void ensureUnconverted(Converter converter)
        {
        if (m_fConverted)
            {
            Set setConv = new HashSet(m_setKeys.size());
            for (Iterator iter = m_setKeys.iterator(); iter.hasNext();)
                {
                setConv.add(converter.convert(iter.next()));
                }
            m_setKeys    = setConv;
            m_fConverted = false;
            }
        }

    /**
    * Check if the underlying key set has been converted.
    *
    * @since Coherence 12.2.1.1.0
    *
    * @return {@code true} if the underlying key has been converted
    */
    public boolean isConverted()
        {
        return m_fConverted;
        }

    /**
    * Mark the underlying keys set as converted.
    *
    * @since Coherence 12.2.1.1.0
    */
    public void markConverted()
        {
        m_fConverted = true;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return "InKeySetFilter(" + m_filter + ", keys="
                + Base.truncateString(m_setKeys, 255) + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter = readObject(in);
        ExternalizableHelper.readCollection(in, m_setKeys = new HashSet(), null);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        ExternalizableHelper.writeCollection(out, m_setKeys);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter  = in.readObject(0);
        m_setKeys = (Set) in.readCollection(1, new HashSet());
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeCollection(1, m_setKeys);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying Filter.
    */
    @JsonbProperty("filter")
    private Filter<T> m_filter;

    /**
    * The underlying set of keys. This set is not exposed via any accessors
    * quite intentionally to ensure that this class is free to manipulate the
    * key set (by the "convert" method) without interfering with any client
    * logic.
    */
    @JsonbProperty("keys")
    private Set<?> m_setKeys;

    /**
    * A flag that indicates that the key set has been converted to internal
    * form.
    */
    private transient volatile boolean m_fConverted;
    }
