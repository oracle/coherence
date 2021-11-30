/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicReference;

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
 * Tests for {@link AtomicReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.08
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicReferenceProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicReferenceProducer.class)
                                                          .addBeanClass(AtomicReferenceBean.class));

    @Inject
    private AtomicReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo");
        assertThat(bean.getLocal().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedLocal().get(), is("bar"));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo");
        assertThat(bean.getRemote().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedRemote().get(), is("bar"));
        }

    @ApplicationScoped
    static class AtomicReferenceBean
        {
        @Inject
        AtomicReference<String> local;

        @Inject
        @Remote
        AtomicReference<String> remote;

        @Inject
        @Name("local")
        LocalAtomicReference<String> typedLocal;

        @Inject
        @Name("remote")
        RemoteAtomicReference<String> typedRemote;

        public AtomicReference<String> getLocal()
            {
            return local;
            }

        public AtomicReference<String> getRemote()
            {
            return remote;
            }

        public LocalAtomicReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicReference<String> getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
