/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import com.tangosol.internal.util.VirtualThreads;

import java.lang.reflect.Method;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runtime gate that prevents benchmark runs from silently falling back to
 * platform-thread execution when virtual mode is requested.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public final class VirtualThreadProbe
    {
    private VirtualThreadProbe()
        {
        }

    public static void verifyVirtualThreads(String sPrefix, String sContext)
        {
        boolean fSupported = VirtualThreads.isSupported();
        System.out.println(sPrefix + " supported=" + fSupported);

        if (!fSupported)
            {
            throw new IllegalStateException(sContext + " requires VirtualThreads.isSupported() == true;"
                    + " benchmark packaging or launch path is not resolving the MR-JAR helper.");
            }

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread threadProbe = VirtualThreads.makeThread(null, () ->
            {
            started.countDown();
            try
                {
                if (!release.await(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    {
                    throw new IllegalStateException(sContext + " probe thread timed out waiting for release.");
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(sContext + " probe thread was interrupted.", e);
                }
            }, "vt-probe");

        threadProbe.start();

        try
            {
            if (!started.await(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                {
                throw new IllegalStateException(sContext + " probe thread did not start in time.");
                }

            boolean fVirtual = isVirtualThread(threadProbe);
            System.out.println(sPrefix + " probeClass="
                    + threadProbe.getClass().getName() + ", probeVirtual=" + fVirtual);

            if (!fVirtual)
                {
                throw new IllegalStateException(sContext
                        + " requires VirtualThreads.makeThread(...) to produce a virtual thread;"
                        + " benchmark packaging or launch path fell back to platform threads.");
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while validating benchmark virtual-thread runtime.", e);
            }
        finally
            {
            release.countDown();
            try
                {
                threadProbe.join(TimeUnit.SECONDS.toMillis(PROBE_TIMEOUT_SECONDS));
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for virtual-thread probe to exit.", e);
                }
            }

        if (threadProbe.isAlive())
            {
            throw new IllegalStateException(sContext + " probe thread failed to terminate.");
            }
        }

    public static boolean isVirtualThread(Thread thread)
        {
        if (thread == null)
            {
            return false;
            }

        try
            {
            Method method = Thread.class.getMethod("isVirtual");
            return Boolean.TRUE.equals(method.invoke(thread));
            }
        catch (ReflectiveOperationException e)
            {
            throw new IllegalStateException("Unable to inspect Thread.isVirtual().", e);
            }
        }

    private static final long PROBE_TIMEOUT_SECONDS = 5L;
    }
