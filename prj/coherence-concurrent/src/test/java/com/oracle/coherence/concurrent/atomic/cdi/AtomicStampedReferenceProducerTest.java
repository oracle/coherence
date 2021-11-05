/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicStampedReference;

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
 * Tests for {@link AtomicStampedReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicStampedReferenceProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicStampedReferenceProducer.class)
                                                          .addBeanClass(AtomicStampedReferenceBean.class));

    @Inject
    private AtomicStampedReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo", 1);
        assertThat(bean.getLocal().compareAndSet("foo", "bar", 1, 2), is(true));
        assertThat(bean.getTypedLocal().getReference(), is("bar"));
        assertThat(bean.getTypedLocal().getStamp(), is(2));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo", 1);
        assertThat(bean.getRemote().compareAndSet("foo", "bar", 1, 2), is(true));
        assertThat(bean.getTypedRemote().getReference(), is("bar"));
        assertThat(bean.getTypedRemote().getStamp(), is(2));
        }

    @ApplicationScoped
    static class AtomicStampedReferenceBean
        {
        @Inject
        AtomicStampedReference<String> local;

        @Inject
        @Remote
        AtomicStampedReference<String> remote;

        @Inject
        @Name("local")
        LocalAtomicStampedReference<String> typedLocal;

        @Inject
        @Name("remote")
        RemoteAtomicStampedReference<String> typedRemote;

        public AtomicStampedReference<String> getLocal()
            {
            return local;
            }

        public AtomicStampedReference<String> getRemote()
            {
            return remote;
            }

        public LocalAtomicStampedReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicStampedReference<String> getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
