/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import com.tangosol.coherence.component.util.DaemonPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * Lightweight benchmark-side sampler for daemon-pool backlog peaks.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public class DaemonPoolMetricsSampler
    {
    public DaemonPoolMetricsSampler(Supplier<DaemonPool> supplierPool)
        {
        f_supplierPool = supplierPool;
        }

    public void start()
        {
        f_cMaxBacklog.set(0L);
        f_fSampling.set(true);

        Thread thread = new Thread(() ->
            {
            while (f_fSampling.get())
                {
                DaemonPool pool = f_supplierPool.get();
                if (pool != null)
                    {
                    updateMax(f_cMaxBacklog, pool.getBacklog());
                    }
                LockSupport.parkNanos(SAMPLER_INTERVAL_NANOS);
                }
            }, "dpb-backlog-sampler");

        thread.setDaemon(true);
        m_thread = thread;
        thread.start();
        }

    public void stop()
        {
        f_fSampling.set(false);

        Thread thread = m_thread;
        if (thread != null)
            {
            try
                {
                thread.join(1_000L);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            finally
                {
                m_thread = null;
                }
            }
        }

    public long getMaxBacklog()
        {
        return f_cMaxBacklog.get();
        }

    private void updateMax(AtomicLong atomicMax, long cValue)
        {
        long cCurrent;
        do
            {
            cCurrent = atomicMax.get();
            if (cValue <= cCurrent)
                {
                return;
                }
            }
        while (!atomicMax.compareAndSet(cCurrent, cValue));
        }

    private final Supplier<DaemonPool> f_supplierPool;
    private final AtomicBoolean        f_fSampling = new AtomicBoolean();
    private final AtomicLong           f_cMaxBacklog = new AtomicLong();
    private volatile Thread            m_thread;

    private static final long SAMPLER_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(1);
    }
