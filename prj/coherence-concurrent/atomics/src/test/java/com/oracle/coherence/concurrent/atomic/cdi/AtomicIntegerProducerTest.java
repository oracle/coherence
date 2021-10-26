/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicInteger;
import com.oracle.coherence.concurrent.atomic.LocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicInteger;

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
 * Tests for {@link AtomicIntegerProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicIntegerProducerTest
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicIntegerProducer.class)
                                                          .addBeanClass(AtomicIntegerBean.class));

    @Inject
    private AtomicIntegerBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0);
        assertThat(bean.getLocal().getAndSet(2), is(0));
        assertThat(bean.getTypedLocal().get(), is(2));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(0);
        assertThat(bean.getRemote().getAndSet(3), is(0));
        assertThat(bean.getTypedRemote().get(), is(3));
        }

    @ApplicationScoped
    static class AtomicIntegerBean
        {
        @Inject
        AtomicInteger local;

        @Inject
        @Remote
        AtomicInteger remote;

        @Inject
        @Name("local")
        LocalAtomicInteger typedLocal;

        @Inject
        @Name("remote")
        RemoteAtomicInteger typedRemote;

        public AtomicInteger getLocal()
            {
            return local;
            }

        public AtomicInteger getRemote()
            {
            return remote;
            }

        public LocalAtomicInteger getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicInteger getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
