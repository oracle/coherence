/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool;

import com.tangosol.coherence.performance.benchmarks.daemonpool.common.PinnedClassPreTouch;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.VirtualThreadProbe;

import com.tangosol.net.DefaultCacheServer;

/**
 * DefaultCacheServer launcher used by multi-member daemon-pool benchmarks.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public class MultiMemberCacheServer
    {
    public static void main(String[] asArg)
        {
        if (Boolean.getBoolean("benchmark.daemonpool.pinned.preTouch"))
            {
            PinnedClassPreTouch.preTouch();
            }

        if ("virtual".equalsIgnoreCase(System.getProperty("coherence.benchmark.daemonpool")))
            {
            VirtualThreadProbe.verifyVirtualThreads("MultiMemberCacheServer[vt-check]",
                    "Multi-member virtual daemon-pool member");
            }

        DefaultCacheServer.main(asArg);
        }
    }
