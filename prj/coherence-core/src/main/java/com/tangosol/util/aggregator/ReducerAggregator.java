/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ValueExtractor;

import com.tangosol.net.cache.CacheMap;

import java.util.Collection;
import java.util.Map;


/**
* The ReducerAggregator is used to implement functionality similar to
* {@link CacheMap#getAll(Collection)} API.  Instead of returning the complete
* set of values, it will return a portion of value attributes based on the
* provided {@link ValueExtractor}.
* <p>
* This aggregator could be used in combination with
* {@link com.tangosol.util.extractor.MultiExtractor MultiExtractor} allowing one
* to collect tuples that are a subset of the attributes of each object stored in
* the cache.
*
* @param <K>  the type of the Map entry keys
* @param <V>  the type of the Map entry values
* @param <T>  the type of the value to extract from
* @param <E>  the type of the extracted value
*
* @author djl  2009.03.02
*/
public class ReducerAggregator<K, V, T, E>
        extends AbstractAggregator<K, V, T, E, Map<K, E>>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ReducerAggregator()
        {
        super();
        }

    /**
     * Construct a ReducerAggregator based on the specified method name.
     *
     * @param sMethod  the name of the method that is used to extract the
     *                 portion of the cached value
     */
    public ReducerAggregator(String sMethod)
        {
        super(sMethod);
        }

    /**
    * Construct a ReducerAggregator based on the specified extractor.
    *
    * @param extractor  the extractor that is used to extract the portion
    *                   of the cached value
    */
    public ReducerAggregator(ValueExtractor<? super T, ? extends E> extractor)
        {
        super(extractor);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, Object, Map<K, E>> supply()
        {
        return new ReducerAggregator<>(getValueExtractor());
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
        Map map = m_map;
        if (map != null)
            {
            map.clear();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void processEntry(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        // collect partial results in a transient map
        ensureMap().put(entry.getKey(), entry.extract(getValueExtractor()));
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
                Map map = (Map) o;
                if (!map.isEmpty())
                    {
                    ensureMap().putAll(map);
                    }
                }
            else
                {
                // should not be called with fFinal == false
                throw new IllegalStateException();
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    protected Map<K, E> finalizeResult(boolean fFinal)
        {
        Map<K, E> map = m_map;

        m_map = null;  // COH-1487

        return map == null
                ? fFinal ? NullImplementation.getMap() : null
                : map;
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Return a map that can be used to store reduced values, creating it if
    * one has not already been created.
    *
    * @return a set that can be used to store distinct values
    */
    protected Map<K, E> ensureMap()
        {
        Map<K, E> map = m_map;
        if (map == null)
            {
            map = m_map = new LiteMap<>();
            }
        return map;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The resulting map of reduced values.
    */
    protected transient Map<K, E> m_map;
    }
