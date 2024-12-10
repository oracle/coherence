/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.locks.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.locks.RemoteReadWriteLock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.locks.internal.cdi.ReadWriteLockProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
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
    @SuppressWarnings("unused")
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
        assertThat(bean.getLocal(), sameInstance(bean.getTypedLocal()));
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
    void testLocalNamedInjection()
        {
        assertThat(bean.getLocalNamed(), sameInstance(bean.getTypedLocalNamed()));
        assertThat(bean.getLocalNamed(), sameInstance(bean.getLocalUnqualified()));
        assertThat(bean.getLocalNamed().writeLock().tryLock(), is(true));
        assertThat(bean.getTypedLocalNamed().isWriteLocked(), is(true));
        bean.getLocalUnqualified().writeLock().unlock();

        assertThat(bean.getLocalNamed().readLock().tryLock(), is(true));
        assertThat(bean.getTypedLocalNamed().getReadHoldCount(), is(1));
        assertThat(bean.getLocalUnqualified().getReadLockCount(), is(1));
        bean.getLocalNamed().readLock().unlock();
        assertThat(bean.getTypedLocalNamed().getReadLockCount(), is(0));
        }

    @Test
    void testRemoteInjection()
        {
        assertThat(bean.getRemote(), sameInstance(bean.getTypedRemote()));
        assertThat(bean.getRemote().writeLock().tryLock(), is(true));
        assertThat(bean.getTypedRemote().isWriteLocked(), is(true));
        bean.getRemote().writeLock().unlock();

        assertThat(bean.getRemote().readLock().tryLock(), is(true));
        assertThat(bean.getTypedRemote().getReadHoldCount(), is(1));
        assertThat(bean.getTypedRemote().getReadLockCount(), is(1));
        bean.getRemote().readLock().unlock();
        assertThat(bean.getTypedRemote().getReadLockCount(), is(0));
        }

    @Test
    void testRemoteNamedInjection()
        {
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteNamed()));
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteUnqualified()));
        assertThat(bean.getRemoteNamed().writeLock().tryLock(), is(true));
        assertThat(bean.getTypedRemoteNamed().isWriteLocked(), is(true));
        bean.getTypedRemoteUnqualified().writeLock().unlock();

        assertThat(bean.getRemoteNamed().readLock().tryLock(), is(true));
        assertThat(bean.getTypedRemoteNamed().getReadHoldCount(), is(1));
        assertThat(bean.getTypedRemoteUnqualified().getReadLockCount(), is(1));
        bean.getRemoteNamed().readLock().unlock();
        assertThat(bean.getTypedRemoteNamed().getReadLockCount(), is(0));
        }

    // ----- inner class ReadWriteLockBean ---------------------------------

    @ApplicationScoped
    static class ReadWriteLockBean
        {
        @Inject
        ReadWriteLock local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        ReentrantReadWriteLock typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        ReadWriteLock localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        ReentrantReadWriteLock typedLocalNamed;

        @Inject
        ReentrantReadWriteLock typedLocalUnqualified;

        @Inject
        @Remote
        ReadWriteLock remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        ReadWriteLock remoteNamed;

        @Inject
        @Name("remote")
        RemoteReadWriteLock typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteReadWriteLock typedRemoteNamed;

        @Inject
        RemoteReadWriteLock typedRemoteUnqualified;

        public ReadWriteLock getLocal()
            {
            return local;
            }

        public ReadWriteLock getLocalNamed()
            {
            return localNamed;
            }

        public ReadWriteLock getRemoteNamed()
            {
            return remoteNamed;
            }

        public ReadWriteLock getRemote()
            {
            return remote;
            }

        public ReentrantReadWriteLock getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteReadWriteLock getTypedRemote()
            {
            return typedRemote;
            }

        public ReentrantReadWriteLock getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public ReentrantReadWriteLock getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteReadWriteLock getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteReadWriteLock getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
