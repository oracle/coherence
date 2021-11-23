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

import com.oracle.coherence.concurrent.locks.DistributedReadWriteLock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * Tests for {@link ReadWriteLockProducer}.
 *
 * @author Aleks Seovic  2021.11.22
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReadWriteLockProducerTest
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(ReadWriteLockProducer.class)
                                                          .addBeanClass(ReadWriteLockBean.class));

    @Inject
    private ReadWriteLockBean bean;

    @Test
    void testLocalInjection()
        {
        assertThat(bean.getLocal(), is(bean.getTypedLocal()));
        assertThat(bean.getLocal().writeLock().tryLock(), is(true));
        assertThat(bean.getTypedLocal().isWriteLocked(), is(true));
        bean.getLocal().writeLock().unlock();
        
        assertThat(bean.getLocal().readLock().tryLock(), is(true));
        assertThat(bean.getTypedLocal().getReadHoldCount(), is(1));
        assertThat(bean.getTypedLocal().getReadLockCount(), is(1));
        bean.getLocal().readLock().unlock();
        assertThat(bean.getTypedLocal().getReadLockCount(), is(0));
        }

    @Test
    void testRemoteInjection()
        {
        assertThat(bean.getRemote(), is(bean.getTypedRemote()));
        assertThat(bean.getRemote().writeLock().tryLock(), is(true));
        assertThat(bean.getTypedRemote().isWriteLocked(), is(true));
        bean.getRemote().writeLock().unlock();
        
        assertThat(bean.getRemote().readLock().tryLock(), is(true));
        assertThat(bean.getTypedRemote().getReadHoldCount(), is(1));
        assertThat(bean.getTypedRemote().getReadLockCount(), is(1));
        bean.getRemote().readLock().unlock();
        assertThat(bean.getTypedRemote().getReadLockCount(), is(0));
        }

    @ApplicationScoped
    static class ReadWriteLockBean
        {
        @Inject
        ReadWriteLock local;

        @Inject
        @Remote
        ReadWriteLock remote;

        @Inject
        @Name("local")
        ReentrantReadWriteLock typedLocal;

        @Inject
        @Name("remote")
        DistributedReadWriteLock typedRemote;

        public ReadWriteLock getLocal()
            {
            return local;
            }

        public ReadWriteLock getRemote()
            {
            return remote;
            }

        public ReentrantReadWriteLock getTypedLocal()
            {
            return typedLocal;
            }

        public DistributedReadWriteLock getTypedRemote()
            {
            return typedRemote;
            }
        }
    }
