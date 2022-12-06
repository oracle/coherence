/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import com.oracle.coherence.common.base.Factory;
import com.oracle.coherence.common.internal.Platform;
import com.oracle.coherence.common.internal.net.MultiProviderSelectionService;
import com.oracle.coherence.common.internal.net.ResumableSelectionService;
import com.oracle.coherence.common.internal.net.HashSelectionService;
import com.oracle.coherence.common.internal.net.RoundRobinSelectionService;
import com.oracle.coherence.common.util.Duration;

import java.util.concurrent.ThreadFactory;


/**
 * The SelectionServices class provides helper methods related to
 * SelectionServices.
 *
 * @author mf  2010.12.01
 */
public class SelectionServices
    {
    /**
     * Return A singleton instance of a SelectionService which is suitable for
     * managing a large number of SelectableChannels efficiently.
     * <p>
     * The number of threads used to handle the load can be influenced via the
     * <tt>com.oracle.coherence.common.net.SelectionServices.threads</tt> system property, and defaults to a value
     * relative to the number of available cores and percentage of system memory the VM has been sized to use.
     * <p>
     * The threads used to run the service will be dynamically started and
     * stopped.  The idle timeout can be specified via the
     * <tt>com.oracle.coherence.common.net.SelectionServices.timeout</tt> system property, and defaults to 5s.
     *
     * @return the default SelectionService
     */
    public static SelectionService getDefaultService()
        {
        return DefaultServiceHolder.INSTANCE;
        }

    /**
     * LoadBalancer defines the various load-balancing algorithms.
     */
    private enum LoadBalancer
        {
        HASH,
        ROUND_ROBIN
        }

    /**
     * The Holder for the DefaultService reference to allow for lazy
     * instantiation.
     */
    private static final class DefaultServiceHolder
        {
        /**
         * The default SelectionService instance.
         */
        private static final SelectionService INSTANCE;

        /**
         * The associated ThreadGroup.
         */
        private static final ThreadGroup GROUP = new ThreadGroup("SelectionService");
        static
            {
            GROUP.setDaemon(false); // keeps group from being auto-destroyed
            }

        static
            {
            final int cThreads        = Integer.parseInt(System.getProperty(SelectionServices.class.getName() + ".threads",
                     String.valueOf(Platform.getPlatform().getFairShareProcessors())));
            final long cMillisTimeout = new Duration(System.getProperty(SelectionServices.class.getName() + ".timeout", "5s"))
                    .as(Duration.Magnitude.MILLI);
            LoadBalancer balancer     = LoadBalancer.valueOf(
                    System.getProperty(SelectionServices.class.getName() + ".loadBalancer", LoadBalancer.HASH.name()));

            final Factory<SelectionService> factoryService =
                new Factory<SelectionService>()
                {
                @Override
                public SelectionService create()
                    {
                    return new ResumableSelectionService(new ThreadFactory()
                        {
                        public Thread newThread(Runnable r)
                            {
                            Thread thread = new Thread(GROUP, r);
                            thread.setDaemon(true);
                            return thread;
                            }
                        }).setIdleTimeout(cMillisTimeout);
                    }
                };

            INSTANCE = new MultiProviderSelectionService(
                balancer == LoadBalancer.ROUND_ROBIN
                    ? new Factory<SelectionService>()
                        {
                        @Override
                        public SelectionService create()
                            {
                            return new RoundRobinSelectionService(cThreads, factoryService);
                            }
                        }
                    : new Factory<SelectionService>()
                        {
                        @Override
                        public SelectionService create()
                            {
                            return new HashSelectionService(cThreads, factoryService);
                            }
                        })
                {
                public void shutdown()
                    {
                    // Service shutdown is not supported on the singleton
                    throw new UnsupportedOperationException();
                    }
                };
            }
        }
    }
