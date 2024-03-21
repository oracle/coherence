/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.collections.NullableConcurrentMap;
import com.tangosol.net.BackingMapContext;

import com.tangosol.util.filter.IndexAwareFilter;

import java.util.Comparator;
import java.util.Map;


/**
* ForwardOnlyMapIndex is a {@link MapIndex} implementation that unlike the
* {@link SimpleMapIndex} maintains only a forward index and not the inverse index.
* As a result, the content of {@link #getIndexContents()} is always empty, so this
* index cannot be used for querying by {@link IndexAwareFilter#applyIndex
* IndexAwareFilter}, with its primary use as a deserialization optimization.
*
* @author gg/hr/jh 2014.03.07
* @since Coherence 12.1.3
*/
public class ForwardOnlyMapIndex
        implements MapIndex
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an index for the given map.
    *
    * @param extractor  the ValueExtractor that is used to extract an indexed
    *                   value from a resource map entry
    * @param ctx        the {@link BackingMapContext context} associated with
    *                   the indexed cache
    * @param fOnDemand  if true, the forward index will be created "on-demand"
    *                   as the values are attempted to be accessed; otherwise
    *                   the forward index is populated proactively
    */
    public ForwardOnlyMapIndex(ValueExtractor extractor, BackingMapContext ctx, boolean fOnDemand)
        {
        Base.azzert(extractor != null);

        f_extractor  = extractor;
        f_ctx        = ctx;
        f_fLazy      = fOnDemand;
        f_mapForward = instantiateForwardIndex();
        }


    // ----- MapIndex interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public ValueExtractor getValueExtractor()
        {
        return f_extractor;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isOrdered()
        {
        // this question makes no sense for this index
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isPartial()
        {
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public Comparator getComparator()
        {
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public Map getIndexContents()
        {
        return NullImplementation.getMap();
        }

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        Object oValue = f_mapForward.get(oKey);
        if (oValue == null && !f_mapForward.containsKey(oKey))
            {
            if (f_fLazy)
                {
                Map mapBacking = f_ctx.getBackingMap();
                if (mapBacking.containsKey(oKey))
                    {
                    oValue = mapBacking.get(oKey);
                    }
                if (oValue != null)
                    {
                    oValue = f_ctx.getManagerContext().
                        getValueFromInternalConverter().convert(oValue);
                    f_mapForward.put(oKey, oValue);
                    return oValue;
                    }
                }
            return NO_VALUE;
            }
        else
            {
            return oValue;
            }
        }

    /**
    * {@inheritDoc}
    */
    public void insert(Map.Entry entry)
        {
        updateInternal(entry);
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
    public void delete(Map.Entry entry)
        {
        deleteInternal(entry);
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Instantiate the forward index.
    *
    * @return the forward index
    */
    protected Map instantiateForwardIndex()
        {
        return new NullableConcurrentMap();
        }

    /**
    * Update this index in response to an update operation on a cache.
    *
    * @param entry  the entry representing the object being updated
    */
    protected void updateInternal(Map.Entry entry)
        {
        Object oKey = entry instanceof BinaryEntry ?
                ((BinaryEntry) entry).getBinaryKey() : entry.getKey();

        if (!f_fLazy || f_mapForward.containsKey(oKey))
            {
            try
                {
                f_mapForward.put(oKey, InvocableMapHelper.extractFromEntry(f_extractor, entry));
                }
            catch (RuntimeException e)
                {
                Logger.warn("An Exception occurred during index update for key " + entry.getKey()
                    + ". The entry will be excluded from the index"
                    + (f_ctx == null ? "" : " for cache " + f_ctx.getCacheName()) + ".\n" + e + ":\n", e);

                f_mapForward.remove(oKey);
                }
            }
        }

    /**
    * Update this index in response to a remove operation on a cache.
    *
    * @param entry  the entry representing the object being removed
    */
    protected void deleteInternal(Map.Entry entry)
        {
        Object oKey = entry instanceof BinaryEntry ?
                ((BinaryEntry) entry).getBinaryKey() : entry.getKey();
        f_mapForward.remove(oKey);
        }


    // ----- Object interface -----------------------------------------------

    /**
    * Returns a string representation of this SimpleMapIndex.
    *
    * @return a String representation of this SimpleMapIndex
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
                + ": Extractor=" + getValueExtractor();
        }

    /**
    * Compares the specified object with this index for equality. Returns
    * <tt>true</tt> if the given object is also a SimpleMapIndex and the two
    * represent the same index.
    *
    * @param o object to be compared for equality with this MapIndex
    *
    * @return <tt>true</tt> if the specified object is equal to this index
    */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o == null || o.getClass() != this.getClass())
            {
            return false;
            }

        ForwardOnlyMapIndex that = (ForwardOnlyMapIndex) o;
        return Base.equals(this.getValueExtractor(), that.getValueExtractor());
        }

    /**
    * Returns the hash code value for this MapIndex.
    *
    * @return the hash code value for this MapIndex
    */
    public int hashCode()
        {
        return f_extractor.hashCode();
        }


    // ----- data members ---------------------------------------------------

    /**
    * ValueExtractor object that this MapIndex uses to extract an indexable
    * property value from a [converted] value stored in the resource map.
    */
    protected final ValueExtractor f_extractor;

    /**
    * Map that contains the index values (forward index). The keys of the Map
    * are the keys to the map being indexed and the values are the extracted
    * values. This map is used by the IndexAwareComparators to avoid a
    * conversion and value extraction steps.
    */
    protected final Map f_mapForward;

    /**
    * The {@link BackingMapContext context} associated with this index.
    */
    protected final BackingMapContext f_ctx;

    /**
    * The "lazy" flag.
    */
    protected boolean f_fLazy;
    }
