/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.Serializable;

/**
 * CPU-only service-side workload used as a deliberate control where virtual
 * daemon-pool scheduling is not expected to provide a blocking-work advantage.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public class CpuBoundProcessor
        extends AbstractProcessor<Integer, Integer, Integer>
        implements Serializable
    {
    public CpuBoundProcessor(int cIterations)
        {
        m_cIterations = Math.max(0, cIterations);
        }

    @Override
    public Integer process(InvocableMap.Entry<Integer, Integer> entry)
        {
        Integer nValue = entry.getValue();
        long    nHash  = nValue == null ? 0L : nValue;

        for (int i = 0; i < m_cIterations; i++)
            {
            nHash = (nHash * 2862933555777941757L + 3037000493L) ^ (nHash >>> 17);
            }

        return (int) nHash;
        }

    private final int m_cIterations;
    }
