/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.locks.DistributedLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
 * Tests for {@link LockProducer}.
 *
 * @author Aleks Seovic  2021.11.22
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LockProducerTest
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(LockProducer.class)
                                                          .addBeanClass(LockBean.class));

    @Inject
    private LockBean bean;

    @Test
    void testLocalInjection()
        {
        assertThat(bean.getLocal(), is(bean.getTypedLocal()));
        assertThat(bean.getLocal().tryLock(), is(true));
        assertThat(bean.getTypedLocal().isLocked(), is(true));
        bean.getLocal().unlock();
        assertThat(bean.getTypedLocal().isLocked(), is(false));
        }

    @Test
    void testRemoteInjection()
        {
        assertThat(bean.getRemote(), is(bean.getTypedRemote()));
        assertThat(bean.getRemote().tryLock(), is(true));
        assertThat(bean.getTypedRemote().isLocked(), is(true));
        bean.getRemote().unlock();
        assertThat(bean.getTypedRemote().isLocked(), is(false));
        }

    @ApplicationScoped
    static class LockBean
        {
        @Inject
        Lock local;

        @Inject
        @Remote
        Lock remote;

        @Inject
        @Name("local")
        ReentrantLock typedLocal;

        @Inject
        @Name("remote")
        DistributedLock typedRemote;

        public Lock getLocal()
            {
            return local;
            }

        public Lock getRemote()
            {
            return remote;
            }

        public ReentrantLock getTypedLocal()
            {
            return typedLocal;
            }

        public DistributedLock getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
