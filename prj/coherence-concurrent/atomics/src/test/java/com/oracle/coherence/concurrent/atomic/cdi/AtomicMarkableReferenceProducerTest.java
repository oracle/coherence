/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicMarkableReference;

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
 * Tests for {@link AtomicMarkableReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicMarkableReferenceProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicMarkableReferenceProducer.class)
                                                          .addBeanClass(AtomicMarkableReferenceBean.class));

    @Inject
    private AtomicMarkableReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo", true);
        assertThat(bean.getLocal().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedLocal().getReference(), is("bar"));
        assertThat(bean.getTypedLocal().isMarked(), is(false));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo", true);
        assertThat(bean.getRemote().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedRemote().getReference(), is("bar"));
        assertThat(bean.getTypedRemote().isMarked(), is(false));
        }

    @ApplicationScoped
    static class AtomicMarkableReferenceBean
        {
        @Inject
        AtomicMarkableReference<String> local;

        @Inject
        @Remote
        AtomicMarkableReference<String> remote;

        @Inject
        @Name("local")
        LocalAtomicMarkableReference<String> typedLocal;

        @Inject
        @Name("remote")
        RemoteAtomicMarkableReference<String> typedRemote;

        public AtomicMarkableReference<String> getLocal()
            {
            return local;
            }

        public AtomicMarkableReference<String> getRemote()
            {
            return remote;
            }

        public LocalAtomicMarkableReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicMarkableReference<String> getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
