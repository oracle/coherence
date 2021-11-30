/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicLong;
import com.oracle.coherence.concurrent.atomic.LocalAtomicLong;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicLong;

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
 * Tests for {@link AtomicLongProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicLongProducerTest
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicLongProducer.class)
                                                          .addBeanClass(AtomicLongBean.class));

    @Inject
    private AtomicLongBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0L);
        assertThat(bean.getLocal().getAndSet(2L), is(0L));
        assertThat(bean.getTypedLocal().get(), is(2L));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(0L);
        assertThat(bean.getRemote().getAndSet(3L), is(0L));
        assertThat(bean.getTypedRemote().get(), is(3L));
        }

    @ApplicationScoped
    static class AtomicLongBean
        {
        @Inject
        AtomicLong local;

        @Inject
        @Remote
        AtomicLong remote;

        @Inject
        @Name("local")
        LocalAtomicLong typedLocal;

        @Inject
        @Name("remote")
        RemoteAtomicLong typedRemote;

        public AtomicLong getLocal()
            {
            return local;
            }

        public AtomicLong getRemote()
            {
            return remote;
            }

        public LocalAtomicLong getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicLong getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
