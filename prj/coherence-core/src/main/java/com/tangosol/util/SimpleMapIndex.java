/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.oracle.coherence.common.collections.NullableConcurrentMap;
import com.oracle.coherence.common.collections.NullableSortedMap;

import com.tangosol.net.BackingMapContext;

import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.cache.SimpleMemoryCalculator;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.MultiExtractor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
* SimpleMapIndex is a MapIndex implementation used to correlate property values
* extracted from resource map entries with corresponding keys using what is
* commonly known as an <i>Inverted Index algorithm</i>.
*
* @author tb 2009.03.19
* @since Coherence 3.5
*/
@SuppressWarnings({"rawtypes", "unchecked"})
public class SimpleMapIndex
        extends    Base
        implements MapIndex
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an index for the given map.
    *
    * @param extractor   the ValueExtractor that is used to extract an indexed
    *                    value from a resource map entry
    * @param fOrdered    true iff the contents of the indexed information
    *                    should be ordered; false otherwise
    * @param comparator  the Comparator object which imposes an ordering
    *                    on entries in the index map; or <tt>null</tt>
    *                    if the entries' values natural ordering should be used
    * @param ctx         the {@link BackingMapContext context} associated with
    *                    the indexed cache
    */
    public SimpleMapIndex(ValueExtractor extractor, boolean fOrdered,
                          Comparator comparator, BackingMapContext ctx)
        {
        this(extractor, fOrdered, comparator, true, ctx);
        }

    /**
    * Construct an index for the given map.
    *
    * @param extractor   the ValueExtractor that is used to extract an indexed
    *                    value from a resource map entry
    * @param fOrdered    true iff the contents of the indexed information
    *                    should be ordered; false otherwise
    * @param comparator  the Comparator object which imposes an ordering
    *                    on entries in the index map; or <tt>null</tt>
    *                    if the entries' values natural ordering should be used
    * @param fInit       initialize the index if true
    * @param ctx         the {@link BackingMapContext context} associated with
    *                    the indexed cache
    */
    protected SimpleMapIndex(ValueExtractor extractor, boolean fOrdered,
                             Comparator comparator, boolean fInit,
                             BackingMapContext ctx)
        {
        azzert(extractor != null);

        m_extractor        = extractor;
        m_fOrdered         = fOrdered;
        m_comparator       = comparator;
        m_fSplitCollection = !(extractor instanceof MultiExtractor);
        m_ctx              = ctx;
        m_cUnits           = 0;
        m_calculator       = instantiateCalculator();
        m_fImmutableValues = extractor instanceof KeyExtractor ||
                extractor instanceof AbstractExtractor &&
                ((AbstractExtractor) extractor).getTarget() == AbstractExtractor.KEY;

        if (fInit)
            {
            initialize(true);
            }
        }


    // ----- MapIndex interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public ValueExtractor getValueExtractor()
        {
        return m_extractor;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isOrdered()
        {
        return m_fOrdered;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isPartial()
        {
        // an index is partial if there are (corrupted) entries
        // which are in the cache but not in the index
        return !m_setKeyExcluded.isEmpty();
        }

    /**
    * {@inheritDoc}
    */
    public Comparator getComparator()
        {
        return m_comparator;
        }

    /**
    * {@inheritDoc}
    */
    public Map getIndexContents()
        {
        return m_mapInverse;
        }

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        Map map = m_mapForward;
        if (map == null)
            {
            return NO_VALUE;
            }

        Object oValue = map.get(oKey);
        return oValue == null && !map.containsKey(oKey) ? NO_VALUE : oValue;
        }

    /**
    * {@inheritDoc}
    */
    public void insert(Map.Entry entry)
        {
        insertInternal(entry);
        }

    /**
    * {@inheritDoc}
    */
    public void update(Map.Entry entry)
        {
        if (!m_fImmutableValues)
            {
            updateInternal(entry);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void delete(Map.Entry entry)
        {
        deleteInternal(entry);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Specifies whether an attempt will be made to search the forward map
    * for an existing reference that is "equal" to the specified mutli-value
    * and use it instead (if available) to reduce the index memory footprint.
    *
    * @return true iff multi-value optimization is allowed
    */
    public boolean isOptimizeMV()
        {
        return m_fOptimizeMV;
        }

    /**
    * For an indexed value that is a multi-value (Collection or array) this flag
    * specifies whether an attempt should be made to search the forward map
    * for an existing reference that is "equal" to the specified mutli-value
    * and use it instead (if available) to reduce the index memory footprint.
    * <p>
    * Note, that even if this optimization is allowed, the full search could be
    * quite expensive and our algorithm will always limit the number of cycles
    * it spends on the search.
    *
    * @param fOptimizeMV  the boolean value to set
    */
    public void setOptimizeMV(boolean fOptimizeMV)
        {
        m_fOptimizeMV = fOptimizeMV;
        }

    /**
    * Retrieve the size of this index in units (as defined by the
    * {@link #getCalculator() UnitCalculator}).
    *
    * @return the main, non-partitioned index size
    */
    public long getUnits()
        {
        return m_cUnits;
        }

    /**
    * Set the size of this index in units (as defined by the
    * {@link #getCalculator() UnitCalculator}).
    *
    * @param cUnits  new index size
    */
    protected void setUnits(long cUnits)
        {
        m_cUnits = Math.max(cUnits, 0);
        }

    /**
    * Return the UnitCalculator used to size this index's contents.
    *
    * @return the unit calculator
    */
    public UnitCalculator getCalculator()
        {
        return m_calculator;
        }

    /**
    * Determine whether or not this SimpleMapIndex supports a forward index.
    *
    * @return true if this SimpleMapIndex supports a forward index; false
    *        otherwise
    */
    public boolean isForwardIndexSupported()
        {
        return m_fForwardIndex;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Initialize the index's data structures.
    *
    * @param fForwardIndex  specifies whether or not this index supports a
    *                       forward map
    */
    protected void initialize(boolean fForwardIndex)
        {
        m_fForwardIndex  = fForwardIndex;
        m_mapInverse     = instantiateInverseIndex(m_fOrdered, m_comparator);
        m_mapForward     = fForwardIndex ? instantiateForwardIndex() : null;
        m_setKeyExcluded = new SafeHashSet();
        }

    /**
    * Get the forward index entry associated with the specified key.
    *
    * @param oKey  the key
    *
    * @return the entry associated with the given key
    */
    protected Map.Entry getForwardEntry(Object oKey)
        {
        return m_mapForward == null ? null : ((NullableConcurrentMap) m_mapForward).getEntry(oKey);
        }

    /**
    * Add a new mapping to the forward index map.
    *
    * @param oKey         the key to add to the mapping
    * @param oIxValueNew  the new value to add to the mapping
    */
    protected void addForwardEntry(Object oKey, Object oIxValueNew)
        {
        Map mapForward = m_mapForward;
        if (mapForward != null)
            {
            mapForward.put(oKey, oIxValueNew);

            // add the overhead of creating a new map entry
            onMappingAdded();
            }
        }

    /**
    * Update a mapping in the forward index map.
    *
    * @param oKey         the key to update
    * @param oIxValueNew  the new value for the mapping
    */
    protected void updateForwardEntry(Object oKey, Object oIxValueNew)
        {
        Map mapForward = m_mapForward;
        if (mapForward != null)
            {
            mapForward.put(oKey, oIxValueNew);
            }
        }

    /**
    * Remove the forward index entry for the specified key.
    *
    * @param oKey  the key to remove the forward index entry for
    */
    protected void removeForwardEntry(Object oKey)
        {
        if (m_mapForward != null)
            {
            m_mapForward.remove(oKey);

            // remove the overhead of a map entry
            onMappingRemoved();
            }
        }

    /**
    * Extract the "new" value from the specified entry.
    *
    * @param entry  the entry to extract the "new" value from
    *
    * @return the extracted "new" value, or NO_VALUE if the extraction failed
    */
    protected Object extractNewValue(Map.Entry entry)
        {
        try
            {
            return InvocableMapHelper.extractFromEntry(m_extractor, entry);
            }
        catch (RuntimeException e)
            {
            Logger.warn("An Exception occurred during index update for key " + entry.getKey()
                        + ". The entry will be excluded from the index"
                        + (m_ctx == null ? "" : " for cache " + m_ctx.getCacheName()) + ".\n" + e + ":\n", e);

            return NO_VALUE;
            }
        }

    /**
    * Extract the "old" value from the specified entry.
    *
    * @param entry  the entry to extract the "old" value from
    *
    * @return the extracted "old" value, or NO_VALUE if the extraction failed
    */
    protected Object extractOldValue(MapTrigger.Entry entry)
        {
        try
            {
            return InvocableMapHelper.extractOriginalFromEntry(m_extractor, entry);
            }
        catch (RuntimeException e)
            {
            return NO_VALUE;
            }
        }

    /**
    * Return a Collection representation of the specified value, which could be
    * a Collection, Object[], scalar, or NO_VALUE.
    *
    * @param oValue  the value
    *
    * @return a Collection representation of the specified value, or an empty
    *         Collection if NO_VALUE
    */
    protected Collection ensureCollection(Object oValue)
        {
        if (oValue == NO_VALUE)
            {
            return Collections.emptySet();
            }
        else if (oValue instanceof Collection)
            {
            return (Collection) oValue;
            }
        else if (oValue instanceof Object[])
            {
            return new ImmutableArrayList((Object[]) oValue).getSet();
            }
        else
            {
            return Collections.singleton(oValue);
            }
        }

    /**
    * Instantiate the forward index.
    * <p>
    * Note: To optimize the memory footprint of the forward index, any
    * subclasses of the SimpleMapIndex that override this method must also
    * implement the {@link #getForwardEntry(Object)} method accordingly.
    *
    * @return the forward index
    */
    protected Map instantiateForwardIndex()
        {
        // add the overhead of creating a new map
        setUnits(getUnits() + IndexCalculator.MAP_OVERHEAD);

        return new NullableConcurrentMap();
        }

    /**
    * Instantiate the inverse index.
    *
    * @param fOrdered    true iff the contents of the indexed information
    *                    should be ordered; false otherwise
    * @param comparator  the Comparator object which imposes an ordering
    *                    on entries in the index map; or <tt>null</tt>
    *                    if the entries' values natural ordering should be
    *                    used
    *
    * @return the inverse index
    */
    protected Map instantiateInverseIndex(boolean fOrdered, Comparator comparator)
        {
        // add the overhead of creating a new map
        setUnits(getUnits() + IndexCalculator.MAP_OVERHEAD);

        if (fOrdered)
            {
            if (!(comparator instanceof SafeComparator))
                {
                comparator = new SafeComparator(comparator);
                }
            return new NullableSortedMap(comparator);
            }
        return new NullableConcurrentMap();
        }

    /**
    * Update this index in response to a insert operation on a cache.
    *
    * @param entry  the entry representing the object being inserted
    */
    protected void insertInternal(Map.Entry entry)
        {
        Object oKey = entry instanceof BinaryEntry ?
                ((BinaryEntry) entry).getBinaryKey() : entry.getKey();

        Object oIxValue = extractNewValue(entry);
        synchronized (this)
            {
            if (oIxValue == NO_VALUE)
                {
                // COH-6447: exclude corrupted entries from index and keep track of them
                updateExcludedKeys(entry, true);
                }
            else
                {
                // add a new mapping(s) to the inverse index
                oIxValue = addInverseMapping(oIxValue, oKey);

                addForwardEntry(oKey, oIxValue);
                }
            }
        }

    /**
    * Update this index in response to an update operation on a cache.
    *
    * @param entry  the entry representing the object being updated
    */
    protected void updateInternal(Map.Entry entry)
        {
        Object oKey        = entry instanceof BinaryEntry ?
                ((BinaryEntry) entry).getBinaryKey() : entry.getKey();
        Object oIxValueNew = extractNewValue(entry);

        synchronized (this)
            {
            Object    oIxValueOld;
            Map.Entry entryFwd = getForwardEntry(oKey);
            if (entryFwd == null)
                {
                if (entry instanceof MapTrigger.Entry)
                    {
                    oIxValueOld = extractOldValue((MapTrigger.Entry) entry);
                    }
                else
                    {
                    throw new IllegalStateException("Cannot extract the old value");
                    }
                }
            else
                {
                // replace the key reference with a pre-existing equivalent
                oKey        = entryFwd.getKey();
                oIxValueOld = entryFwd.getValue();
                }

            // replace the mapping(s) in the inverse index (if necessary);
            // the inverse index needs to be updated if either the extracted
            // values do not match, or if the partial index did not contain the
            // previous entry (COH-10259)
            if (!equals(oIxValueOld, oIxValueNew) || (entryFwd == null && isPartial()))
                {
                // remove the old mapping(s) from the inverse index
                if (oIxValueOld == NO_VALUE)
                    {
                    // extraction of old value failed; must do a full-scan, ensuring that
                    // any mappings to collection element values in the "new" value will remain
                    // (COH-7206)
                    removeInverseMapping(NO_VALUE, oKey, ensureCollection(oIxValueNew));
                    }
                else if (m_fSplitCollection && oIxValueOld instanceof Collection ||
                        oIxValueOld instanceof Object[])
                    {
                    // Note: it's important to only remove the elements that are no longer
                    //       present in the new value (see COH-7206)
                    removeInverseMapping(collectRemoved(oIxValueOld, oIxValueNew), oKey);
                    }
                else
                    {
                    removeInverseMapping(oIxValueOld, oKey);
                    }

                if (oIxValueNew == NO_VALUE)
                    {
                    // COH-6447: exclude corrupted entries from index and keep track of them
                    if (entryFwd != null)
                        {
                        removeForwardEntry(oKey);
                        }

                    updateExcludedKeys(entry, true);
                    }
                else
                    {
                    // add a new mapping(s) to the inverse index
                    oIxValueNew = addInverseMapping(oIxValueNew, oKey);

                    // replace the mapping in the forward index
                    if (entryFwd == null)
                        {
                        addForwardEntry(oKey, oIxValueNew);
                        }
                    else
                        {
                        updateForwardEntry(oKey, oIxValueNew);
                        }

                    // entry was successfully updated, ensure that the key is not excluded
                    updateExcludedKeys(entry, false);
                    }
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

        synchronized (this)
            {
            Object oIxValueOld;
            Map    mapForward = m_mapForward;
            if (mapForward == null)
                {
                if (entry instanceof MapTrigger.Entry)
                    {
                    oIxValueOld = extractOldValue((MapTrigger.Entry) entry);
                    }
                else
                    {
                    throw new IllegalStateException("Cannot extract the old value");
                    }
                }
            else
                {
                // remove the mapping from the forward index
                oIxValueOld = mapForward.remove(oKey);

                // remove the overhead of deleting a map entry
                onMappingRemoved();
                }

            // remove the mapping(s) from the inverse index
            removeInverseMapping(oIxValueOld, oKey);
            // make sure the entry is no longer tracked as excluded
            updateExcludedKeys(entry, false);
            }
        }

    /**
    * Add a new mapping from the given indexed value to the given key in the
    * inverse index.
    *
    * @param oIxValue  the indexed value (serves as a key in the inverse index)
    * @param oKey      the key to insert into the inverse index
    *
    * @return an already existing reference that is "equal" to the specified
    *         value (if available)
    */
    protected Object addInverseMapping(Object oIxValue, Object oKey)
        {
        Map mapInverse = m_mapInverse;

        // add a new mapping(s) to the inverse index
        // if the extracted value has been already "known", substitute
        // the one we just extracted with a previously extracted one
        // to avoid having multiple copies of equivalent values
        if (m_fSplitCollection && oIxValue instanceof Collection
                || oIxValue instanceof Object[])
            {
            oIxValue = addInverseCollectionMapping(mapInverse, oIxValue, oKey);
            }
        else
            {
            oIxValue = addInverseMapping(mapInverse, oIxValue, oKey);
            }

        return oIxValue;
        }

    /**
    * Add a new mapping from the given indexed value to the given key in the
    * supplied index.
    *
    * @param mapIndex  the index to which to add the mapping
    * @param oIxValue  the indexed value (serves as a key in the inverse index)
    * @param oKey      the key to insert into the inverse index
    *
    * @return an already existing reference that is "equal" to the specified
    *         value (if available)
    */
    protected Object addInverseMapping(Map<Object, Set<Object>> mapIndex, Object oIxValue, Object oKey)
        {
        SimpleHolder holder = new SimpleHolder(oIxValue);

        mapIndex.compute(oIxValue, (key, setKeys) ->
            {
            Object oExtracted = null;
            if (setKeys == null)
                {
                oExtracted = key;
                setKeys = instantiateSet();
                }
            else
                {
                // update holder with the existing key reference
                holder.set(key);
                }

            setKeys.add(oKey);
            onMappingAdded(oExtracted, setKeys.size());

            return setKeys;
            });

        return holder.get();
        }

    /**
    * Add new mappings from the elements of the given value to the given key
    * in the supplied index.  The given value is expected to be either a
    * Collection or an Object array.
    *
    * @param mapIndex  the index to which to add the mapping
    * @param oIxValue  the indexed Collection value (each element serves
    *                  as a key in the inverse index)
    * @param oKey      the key to insert into the inverse index
    *
    * @return an already existing reference that is "equal" to the specified
    *         value (if available)
    */
    protected Object addInverseCollectionMapping(Map mapIndex, Object oIxValue,
            Object oKey)
        {
        // in addition to adding the reverse index we want to find any entry
        // in the forward index that is equal to the currently extracted
        // collection (oIxValue); naturally that match, if exists, could only
        // be a forward index value for some of the keys that "match" all the
        // values in the passed in collection

        // search for an existing value in forward map for memory optimization
        Set       setCandidateKeys = null;
        boolean   fCandidate       = m_fOptimizeMV && m_mapForward != null;
        boolean   fScanForward     = false; // search by scanning the forward index
        int       cCost            = 0;     // cost factor for retainAll and new LiteSet operation
        final int THRESHOLD        = 500;   // hard-coded threshold to scan forward map

        Iterator iterator = oIxValue instanceof Object[] ?
                new SimpleEnumerator((Object[]) oIxValue) :
                ((Collection) oIxValue).iterator();

        while (iterator.hasNext())
            {
            Object oValue     = iterator.next();
            Object oExtracted = null;
            Set    setKeys    = (Set) mapIndex.get(oValue);

            if (setKeys == null)
                {
                fCandidate = false;
                setKeys    = instantiateSet();
                oExtracted = oValue;
                mapIndex.put(oExtracted, setKeys);
                }
            else if (fCandidate && !fScanForward)
                {
                if (setCandidateKeys == null)
                    {
                    cCost = setKeys.size();
                    if (cCost <= THRESHOLD)
                        {
                        setCandidateKeys = new LiteSet(setKeys);
                        }
                    else
                        {
                        // for values that are associated with a large number of
                        // keys, the new LiteSet and retainAll operations are expensive;
                        // do a controlled scan through the forward index instead
                        fScanForward = true;
                        }
                    }
                else
                    {
                    // use an empirical cost estimate
                    cCost += 4 * Math.max(setCandidateKeys.size(), setKeys.size());
                    if (cCost <= THRESHOLD)
                        {
                        setCandidateKeys.retainAll(setKeys);
                        }
                    }
                }

            setKeys.add(oKey);
            onMappingAdded(oExtracted, setKeys.size());
            }

        if (fCandidate && (fScanForward || setCandidateKeys != null))
            {
            // find an existing reference in the forward index that is "equal"
            // to the given (collection or array) value

            // if we are scanning the forward index, the iterator holds the values
            // to check, otherwise it holds the keys whose values are checked
            int cSize = oIxValue instanceof Object[] ?
                    ((Object[]) oIxValue).length :
                    ((Collection) oIxValue).size();

            Iterator iter = fScanForward
                    ? m_mapForward.values().iterator()
                    : setCandidateKeys.iterator();

            for (int i = 0, c = 4 * THRESHOLD/(cSize+1); i < c && iter.hasNext(); i++)
                {
                Object oCandidateValue = fScanForward ? iter.next() : get(iter.next());
                if (Base.equalsDeep(oCandidateValue, oIxValue))
                    {
                    // found an "equal" reference
                    return oCandidateValue;
                    }
                }
            }

        // no match is found; the passed in value should be used for the
        // forward map
        return oIxValue;
        }

    /**
    * Remove the mapping from the given indexed value to the given key from
    * the inverse index.
    *
    * @param oIxValue   the indexed value, or NO_VALUE if unknown
    * @param oKey       the key
    * @param colIgnore  the Collection of values to ignore (exclude from removal), or null
    */
    protected void removeInverseMapping(Object oIxValue, Object oKey, Collection colIgnore)
        {
        Map mapInverse = m_mapInverse;
        if (oIxValue == NO_VALUE && !isKeyExcluded(oKey))
            {
            // the old value could not be obtained; must resort to a full-scan
            for (Iterator iter = mapInverse.keySet().iterator(); iter.hasNext(); )
                {
                removeInverseMapping(iter.next(), oKey, colIgnore);
                }
            }
        else if (oIxValue instanceof Collection && m_fSplitCollection)
            {
            for (Iterator iter = ((Collection) oIxValue).iterator();
                 iter.hasNext();)
                {
                Object oElemValue = iter.next();
                if (colIgnore == null || !colIgnore.contains(oElemValue))
                    {
                    removeInverseMapping(mapInverse, oElemValue, oKey);
                    }
                }
            }
        else if (oIxValue instanceof Object[])
            {
            Object[] aoIxValueOld = (Object[]) oIxValue;

            for (int i = 0, c = aoIxValueOld.length; i < c; ++i)
                {
                Object oElemValue = aoIxValueOld[i];
                if (colIgnore == null || !colIgnore.contains(oElemValue))
                    {
                    removeInverseMapping(mapInverse, oElemValue, oKey);
                    }
                }
            }
        else
            {
            removeInverseMapping(mapInverse, oIxValue, oKey);
            }
        }

    /**
    * Remove the mapping from the given indexed value to the given key from
    * the inverse index.
    *
    * @param oIxValue  the indexed value, or NO_VALUE if unknown
    * @param oKey      the key
    */
    protected void removeInverseMapping(Object oIxValue, Object oKey)
        {
        removeInverseMapping(oIxValue, oKey, null);
        }

    /**
    * Remove the mapping from the given indexed value to the given key from
    * the supplied index.
    *
    * @param mapIndex  the index from which to remove the mapping
    * @param oIxValue  the indexed value
    * @param oKey      the key
    */
    protected void removeInverseMapping(Map mapIndex, Object oIxValue, Object oKey)
        {
        Set setKeys = (Set) mapIndex.get(oIxValue);
        if (setKeys == null)
            {
            // there is a legitimate scenario when the inverse index may miss an entry -
            // if the entry was evicted during the "build index" phase, in which case oIxValue would be null;
            // while there is a possibility that a "null" value is extracted from an existing entry,
            // we will ignore that possibility for the [debug] message below
            if (!isPartial() && oIxValue != null)
                {
                logMissingIdx(oIxValue, oKey);
                }
            }
        else
            {
            Object oExtracted = null;
            setKeys.remove(oKey);

            if (setKeys.isEmpty())
                {
                oExtracted = oIxValue;
                mapIndex.remove(oExtracted);
                }

            onMappingRemoved(oExtracted);
            }
        }

    /**
    * Log messages for missing inverse index.
    *
    * @param oIxValue  the indexed value
    * @param oKey      the key
    */
    protected void logMissingIdx(Object oIxValue, Object oKey)
        {
        // COH-5939: limit logging frequency to 10 messages for every 5 minutes interval
        long ldtNow       = getSafeTimeMillis();
        long ldtLogResume = m_ldtLogMissingIdx + 300000L;

        if (ldtNow > ldtLogResume)
            {
            m_ldtLogMissingIdx = ldtNow;
            m_cLogMissingIdx   = 0;
            }

        int cLog = ++m_cLogMissingIdx;
        if (cLog < 10)
            {
            log("Missing inverse index: value=" + oIxValue + ", key=" + oKey);
            }
        else if (cLog == 10)
            {
            log("Suppressing missing inverse index messages for " +
                    (ldtLogResume - ldtNow) / 1000 + " seconds");
            }
        }

    /**
    * Given that the old value is known to be a Collection or an array,
    * collect all the enclosed elements that are not part of the new value.
    *
    * @param oIxValueOld  the old value (must be a collection or an array)
    * @param oIxValueNew  the new value
    *
    * @return the set of values that are contained in the old collection or array,
    *         but not part of the new value
    */
    protected Set collectRemoved(Object oIxValueOld, Object oIxValueNew)
        {
        Set setRemove;
        if (oIxValueOld instanceof Collection)
            {
            // clone the original collection
            setRemove = new HashSet((Collection) oIxValueOld);
            }
        else // oIxValueOld instanceof Object[]
            {
            setRemove = new HashSet(Arrays.asList((Object[]) oIxValueOld));
            }

        setRemove.removeAll(ensureCollection(oIxValueNew));
        return setRemove;
        }

    /**
    * Factory method used to create a new set containing the keys associated
    * with a single value.
    *
    * @return a Set to keep the corresponding keys at
    */
    protected Set instantiateSet()
        {
        return new InflatableSet();
        }

    /**
    * Factory method used to create a new calculator.
    *
    * @return a {@link UnitCalculator}
    */
    protected UnitCalculator instantiateCalculator()
        {
        return new IndexCalculator(m_ctx, this);
        }

    /**
    * Decrease the size of the index by the size of an index map entry.
    */
    protected void onMappingRemoved()
        {
        onMappingRemoved(null);
        }

    /**
    * Decrease the size of the index by the estimated size of the specified
    * removed value.
    *
    * @param oValue  the value being removed from the index
    */
    protected void onMappingRemoved(Object oValue)
        {
        IndexCalculator calc = (IndexCalculator) getCalculator();

        int cb = calc.getEntrySize() +
                (oValue == null
                    ? 0
                    : calc.calculateUnits(null, oValue) +
                        IndexCalculator.SET_OVERHEAD + IndexCalculator.INFLATION_OVERHEAD);
        setUnits(getUnits() - cb);
        }

    /**
    * Increase the size of the index by the size of an index map entry.
    */
    protected void onMappingAdded()
        {
        onMappingAdded(null, 0);
        }

    /**
    * Increase the size of the index by the estimated size of the specified
    * added value.
    *
    * @param oValue the value being added to the index
    * @param cSize  the current key set size indexed by the given value
    */
    protected void onMappingAdded(Object oValue, int cSize)
        {
        IndexCalculator calc = (IndexCalculator) getCalculator();

        int cb = calc.getEntrySize() +
                (oValue == null ? 0 : calc.calculateUnits(null, oValue) + IndexCalculator.SET_OVERHEAD) +
                (cSize == 2 ? IndexCalculator.INFLATION_OVERHEAD : 0);

        setUnits(getUnits() + cb);
        }

    /**
    * Check the entry against the set of entries not included in the index and
    * update the set if necessary.
    *
    * @param entry    the entry to be checked
    * @param fExclude true iff the insert or update of the entry into the index
    *                 caused an exception
    */
    protected void updateExcludedKeys(Map.Entry entry, boolean fExclude)
        {
        Set setExcluded = m_setKeyExcluded;
        if (fExclude || !setExcluded.isEmpty())
            {
            Object oKey = entry.getKey();
            if (fExclude)
                {
                setExcluded.add(oKey);
                }
            else
                {
                setExcluded.remove(oKey);
                }
            }
        }

    /**
    * Check if the entry with the given key is excluded from the index.
    *
    * @param oKey  the key to test
    * @return true if the key is in the list of keys currently excluded
    *         from the index, false if the entry with the key is in the index
    */
    protected boolean isKeyExcluded(Object oKey)
        {
        return m_setKeyExcluded.contains(oKey);
        }

    // ----- Object interface -----------------------------------------------

    /**
    * Returns a string representation of this SimpleMapIndex.
    *
    * @return a String representation of this SimpleMapIndex
    */
    public String toString()
        {
        return toString(false);
        }

    /**
    * Returns a string representation of this SimpleMapIndex.  If called in
    * verbose mode, include the contents of the index (the inverse
    * index). Otherwise, just print the number of entries in the index.
    *
    * @param fVerbose  if true then print the content of the index otherwise
    *                  print the number of entries
    *
    * @return a String representation of this SimpleMapIndex
    */
    public String toString(boolean fVerbose)
        {
        return ClassHelper.getSimpleName(getClass())
                + ": Extractor=" + getValueExtractor()
                + ", Ordered=" + isOrdered()
                + ", Footprint=" + Base.toMemorySizeString(getUnits(), false)
                + ", Content="
                + (fVerbose ? getIndexContents().keySet() : getIndexContents().size());
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
        if (!(o instanceof SimpleMapIndex))
            {
            return false;
            }

        SimpleMapIndex that = (SimpleMapIndex) o;
        return equals(this.getComparator(),     that.getComparator()) &&
               equals(this.getValueExtractor(), that.getValueExtractor()) &&
                      this.isOrdered()       == that.isOrdered();
        }

    /**
    * Returns the hash code value for this MapIndex.
    *
    * @return the hash code value for this MapIndex
    */
    public int hashCode()
        {
        return m_comparator == null ? 0 : m_comparator.hashCode() +
               m_extractor.hashCode() + (m_fOrdered ? 1 : 0);
        }


    // ----- inner class: IndexCalculator -----------------------------------

    /**
    * A stateful {@link
    * com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator calculator}
    * used to calculate the cost of a homogeneous index (holding all values of a
    * single type).
    */
    public static class IndexCalculator
            extends SimpleMemoryCalculator
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct an IndexCalculator which allows for conversion of items into
        * a serialized format. The calculator may use the size of the serialized
        * value representation to approximate the size of indexed values.
        * <p>
        * Note: {@link ExternalizableHelper#CONVERTER_TO_BINARY} can be used to
        *       convert Serializable or ExternalizableLite objects
        *
        * @param ctx   the {@link BackingMapContext} associated with the indexed cache
        * @param index the container index for this calculator (used only for logging)
        */
        public IndexCalculator(BackingMapContext ctx, SimpleMapIndex index)
            {
            m_index = index;

            if (ctx == null)
                {
                m_converter  = null;
                m_calculator = null;
                return;
                }
            m_converter = ctx.getManagerContext().getValueToInternalConverter();
            Map mapBack = ctx.getBackingMap();

            if (mapBack instanceof ReadWriteBackingMap)
                {
                mapBack = ((ReadWriteBackingMap) mapBack).getInternalCache();
                }

            if (mapBack instanceof ConfigurableCacheMap)
                {
                m_calculator = ((ConfigurableCacheMap) mapBack).getUnitCalculator();
                }
            else
                {
                m_calculator = null;
                }
            }

        // ----- IndexCalculator methods ------------------------------------

        /**
        * Determine which method to use to count the size of an instance of the
        * specified class.
        *
        * @param clz  the type of objects that this calculator will operate on
        *
        * @return the {@link CalculatorState}
        */
        protected CalculatorState getCalculatorState(Class clz)
            {
            if (MAP_FIXED_SIZES.containsKey(clz) || clz.isEnum())
                {
                return CalculatorState.FIXED;
                }
            if (clz == String.class || clz == Binary.class)
                {
                return CalculatorState.STANDARD;
                }
            if (clz.isArray())
                {
                // check if the component type is either FIXED or STANDARD.
                // If so, the standard calculator can count the size
                switch (getCalculatorState(clz.getComponentType()))
                    {
                    case FIXED:
                    case STANDARD:
                        return CalculatorState.STANDARD;

                    default:
                        break;
                    }
                }

            // the calculator cannot calculate the size
            return CalculatorState.UNKNOWN;
            }

        /**
        * Initialize the calculator based on the type of the specified object
        * assuming that the indexed values are homogeneous (of the same type).
        *
        * @param o  an instance of the value to calculate the size for
        *
        * @return the new {@link CalculatorState}
        */
        protected synchronized CalculatorState initialize(Object o)
            {
            // Note: this method is synchronized to protect the against possible
            //       concurrent initialization on multiple threads
            UnitCalculator calculator = m_calculator;
            if (calculator != null)
                {
                try
                    {
                    calculator.calculateUnits(null, o);
                    return m_state = CalculatorState.CONFIGURED;
                    }
                catch (IllegalArgumentException e)
                    {
                    // the cache calculator does not work for the index
                    // (most likely it's just a BinaryCalculator)
                    }
                }

            CalculatorState state = m_state;
            if (state == CalculatorState.UNINITIALIZED)
                {
                state = getCalculatorState(o.getClass());
                switch (state)
                    {
                    case UNKNOWN:
                        {
                        String sMsg;
                        if (calculator == null)
                            {
                            sMsg = "There is no configured calculator "
                                + "for " + o.getClass().getCanonicalName()
                                + "; this " + m_index + " will"
                                + " estimate the index size using serialization,"
                                + " which could impact its performance";
                            }
                        else
                            {
                            sMsg = "The configured calculator "
                                + calculator.getClass().getCanonicalName()
                                + " cannot be used to estimate the size of "
                                + o.getClass().getCanonicalName()
                                + "; this " + m_index + " will"
                                + " estimate the index size using serialization,"
                                + " which could impact its performance";
                            }

                        long ldtNow = Base.getLastSafeTimeMillis();

                        // Enh 35977966: only log a message every 60 seconds to minimize output due to above messages
                        // displaying for each cache entry and overwhelming the log files with messages
                        if (ldtNow > s_ldtLastIndexLogTime + 60_000L)
                            {
                            Logger.info(sMsg);
                            s_ldtLastIndexLogTime = ldtNow;
                            }

                        m_state = state;
                        break;
                        }

                    case FIXED:
                        m_cbFixed = super.sizeOf(o);
                        // fall-through
                    default:
                        m_state = state;
                        break;
                    }
                }
            return state;
            }

        // ----- SimpleMemoryCalculator methods -----------------------------

        /**
        * {@inheritDoc}
        */
        protected int getEntrySize()
            {
            return m_index.isOrdered() ? SORTED_ENTRY_OVERHEAD : ENTRY_OVERHEAD;
            }

        /**
        * {@inheritDoc}
        */
        public int sizeOf(Object o)
            {
            try
                {
                switch (m_state)
                    {
                    case UNINITIALIZED:
                        initialize(o);
                        return sizeOf(o);

                    case FIXED:
                        return m_cbFixed;

                    case STANDARD:
                        return super.sizeOf(o);

                    case CONFIGURED:
                        return m_calculator.calculateUnits(null, o);

                    case UNKNOWN:
                        Converter conv = getConverter();
                        return conv == null
                            ? DEFAULT_SIZE
                            : super.sizeOf(conv.convert(o));

                    default:
                        throw new IllegalStateException();
                    }
                }
            catch (Exception e)
                {
                // something went wrong with either conversion or the index
                // was thought to be STANDARD, but appears to be not
                // homogeneous
                return DEFAULT_SIZE;
                }
            }

        /**
        * Return the converter used by this IndexCalculator, or null.
        *
        * @return the converter used by this IndexCalculator
        */
        protected Converter getConverter()
            {
            return m_converter;
            }

        // ----- enum: CalculatorState --------------------------------------

        /**
        * The CalculatorState identifies the method used by the calculator to
        * calculate the cost of a key or a value type. There are four states:
        * <ul>
        * <li>UNINITIALIZED - The calculator has not yet been initialized;
        * <li>UNKNOWN - The calculator is unable to determine the exact size for
        *     this type. It will either use a pluggable calculator or an
        *     approximation based on the serialized size. The {@link
        *     IndexCalculator#DEFAULT_SIZE} will be used as a fallback;
        * <li>FIXED - instances of the key or value will always be of the same
        *     size (e.g. int, short, double).
        * <li>STANDARD - instances of the key or value type need to be
        *     calculated by the unit calculator (e.g. Binary, String).
        * <li>CONFIGURED - The configured calculator on the cache where the
        *     index is defined will be used to calculate the size.
        * </ul>
        */
        public static enum CalculatorState
            {
            UNINITIALIZED, UNKNOWN, FIXED, STANDARD, CONFIGURED
            }

        // ----- data members and constants ---------------------------------

        /**
        * The container index. Since we use an enum for the state we have to
        * make this inner class a static one and pass the parent index into
        * the constructor.
        */
        protected SimpleMapIndex m_index;

        /**
        * The {@link CalculatorState} of this calculator.
        */
        protected CalculatorState m_state = CalculatorState.UNINITIALIZED;

        /**
        * If the state is {@link CalculatorState#FIXED}, contains a cached cost
        * per instance.
        */
        protected int m_cbFixed;

        /**
        * The {@link Converter} used to convert {@link CalculatorState#UNKNOWN}
        * types into {@link Binary} to approximate the size.
        */
        protected final Converter m_converter;

        /**
        * The {@link UnitCalculator} used to calculate the size of {@link CalculatorState#CONFIGURED}
        * types. into {@link Binary} to approximate the size.
        * The calculator is initialized iff the {@link BackingMapContext} associated
        * with the index represents a {@link ConfigurableCacheMap}
        */
        protected final UnitCalculator m_calculator;

        /**
        * The memory cost of a NullableConcurrentMap used as the Forward Index.
        */
        protected static final int MAP_OVERHEAD = calculateShallowSize(NullableConcurrentMap.class) +
                                                  calculateShallowSize(ConcurrentHashMap.class) +
                                                  padMemorySize(SIZE_BASIC_OBJECT + 4);

        /**
        * The memory cost of creating NullableConcurrentMap.Entry.
        */
        protected static final int ENTRY_OVERHEAD = padMemorySize(SIZE_OBJECT_REF + 4) +
                                                    40; // ConcurrentSkipListMap$Node

        /**
        * The memory cost of creating NullableSortedMap.Entry.
        */
        protected static final int SORTED_ENTRY_OVERHEAD = padMemorySize(SIZE_OBJECT_REF + 4) +
                                                           40; // ConcurrentSkipListMap$Node

        /**
        * The memory cost of creating an InflatableSet.
        * <p>
        * This cost does not include post inflation of the set; INFLATION_OVERHEAD
        * should be considered as a one-off memory cost to be added post inflation.
        */
        protected static final int SET_OVERHEAD = calculateShallowSize(InflatableSet.class);

        /**
        * The memory cost of inflating an InflatableSet. The inflation can be determined
        * when its size grows from 1 to 2, at which point INFLATION_OVERHEAD should
        * be added to any models predicting memory usage.
        */
        protected static final int INFLATION_OVERHEAD = calculateShallowSize(SafeHashMap.class) +
                                                          calculateShallowSize(InflatableSet.class) + SIZE_OBJECT +
                                                          padMemorySize(SIZE_BASIC_OBJECT + 4);

        /**
        * The default size of an entry used if the calculator is unable to
        * calculate the size.
        */
        protected static final int DEFAULT_SIZE = 32;
        }

    // ----- data members ---------------------------------------------------

    /**
    * ValueExtractor object that this MapIndex uses to extract an indexable
    * property value from a [converted] value stored in the resource map.
    */
    protected ValueExtractor m_extractor;

    /**
    * Comparator used to sort the index. Used iff the Ordered flag is true.
    * Null implies a natural order.
    */
    protected Comparator m_comparator;

    /**
    * Specifies whether or not this MapIndex orders the contents of the
    * indexed information.
    */
    protected boolean m_fOrdered;

    /**
    * Map that contains the index values (forward index). The keys of the Map
    * are the keys to the map being indexed and the values are the extracted
    * values. This map is used by the IndexAwareComparators to avoid a
    * conversion and value extraction steps.
    */
    protected Map m_mapForward;

    /**
    * For an indexed value that is a multi-value (Collection or array) this flag
    * specifies whether an attempt should be made to search the forward map
    * for an existing reference that is "equal" to the specified mutli-value
    * and use it instead (if available) to reduce the index memory footprint.
    * <p>
    * Note, that even if this optimization is allowed, the full search could be
    * quite expensive and our algorithm will always limit the number of cycles
    * it spends on the search.
    */
    private boolean m_fOptimizeMV = true;

    /**
    * Map that contains the index contents (inverse index). The keys of the
    * Map are the return values from the ValueExtractor operating against the
    * entries of the resource map, and for each key, the corresponding value
    * stored in the Map is a Set of keys to the resource map.
    */
    protected Map m_mapInverse;

    /**
    * If a value extracted by the ValueExtractor is a Collection, this
    * property specifies whether or not it should be treated as a Collection
    * of contained attributes or indexed as a single composite attribute.
    */
    protected boolean m_fSplitCollection;

    /**
    * The size footprint of the index, in units (as defined by the UnitCalculator).
    */
    protected volatile long m_cUnits;

    /**
    * UnitCalculator used to estimate the cost of a value.
    */
    protected UnitCalculator m_calculator;

    /**
    * The {@link BackingMapContext context} associated with this index.
    */
    protected BackingMapContext m_ctx;

    /**
    * The time at which the most recent logging of "missing inverse index"
    * messages started.
    */
    protected long m_ldtLogMissingIdx;

    /**
    * The number of "missing inverse index" messages that have been logged.
    */
    protected int m_cLogMissingIdx;

    /**
    * A set of keys for the entries, which could not be included in the index.
    */
    protected Set m_setKeyExcluded;

    /**
    * Specifies whether or not this MapIndex supports a forward index.
    */
    protected boolean m_fForwardIndex;

    /**
    * Specifies whether or not the index is based on the immutable values (e.g. keys).
    */
    protected boolean m_fImmutableValues;

    /**
     * Used to minimize logging of index error message.
     */
    private static long s_ldtLastIndexLogTime = 0;
    }
