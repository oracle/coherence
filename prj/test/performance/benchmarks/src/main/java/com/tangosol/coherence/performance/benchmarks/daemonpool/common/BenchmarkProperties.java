/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System-property save and restore helper for daemon-pool benchmarks.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public final class BenchmarkProperties
    {
    private BenchmarkProperties()
        {
        }

    public static Map<String, String> capture(String... asProperty)
        {
        Map<String, String> mapPrevious = new LinkedHashMap<>();
        for (String sProperty : asProperty)
            {
            mapPrevious.put(sProperty, System.getProperty(sProperty));
            }
        return mapPrevious;
        }

    public static void restore(Map<String, String> mapPrevious)
        {
        if (mapPrevious == null)
            {
            return;
            }

        for (Map.Entry<String, String> entry : mapPrevious.entrySet())
            {
            String sProperty = entry.getKey();
            String sValue    = entry.getValue();
            if (sValue == null)
                {
                System.clearProperty(sProperty);
                }
            else
                {
                System.setProperty(sProperty, sValue);
                }
            }
        }
    }
