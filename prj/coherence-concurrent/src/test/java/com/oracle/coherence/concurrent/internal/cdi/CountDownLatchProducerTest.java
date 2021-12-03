/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Name;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.DistributedCountDownLatch;
import com.oracle.coherence.concurrent.Latches;

import com.oracle.coherence.concurrent.cdi.Count;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link CountDownLatchProducer}.
 *
 * @author lh  2021.11.30
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CountDownLatchProducerTest
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(CountDownLatchProducer.class)
                                                          .addBeanClass(CountDownLatchBean.class));

    @Inject
    private CountDownLatchBean bean;

    @Test
    void testLocalInjection()
        {
        CountDownLatch localLatch = bean.getLocal();
        long count = localLatch.getCount();
        assertThat(count, is(1L));
        assertThat(Latches.localCountDownLatch("foo"), notNullValue());

        localLatch = bean.getLocalNoName();
        count = localLatch.getCount();
        assertThat(count, is(3L));
        }

    @Test
    void testDistributedInjection()
        {
        DistributedCountDownLatch latch = bean.getDistributed();
        assertThat(latch.getCount(), is(2L));
        latch.countDown();
        assertThat(latch.getCount(), is(1L));
        latch.countDown();

        latch = bean.getDistributedNoName();
        assertThat(latch.getCount(), is(4L));
        }

    // ----- inner class CountDownLatchBean ---------------------------------

    @ApplicationScoped
    static class CountDownLatchBean
        {
        @Inject
        @Name("foo")
        @Count(1)
        CountDownLatch local;

        @Inject
        @Count(3)
        CountDownLatch localNoName;

        @Inject
        @Name("foo")
        @Count(2)
        DistributedCountDownLatch distributed;

        @Inject
        @Count(4)
        DistributedCountDownLatch distributedNoName;

        public CountDownLatch getLocal()
            {
            return local;
            }

        public CountDownLatch getLocalNoName()
            {
            return localNoName;
            }

        public DistributedCountDownLatch getDistributed()
            {
            return distributed;
            }

        public DistributedCountDownLatch getDistributedNoName()
            {
            return distributedNoName;
            }
        }
    }
