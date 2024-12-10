/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.BackingMapContext;

import com.tangosol.net.CacheFactory;
import java.util.Comparator;
import java.util.Map;


/**
* ConditionalIndex is a {@link MapIndex} implementation that uses an associated
* filter to evaluate whether or not an entry should be indexed.  An entry's
* extracted value is only added to the index if the filter evaluates to true.
*
* @author tb 2010.02.08
* @since Coherence 3.6
*/
public class ConditionalIndex
        extends SimpleMapIndex
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ConditionalIndex.
    *
    * @param filter         the filter that is used to evaluate the entries of
    *                       the resource map that is being indexed
    * @param extractor      the {@link ValueExtractor} that is used to extract
    *                       an indexed value from a resource map entry
    * @param fOrdered       true iff the contents of the indexed information
    *                       should be ordered; false otherwise
    * @param comparator     the Comparator object which imposes an ordering on
    *                       entries in the index map; or <tt>null</tt> if the
    *                       entries' values natural ordering should be used
    * @param fForwardIndex  specifies whether or not this index supports a
    *                       forward map
    * @param ctx            the {@link BackingMapContext context} associated with
    *                       this index
    */
    public ConditionalIndex(Filter filter, ValueExtractor extractor,
            boolean fOrdered, Comparator comparator, boolean fForwardIndex,
            BackingMapContext ctx)
        {
        super(extractor, fOrdered, comparator, false, ctx);

        f_filter = filter;
        m_fPartial = false;

        initialize(fForwardIndex);
        }


    // ----- SimpleMapIndex methods -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        if (isForwardIndexSupported())
            {
            Map    mapForward = m_mapForward;
            Object oValue     = mapForward.get(oKey);

            return oValue != null || mapForward.containsKey(oKey) ?
                    oValue : MapIndex.NO_VALUE;
            }
        return MapIndex.NO_VALUE;
        }

    /**
    * {@inheritDoc}
    */
    protected Map.Entry getForwardEntry(Object oKey)
        {
        return m_fForwardIndex ? super.getForwardEntry(oKey) : null;
        }

    /**
    * {@inheritDoc}
    */
    protected void removeForwardEntry(Object oKey)
        {
        if (m_fForwardIndex)
            {
            super.removeForwardEntry(oKey);
            }
        }

    /**
    * {@inheritDoc}
    */
    protected Map instantiateForwardIndex()
        {
        return m_fForwardIndex ? super.instantiateForwardIndex() : null;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isPartial()
        {
        return m_fPartial || super.isPartial();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Get the associated filter.
    *
    * @return the filter
    */
    public Filter getFilter()
        {
        return f_filter;
        }

    /**
    * Evaluate the given entry using this index's filter.  If the entry does
    * not pass the filter then it should be excluded from this index, making
    * this a partial index.
    *
    * @param entry  the entry to evaluate
    *
    * @return true if the entry passes the filter, false otherwise
    */
    protected boolean evaluateEntry(Map.Entry entry)
        {
        try
            {
            if (InvocableMapHelper.evaluateEntry(f_filter, entry))
                {
                return true;
                }
            }
        catch (RuntimeException e)
            {
            // COH-6447: don't drop the index upon exception
            }

        m_fPartial = true;
        return false;
        }

    /**
     * {@inheritDoc}
     */
    public void update(Map.Entry entry)
        {
        updateInternal(entry);
        }

    /**
    * {@inheritDoc}
    */
    protected void insertInternal(Map.Entry entry)
        {
        if (evaluateEntry(entry))
            {
            super.insertInternal(entry);
            }
        }

    /**
    * {@inheritDoc}
    */
    protected void updateInternal(Map.Entry entry)
        {
        if (evaluateEntry(entry))
            {
            super.updateInternal(entry);
            }
        else
            {
            deleteInternal(entry);
            }
        }

    /**
    * {@inheritDoc}
    */
    protected void deleteInternal(Map.Entry entry)
        {
        try
            {
            if (entry instanceof MapTrigger.Entry &&
                !InvocableMapHelper.evaluateOriginalEntry(getFilter(), (MapTrigger.Entry) entry))
                {
                // the "original" entry would have been excluded; nothing to do
                return;
                }
            }
        catch (RuntimeException e)
            {
            // COH-6447: attempt the delete anyway because the filter may have
            // allowed this value previously and it may be in the index
            }

        super.deleteInternal(entry);
        }

    // ----- Object interface -----------------------------------------------

    /**
    * Returns a string representation of this ConditionalIndex.  The string
    * representation consists of the SimpleMapIndex representation concatenated
    * by the Filter and the ForwardIndexSupported flag.
    *
    * @return a String representation of this ConditionalIndex
    */
    public String toString()
        {
        return super.toString() +
            ", Filter="         + getFilter() +
            ", ForwardIndex="   + isForwardIndexSupported();
        }

    /**
    * Compares the specified object with this index for equality.  Returns
    * <tt>true</tt> if the given object is also a SimpleMapIndex and the two
    * represent the same index.
    *
    * @param o object to be compared for equality with this MapIndex
    *
    * @return <tt>true</tt> if the specified object is equal to this index
    */
    public boolean equals(Object o)
        {
        if (!super.equals(o) || !(o instanceof ConditionalIndex))
            {
            return false;
            }

        ConditionalIndex that = (ConditionalIndex) o;
        return equals(this.getFilter(), that.getFilter())
            && this.isForwardIndexSupported() == that.isForwardIndexSupported();
        }


    // ----- data members ---------------------------------------------------

    /**
    * Filter object that this index uses to evaluate entries.
    * An entry's extracted value is only added to the index if this filter
    * evaluates to true.
    */
    private final Filter<?> f_filter;

    /**
    * Specifies whether or not this ConditionalIndex is a partial index.
    * The index is regarded as partial if any entry in the indexed map has
    * been excluded from this index.
    */
    private volatile boolean m_fPartial;
    }
