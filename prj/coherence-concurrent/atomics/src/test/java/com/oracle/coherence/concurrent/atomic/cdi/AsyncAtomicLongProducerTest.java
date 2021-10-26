/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicLong;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicLong;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicLong;

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
 * Tests for {@link AsyncAtomicLongProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicLongProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicLongProducer.class)
                                                          .addBeanClass(AsyncAtomicLongBean.class));

    @Inject
    private AsyncAtomicLongBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0L).join();
        assertThat(bean.getLocal().getAndSet(2L).join(), is(0L));
        assertThat(bean.getTypedLocal().get().join(), is(2L));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(0L).join();
        assertThat(bean.getRemote().getAndSet(3L).join(), is(0L));
        assertThat(bean.getTypedRemote().get().join(), is(3L));
        }

    @ApplicationScoped
    static class AsyncAtomicLongBean
        {
        @Inject
        AsyncAtomicLong local;

        @Inject
        @Remote
        AsyncAtomicLong remote;

        @Inject
        @Name("local")
        AsyncLocalAtomicLong typedLocal;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicLong typedRemote;

        public AsyncAtomicLong getLocal()
            {
            return local;
            }

        public AsyncAtomicLong getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicLong getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicLong getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
