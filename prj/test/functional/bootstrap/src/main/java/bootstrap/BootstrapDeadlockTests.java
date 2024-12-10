/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.CoherenceLifecycleEvents;
import com.tangosol.net.events.annotation.Interceptor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test for COH-22665 (bug 32321299) where the bootstrap API deadlocks if
 * shutdown is called before start-up is complete.
 *
 * @author Jonathan Knight  2020.12.24
 */
public class BootstrapDeadlockTests
    {
    @Test
    public void shouldNotDeadlock() throws Exception
        {
        Locker locker = new Locker();

        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withEventInterceptors(new StartInterceptor(locker), new StopInterceptor(locker))
                .build();

        Coherence coherence = Coherence.clusterMember(cfg);

        locker.setCoherence(coherence);

        CompletableFuture<Coherence> future = coherence.start();

        Coherence.closeAll();

        // should not timeout, which would indicate a deadlock (or a stupidly slow machine)
        future.get(5, TimeUnit.MINUTES);
        }

    public static class Locker
        {
        public Locker()
            {
            }

        public void setCoherence(Coherence coherence)
            {
            m_coherence = coherence;
            }

        public void setWait(boolean fWait)
            {
            m_fWait = fWait;
            }

        public synchronized void start()
            {
            // wait....
            while (m_fWait)
                {
                try
                    {
                    Thread.sleep(10);
                    }
                catch (InterruptedException e)
                    {
                    // ignored
                    }
                }
            m_coherence.start().join();
            }

        public synchronized void stop()
            {
            }

        private Coherence m_coherence;

        private volatile boolean m_fWait = true;
        }

    @Interceptor(identifier = "One")
    @CoherenceLifecycleEvents
    public static class StartInterceptor
            implements EventInterceptor<CoherenceLifecycleEvent>
        {
        public StartInterceptor(Locker locker)
            {
            m_locker = locker;
            }

        @Override
        public void onEvent(CoherenceLifecycleEvent event)
            {
            if (event.getType() == CoherenceLifecycleEvent.Type.STARTED)
                {
                m_locker.start();
                }
            }

        private final Locker m_locker;
        }

    @Interceptor(identifier = "Two")
    @CoherenceLifecycleEvents
    public static class StopInterceptor
            implements EventInterceptor<CoherenceLifecycleEvent>
        {
        public StopInterceptor(Locker locker)
            {
            m_locker = locker;
            }

        @Override
        public void onEvent(CoherenceLifecycleEvent event)
            {
            if (event.getType() == CoherenceLifecycleEvent.Type.STOPPED)
                {
                m_locker.setWait(false);
                m_locker.stop();
                }
            }

        private final Locker m_locker;
        }
    }
