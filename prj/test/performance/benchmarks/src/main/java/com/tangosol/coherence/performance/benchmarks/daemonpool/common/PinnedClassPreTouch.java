/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

/**
 * Diagnostic-only pre-touch helper for classes observed in warmup-window pin
 * stacks.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public final class PinnedClassPreTouch
    {
    private PinnedClassPreTouch()
        {
        }

    public static void preTouch()
        {
        for (String sClass : PRETOUCH_CLASSES)
            {
            try
                {
                Class.forName(sClass, true, Thread.currentThread().getContextClassLoader());
                }
            catch (ClassNotFoundException e)
                {
                throw new IllegalStateException("Unable to pre-touch " + sClass, e);
                }
            }
        }

    private static final String[] PRETOUCH_CLASSES =
        {
        "com.tangosol.internal.util.ConversionHelper",
        "com.tangosol.util.ConverterCollections",
        "com.oracle.coherence.common.collections.NullableSortedMap",
        "com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$InvocationContext"
        };
    }
