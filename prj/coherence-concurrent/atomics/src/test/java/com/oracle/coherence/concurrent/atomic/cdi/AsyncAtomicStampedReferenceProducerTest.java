/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicStampedReference;

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
 * Tests for {@link AsyncAtomicStampedReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicStampedReferenceProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicStampedReferenceProducer.class)
                                                          .addBeanClass(AsyncAtomicStampedReferenceBean.class));

    @Inject
    private AsyncAtomicStampedReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo", 1).join();
        assertThat(bean.getLocal().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedLocal().getReference().join(), is("bar"));
        assertThat(bean.getTypedLocal().getStamp().join(), is(2));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo", 1).join();
        assertThat(bean.getRemote().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedRemote().getReference().join(), is("bar"));
        assertThat(bean.getTypedRemote().getStamp().join(), is(2));
        }

    @ApplicationScoped
    static class AsyncAtomicStampedReferenceBean
        {
        @Inject
        AsyncAtomicStampedReference<String> local;

        @Inject
        @Remote
        AsyncAtomicStampedReference<String> remote;

        @Inject
        @Name("local")
        AsyncLocalAtomicStampedReference<String> typedLocal;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicStampedReference<String> typedRemote;

        public AsyncAtomicStampedReference<String> getLocal()
            {
            return local;
            }

        public AsyncAtomicStampedReference<String> getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicStampedReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicStampedReference<String> getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
