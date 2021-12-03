/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal.cdi;

import com.oracle.coherence.cdi.Name;

import com.oracle.coherence.concurrent.DistributedCountDownLatch;
import com.oracle.coherence.concurrent.Latches;

import com.oracle.coherence.concurrent.cdi.Count;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

import java.util.concurrent.CountDownLatch;

/**
 * CDI producers for {@link DistributedCountDownLatch} or
 * {@link CountDownLatch} object.
 *
 * @author lh  2021.11.29
 * @since 21.12
 */
@ApplicationScoped
class CountDownLatchProducer
        extends AbstractLatchProducer
    {
    /**
     * Returns a local {@link CountDownLatch} for the provided
     * {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link CountDownLatch} for the provided
     *         {@link InjectionPoint}
     */
    @Produces
    CountDownLatch getUnqualifiedCountDownLatch(InjectionPoint ip)
        {
        return getLocalCountDownLatch(ip);
        }

    /**
     * Returns a {@link DistributedCountDownLatch} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link DistributedCountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    DistributedCountDownLatch getUnqualifiedDistributedCountDownLatch(InjectionPoint ip)
        {
        return getDistributedCountDownLatch(ip);
        }

    /**
     * Returns a {@link CountDownLatch} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link CountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Count()
    @Typed(CountDownLatch.class)
    CountDownLatch getLocalCountDownLatch(InjectionPoint ip)
        {
        return Latches.localCountDownLatch(getName(ip), getCount(ip));
        }

    /**
     * Returns a {@link DistributedCountDownLatch} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link DistributedCountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Count()
    @Typed(DistributedCountDownLatch.class)
    DistributedCountDownLatch getDistributedCountDownLatch(InjectionPoint ip)
        {
        return Latches.countDownLatch(getName(ip), getCount(ip));
        }
    }