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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Minimal blocking service-side workload used to put pressure on daemon-pool
 * scheduling without adding application work.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public class SleepingProcessor
        extends AbstractProcessor<Integer, Integer, Integer>
        implements Serializable
    {
    public SleepingProcessor(int cMicros)
        {
        m_cNanos = TimeUnit.MICROSECONDS.toNanos(Math.max(0, cMicros));
        }

    @Override
    public Integer process(InvocableMap.Entry<Integer, Integer> entry)
        {
        long cNanos = m_cNanos;
        if (cNanos > 0L)
            {
            LockSupport.parkNanos(cNanos);
            }

        Integer nValue = entry.getValue();
        return nValue == null ? -1 : nValue;
        }

    private final long m_cNanos;
    }
