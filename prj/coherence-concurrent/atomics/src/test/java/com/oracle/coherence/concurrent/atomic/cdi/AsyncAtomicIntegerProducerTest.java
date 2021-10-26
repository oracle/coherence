/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicInteger;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AsyncAtomicIntegerProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicIntegerProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicIntegerProducer.class)
                                                          .addBeanClass(AsyncAtomicIntegerBean.class));

    @Inject
    private AsyncAtomicIntegerBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0).join();
        assertThat(bean.getLocal().getAndSet(2).join(), is(0));
        assertThat(bean.getTypedLocal().get().join(), is(2));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(0).join();
        assertThat(bean.getRemote().getAndSet(3).join(), is(0));
        assertThat(bean.getTypedRemote().get().join(), is(3));
        }

    @ApplicationScoped
    static class AsyncAtomicIntegerBean
        {
        @Inject
        AsyncAtomicInteger local;

        @Inject
        @Remote
        AsyncAtomicInteger remote;

        @Inject
        @Name("local")
        AsyncLocalAtomicInteger typedLocal;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicInteger typedRemote;

        public AsyncAtomicInteger getLocal()
            {
            return local;
            }

        public AsyncAtomicInteger getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicInteger getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicInteger getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
