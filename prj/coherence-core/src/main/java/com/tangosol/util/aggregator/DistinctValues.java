/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteSet;
import com.tangosol.util.MapIndex;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.Streamer;
import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
* Return the set of unique values extracted from a set of entries in a Map.
* If the set of entries is empty, an empty set is returned.
* <p>
* This aggregator could be used in combination with
* {@link com.tangosol.util.extractor.MultiExtractor MultiExtractor} allowing
* to collect all unique combinations (tuples) of a given set of attributes.
* <p>
* The DistinctValues aggregator covers a simple case of a more generic
* aggregation pattern implemented by the {@link GroupAggregator}, which in
* addition to collecting all distinct values or tuples, runs an aggregation
* against each distinct entry set (group).
*
* @param <T>  the type of the value to extract from
* @param <E>  the type of the extracted value
*
* @author jh  2005.12.20
*/
public class DistinctValues<K, V, T, E>
        extends AbstractAggregator<K, V, T, E, Collection<E>> // Collection is used instead of Set
                                                              // because of POF restrictions
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public DistinctValues()
        {
        super();
        }

    /**
    * Construct a DistinctValues aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object
    *
    */
    public DistinctValues(ValueExtractor<? super T, ? extends E> extractor)
        {
        super(extractor);
        }

    /**
    * Construct an DistinctValues aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object
    */
    public DistinctValues(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, Object, Collection<E>> supply()
        {
        return new DistinctValues<>(getValueExtractor());
        }

    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        ensureInitialized(false);

        if (streamer.isAllInclusive())
            {
            if (streamer.hasNext())
                {
                InvocableMap.Entry<? extends K, ? extends V> entry = streamer.next();
                if (entry instanceof BinaryEntry)
                    {
                    BinaryEntry binEntry = (BinaryEntry) entry;
                    MapIndex    index    = binEntry.getBackingMapContext().getIndexMap().get(getValueExtractor());
                    if (index != null && !index.isPartial())
                        {
                        Map mapContents = index.getIndexContents();
                        if (mapContents != null && !mapContents.isEmpty())
                            {
                            Set<E> set = ensureSet();
                            Map<E, Integer> mapCounts = ensureCounts();
                            for (Object oEntry : mapContents.entrySet())
                                {
                                Map.Entry entryIndex = (Map.Entry) oEntry;
                                E         value      = (E) entryIndex.getKey();
                                if (value != null)
                                    {
                                    set.add(value);
                                    mapCounts.put(value,
                                            Integer.valueOf(((Collection) entryIndex.getValue()).size()));
                                    }
                                }
                            return false;
                            }
                        }
                    }

                // consume the first entry and proceed to the default implementation
                super.accumulate(entry);
                }
            else
                {
                m_set       = new LiteSet<>();
                m_mapCounts = new HashMap<>();
                return false;
                }
            }

        return super.accumulate(streamer);
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY | CONTINUOUS | STATE_CHECKPOINTABLE;
        }

    @Override
    public Object snapshotState()
        {
        ensureInitialized(false);
        return new HashMap<>(ensureCounts());
        }

    @Override
    public void restoreState(Object oState)
        {
        if (!(oState instanceof Map))
            {
            throw new IllegalArgumentException("Expected a frequency map");
            }

        Map<E, Integer> mapCounts = new HashMap<>();
        for (Object oEntry : ((Map) oState).entrySet())
            {
            Map.Entry entry = (Map.Entry) oEntry;
            Object    count = entry.getValue();
            if (!(count instanceof Number) || ((Number) count).intValue() <= 0)
                {
                throw new IllegalArgumentException("Invalid distinct value frequency: " + count);
                }
            mapCounts.put((E) entry.getKey(), Integer.valueOf(((Number) count).intValue()));
            }

        ensureInitialized(false);
        m_mapCounts = mapCounts;
        m_set       = new LiteSet<>(mapCounts.keySet());
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        m_set       = null;
        m_mapCounts = fFinal ? null : new HashMap<>();
        }

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            if (fFinal)
                {
                // aggregate partial results
                Collection colPartial = (Collection) o;
                if (!colPartial.isEmpty())
                    {
                    ensureSet().addAll(colPartial);
                    }
                }
            else
                {
                // collect partial results
                ensureSet().add((E) o);
                Map<E, Integer> mapCounts = ensureCounts();
                E               value     = (E) o;
                mapCounts.put(value, Integer.valueOf(mapCounts.getOrDefault(value, 0) + 1));
                }
            }
        }

    @Override
    protected RetractionResult remove(Object o)
        {
        if (o == null)
            {
            return RetractionResult.UPDATED;
            }

        Map<E, Integer> mapCounts = m_mapCounts;
        if (mapCounts == null)
            {
            return RetractionResult.REBUILD_REQUIRED;
            }

        E       value = (E) o;
        Integer count = mapCounts.get(value);
        if (count == null || count == 0)
            {
            return RetractionResult.REBUILD_REQUIRED;
            }
        if (count == 1)
            {
            mapCounts.remove(value);
            ensureSet().remove(value);
            }
        else
            {
            mapCounts.put(value, Integer.valueOf(count - 1));
            }
        return RetractionResult.UPDATED;
        }

    /**
    * {@inheritDoc}
    */
    protected Set<E> finalizeResult(boolean fFinal)
        {
        Set<E> set = m_set;

        if (fFinal)
            {
            m_set       = null;  // COH-1487
            m_mapCounts = null;
            return set == null ? NullImplementation.getSet() : set;
            }

        return set == null ? null : new LiteSet<>(set);
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Return a set that can be used to store distinct values, creating it if
    * one has not already been created.
    *
    * @return a set that can be used to store distinct values
    */
    protected Set<E> ensureSet()
        {
        Set<E> set = m_set;
        if (set == null)
            {
            set = m_set = new LiteSet<>();
            }
        return set;
        }

    /**
    * Return the frequency map for maintained values.
    *
    * @return the value frequency map
    */
    protected Map<E, Integer> ensureCounts()
        {
        Map<E, Integer> mapCounts = m_mapCounts;
        if (mapCounts == null)
            {
            mapCounts = m_mapCounts = new HashMap<>();
            }
        return mapCounts;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The resulting set of distinct values.
    */
    protected transient Set<E> m_set;

    /**
    * The number of current entries contributing each distinct value.
    */
    protected transient Map<E, Integer> m_mapCounts;
    }
