/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import java.util.Map;

import java.util.function.BiConsumer;

import java.util.stream.Collector;

import javax.management.openmbean.CompositeData;

/**
 * A {@link Collector} that understands how to interrogate {@link CompositeData}
 * types.
 *
 * @author sr 11.3.17
 * @since 12.2.1.4.0
 */
public class CompositeDataCollector<T extends Map<String, Object>>
        extends AbstractMultiCollector
    {
    // ----- Collector interface --------------------------------------------

    @Override
    public BiConsumer<T, CompositeData> accumulator()
        {
        return (map, compositeData) ->
            {
            for (String sKey : compositeData.getCompositeType().keySet())
                {
                Object    oValue    = compositeData.get(sKey);
                Collector collector = (Collector) f_mapCollector.get(sKey);
                if (collector == null)
                    {
                    collector = MBeanCollectorFunction.createTypeBasedCollector(oValue);

                    if (collector != null)
                        {
                        f_mapCollector.put(sKey, collector);
                        map.put(sKey, collector.supplier().get());
                        }
                    }

                collector.accumulator().accept(map.get(sKey), oValue);
                }
            };
        }
    }
