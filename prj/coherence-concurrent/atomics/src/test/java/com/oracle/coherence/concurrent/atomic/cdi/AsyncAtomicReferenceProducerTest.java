/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicReference;

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
 * Tests for {@link AsyncAtomicReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.08
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicReferenceProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicReferenceProducer.class)
                                                          .addBeanClass(AsyncAtomicReferenceBean.class));

    @Inject
    private AsyncAtomicReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo").join();
        assertThat(bean.getLocal().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedLocal().get().join(), is("bar"));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo").join();
        assertThat(bean.getRemote().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedRemote().get().join(), is("bar"));
        }

    @ApplicationScoped
    static class AsyncAtomicReferenceBean
        {
        @Inject
        AsyncAtomicReference<String> local;

        @Inject
        @Remote
        AsyncAtomicReference<String> remote;

        @Inject
        @Name("local")
        AsyncLocalAtomicReference<String> typedLocal;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicReference<String> typedRemote;

        public AsyncAtomicReference<String> getLocal()
            {
            return local;
            }

        public AsyncAtomicReference<String> getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicReference<String> getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
