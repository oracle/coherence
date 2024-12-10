/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.net.CacheFactory;

import java.util.Map;

import java.util.function.BiConsumer;

import java.util.stream.Collector;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;

/**
 * A {@link Collector} that understands how to interrogate {@link TabularData}
 * types.
 *
 * @author sr  11.3.17
 * @since 12.2.1.4.0
 */
public class TabularDataCollector<T extends Map<String, Object>>
        extends AbstractMultiCollector
    {
    // ----- Collector interface --------------------------------------------

    @Override
    public BiConsumer<T, TabularData> accumulator()
        {
        return (map, tabularData) ->
            {
            CompositeType rowType = tabularData.getTabularType().getRowType();

            // for composite types in MBeans, each row will have a key attribute and will be a simple type
            if (rowType.containsKey(KEY) &&
                    rowType.containsKey(VALUE) &&
                    rowType.getType(KEY) instanceof SimpleType)
                {

                for (Object objValue : tabularData.values())
                    {
                    String sKey   = ((CompositeData) objValue).get(KEY).toString();
                    Object oValue = ((CompositeData) objValue).get(VALUE);

                    Collector collector = (Collector) f_mapCollector.get(sKey);

                    if (collector == null)
                        {
                        collector = MBeanCollectorFunction.createTypeBasedCollector(oValue);
                        f_mapCollector.put(sKey, collector);
                        map.put(sKey, collector.supplier().get());
                        }

                    collector.accumulator().accept(map.get(sKey), oValue);
                    }
                }
            };
        }

    // ----- constants ------------------------------------------------------

    /**
     * The key of a CompositeType.
     */
    protected static String KEY = "key";

    /**
     * The value of a CompositeType.
     */
    protected static String VALUE = "value";
    }
