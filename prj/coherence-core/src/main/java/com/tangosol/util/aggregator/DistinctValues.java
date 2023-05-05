/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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
import java.util.Collections;
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
                            m_set = Collections.unmodifiableSet(mapContents.keySet());
                            return false;
                            }
                        }
                    }

                // consume the first entry and proceed to the default implementation
                super.accumulate(entry);
                }
            else
                {
                m_set = Collections.emptySet();
                return false;
                }
            }

        return super.accumulate(streamer);
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY;
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        Set<E> set = m_set;
        if (set != null)
            {
            set.clear();
            }
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
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    protected Set<E> finalizeResult(boolean fFinal)
        {
        Set<E> set = m_set;

        m_set = null;  // COH-1487

        return set == null
                ? fFinal ? NullImplementation.getSet() : null
                : set;
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


    // ----- data members ---------------------------------------------------

    /**
    * The resulting set of distinct values.
    */
    protected transient Set<E> m_set;
    }
