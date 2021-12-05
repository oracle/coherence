/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.semaphores.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.server.CoherenceServerExtension;
import com.oracle.coherence.concurrent.semaphores.DistributedSemaphore;
import com.oracle.coherence.concurrent.semaphores.Semaphores;
import com.oracle.coherence.concurrent.semaphores.cdi.Permits;

import java.util.concurrent.Semaphore;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link DistributedSemaphoreProducer}.

 * @author Vaso Putica  2021.12.01
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DistributedSemaphoreProducerTest
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                                .addExtension(new CoherenceExtension())
                                                                .addPackages(CoherenceExtension.class)
                                                                .addPackages(CoherenceServerExtension.class)
                                                                .addBeanClass(DistributedSemaphoreProducer.class)
                                                                .addBeanClass(DistributedSemaphoreBean.class));

    @Inject
    private DistributedSemaphoreBean bean;

    @Test
    void testLocalInjection()
        {
        Semaphore localSemaphore = bean.getLocal();
        int permits = localSemaphore.availablePermits();
        assertThat(permits, is(1));
        assertThat(Semaphores.localSemaphore("foo", permits), notNullValue());

        Semaphore localNoNameSemaphore = bean.getLocalNoName();
        permits = localNoNameSemaphore.availablePermits();
        assertThat(permits, is(3));
        assertThat(localNoNameSemaphore, is(Semaphores.localSemaphore("localNoName", 3)));
        }

    @Test
    void testDistributedInjection()
        {
        DistributedSemaphore distributedSemaphore = bean.getDistributed();
        assertThat(distributedSemaphore.availablePermits(), is(2));
        distributedSemaphore.acquireUninterruptibly();
        assertThat(distributedSemaphore.availablePermits(), is(1));
        distributedSemaphore.acquireUninterruptibly();

        DistributedSemaphore distributedNoNameSemaphore = bean.getDistributedNoName();
        assertThat(distributedNoNameSemaphore.availablePermits(), is(4));
        assertThat(distributedNoNameSemaphore, is(Semaphores.remoteSemaphore("distributedNoName", 4)));
        }

    // ----- inner class DistributedSemaphoreBean ---------------------------------

    @ApplicationScoped
    static class DistributedSemaphoreBean
        {
        @Inject
        @Name("foo")
        @Permits(1)
        Semaphore local;

        @Inject
        @Permits(3)
        Semaphore localNoName;

        @Inject
        @Name("foo")
        @Permits(2)
        DistributedSemaphore distributed;

        @Inject
        @Permits(4)
        DistributedSemaphore distributedNoName;

        public Semaphore getLocal()
            {
            return local;
            }

        public Semaphore getLocalNoName()
            {
            return localNoName;
            }

        public DistributedSemaphore getDistributed()
            {
            return distributed;
            }

        public DistributedSemaphore getDistributedNoName()
            {
            return distributedNoName;
            }
        }

    }
