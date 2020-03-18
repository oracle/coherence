/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.Collector;

/**
 * An abstract class for {@link Collector collecting} map-style data structures;
 * specifically with key-value pairs in which the values need to be aggregated
 * independently.
 *
 * @author hr/sr  11.3.17
 * @since 12.2.1.4.0
 */
public abstract  class AbstractMultiCollector<T extends Map<String, Object>>
        implements Collector
    {
    // ----- Collector interface --------------------------------------------

    @Override
    public Supplier<T> supplier()
        {
        return () -> (T) new HashMap<String, Object>();
        }

    @Override
    public BinaryOperator<T> combiner()
        {
        return (mapResult1, mapResult2) ->
            {
            int cMap2 = mapResult2.size();
            if (cMap2 > mapResult1.size()) // iterate the larger map
                {
                cMap2 = mapResult1.size();

                Map mapTmp = mapResult1;
                mapResult1 = mapResult2;
                mapResult2 = (T) mapTmp;
                }

            Map<String, Object> mapResult = new HashMap<>(mapResult1.size());

            for (Map.Entry<String, Object> entry : mapResult1.entrySet())
                {
                String sAttribute = entry.getKey();
                Object oValue2    = null;
                if (mapResult2.containsKey(sAttribute))
                    {
                    oValue2 = mapResult2.get(sAttribute);
                    cMap2--;
                    }

                mapResult.put(sAttribute, ((Collector) f_mapCollector.get(sAttribute)).combiner().apply(
                        entry.getValue(), oValue2));
                }

            if (cMap2 > 0)
                {
                for (Map.Entry<String, Object> entry : mapResult2.entrySet())
                    {
                    if (!mapResult1.containsKey(entry.getKey()))
                        {
                        String sAttribute = entry.getKey();
                        mapResult.put(sAttribute, ((Collector) f_mapCollector.get(sAttribute)).combiner().apply(
                                null, entry.getValue()));
                        }
                    }
                }
            return (T) mapResult;
            };
        }

    @Override
    public Function<T, T> finisher()
        {
        return (mapResult) ->
        {
        for (Iterator<Map.Entry<String, Object>> iter = mapResult.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry<String, Object> entry = iter.next();

            String sAttribute = entry.getKey();
            Object oResult    = ((Collector)(f_mapCollector.get(sAttribute))).finisher().apply(entry.getValue());
                   oResult    = ATTRIBUTE_NULLIFIER.apply(oResult);

            // TODO: validate that type specific summary stats are serializable
            //       oResult    = ATTRIBUTE_SERIALIZABLE.apply(oResult);

            if (oResult == null)
                {
                iter.remove();
                }
            else
                {
                entry.setValue(oResult);
                }
            }
        return mapResult;
        };
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A Function that returns the provided object or null if the object
     * is one of the 'no value' defaults.
     */
     public static final Function<Object, Object> ATTRIBUTE_NULLIFIER = oValue ->
         {
         if (oValue instanceof Optional)
             {
             Optional optional = (Optional) oValue;
                      oValue   = optional.isPresent() ? optional.get() : null;
             }
         // account for Coherence MBean Attribute defaults
         return oValue instanceof Number && ((Number) oValue).intValue() == -1 ||
            oValue instanceof String && "n/a".equals(oValue)
                       ? null : oValue;
         };

    // ----- data members ---------------------------------------------------

    /**
     * The map of attribute name to collector.
     */
    protected Map f_mapCollector = new HashMap<String, Collector<Object, Object, Object>>();
    }
