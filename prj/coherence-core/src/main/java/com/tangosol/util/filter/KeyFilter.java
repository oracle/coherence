/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Filter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
* EntryFilter which checks whether an entry key belongs to a set.
* <p>
* <b>Note: this filter is not serializable and intended to be used only
* internally by other composite filters to reduce the remaining key set.</b>
* <p>
* As of Coherence 3.2, an equivalent functionality could be achieved using the
* {@link InFilter} as follows:
* <pre>
*   new InFilter(new KeyExtractor(IdentityExtractor.INSTANCE), setKeys);
* </pre>
*
* @author cp/gg 2002.11.01
*/
public class KeyFilter<T>
        extends    Base
        implements Filter<T>, EntryFilter<Object, T>, IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a key filter.
    *
    * @param setKeys  the keys that this filter will evaluate to true
    */
    public KeyFilter(Set<T> setKeys)
        {
        Set     set   = m_setKeys;
        boolean fInit = true;

        // [2011.03.07 coh, gg]
        // KeyFilter is used internally to encapsulate partially processed keys
        // (for example, see AnyFilter.applyIndex).
        // The format of those keys could be different, depending on the cache
        // topology; e.g. it's Binary for partitioned but Object for local caches.
        // As a result, during the entry evaluation, in the case of BinaryEntry,
        // we need to know whether or not to use the Binary or Object key format.
        // Unfortunately, at this time we don't see an elegant way to determine
        // that rather than a brute force approach below.
        for (Object oKey : setKeys)
            {
            if (fInit)
                {
                m_fBinary = oKey instanceof Binary;
                fInit     = false;
                }
            set.add(oKey);
            }
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(Object o)
        {
        throw new UnsupportedOperationException();
        }


    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return m_fBinary && entry instanceof BinaryEntry
                ? m_setKeys.contains(((BinaryEntry) entry).getBinaryKey())
                : m_setKeys.contains(entry.getKey());
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        return Math.min(m_setKeys.size(), setKeys.size());
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        setKeys.retainAll(m_setKeys);
        return null;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the set of keys that are evaluated to true by this filter.
    *
    * @return the set of keys
    */
    public Set getKeys()
        {
        return m_setKeys;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return "Key in " + Base.truncateString(m_setKeys, 255);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The keys that are evaluated to true by this filter.
    */
    protected Set m_setKeys = new HashSet();

    /**
    * Internal flag used to indicate whether or not the underlying set contains
    * keys in Binary format.
    */
    private transient boolean m_fBinary;
    }
