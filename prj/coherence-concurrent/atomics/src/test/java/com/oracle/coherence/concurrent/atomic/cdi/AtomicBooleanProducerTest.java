/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicBoolean;
import com.oracle.coherence.concurrent.atomic.LocalAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicBoolean;

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
 * Tests for {@link AtomicBooleanProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicBooleanProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicBooleanProducer.class)
                                                          .addBeanClass(AtomicBooleanBean.class));

    @Inject
    private AtomicBooleanBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(true);
        assertThat(bean.getLocal().getAndSet(false), is(true));
        assertThat(bean.getTypedLocal().get(), is(false));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(true);
        assertThat(bean.getRemote().getAndSet(false), is(true));
        assertThat(bean.getTypedRemote().get(), is(false));
        }

    @ApplicationScoped
    static class AtomicBooleanBean
        {
        @Inject
        AtomicBoolean local;

        @Inject
        @Remote
        AtomicBoolean remote;

        @Inject
        @Name("local")
        LocalAtomicBoolean typedLocal;

        @Inject
        @Name("remote")
        RemoteAtomicBoolean typedRemote;

        public AtomicBoolean getLocal()
            {
            return local;
            }

        public AtomicBoolean getRemote()
            {
            return remote;
            }

        public AtomicBoolean getTypedLocal()
            {
            return typedLocal;
            }

        public AtomicBoolean getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
