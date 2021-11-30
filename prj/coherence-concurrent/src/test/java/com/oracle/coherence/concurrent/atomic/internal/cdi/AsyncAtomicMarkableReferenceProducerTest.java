/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicMarkableReference;

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
 * Tests for {@link AsyncAtomicMarkableReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicMarkableReferenceProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicMarkableReferenceProducer.class)
                                                          .addBeanClass(AsyncAtomicMarkableReferenceBean.class));

    @Inject
    private AsyncAtomicMarkableReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo", true).join();
        assertThat(bean.getLocal().compareAndSet("foo", "bar", true, false).join(), is(true));
        assertThat(bean.getTypedLocal().getReference().join(), is("bar"));
        assertThat(bean.getTypedLocal().isMarked().join(), is(false));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo", true).join();
        assertThat(bean.getRemote().compareAndSet("foo", "bar", true, false).join(), is(true));
        assertThat(bean.getTypedRemote().getReference().join(), is("bar"));
        assertThat(bean.getTypedRemote().isMarked().join(), is(false));
        }

    @ApplicationScoped
    static class AsyncAtomicMarkableReferenceBean
        {
        @Inject
        AsyncAtomicMarkableReference<String> local;

        @Inject
        @Remote
        AsyncAtomicMarkableReference<String> remote;

        @Inject
        @Name("local")
        AsyncLocalAtomicMarkableReference<String> typedLocal;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicMarkableReference<String> typedRemote;

        public AsyncAtomicMarkableReference<String> getLocal()
            {
            return local;
            }

        public AsyncAtomicMarkableReference<String> getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicMarkableReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicMarkableReference<String> getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
