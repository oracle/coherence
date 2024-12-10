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

import com.oracle.coherence.concurrent.locks.RemoteLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.locks.internal.cdi.LockProducer;
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
 * Tests for {@link LockProducer}.
 *
 * @author Aleks Seovic  2021.11.22
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LockProducerTest
    {

    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
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
        assertThat(bean.getLocal(), sameInstance(bean.getTypedLocal()));
        assertThat(bean.getLocal().tryLock(), is(true));
        assertThat(bean.getTypedLocal().isLocked(), is(true));
        }

    @Test
    void testLocalNamedInjection()
        {
        assertThat(bean.getLocalNamed(), sameInstance(bean.getTypedLocalNamed()));
        assertThat(bean.getLocalNamed(), sameInstance(bean.getLocalUnqualified()));
        assertThat(bean.getLocalNamed().tryLock(), is(true));
        assertThat(bean.getTypedLocalNamed().isLocked(), is(true));
        }

    @Test
    void testRemoteInjection()
        {
        assertThat(bean.getRemote(), sameInstance(bean.getTypedRemote()));
        assertThat(bean.getRemote().tryLock(), is(true));
        assertThat(bean.getTypedRemote().isLocked(), is(true));
        }

    @Test
    void testRemoteNamedInjection()
        {
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteNamed()));
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteUnqualified()));
        assertThat(bean.getRemoteNamed().tryLock(), is(true));
        assertThat(bean.getTypedRemoteNamed().isLocked(), is(true));
        }

    // ----- inner class LockBean ---------------------------------

    @ApplicationScoped
    static class LockBean
        {
        @Inject
        Lock local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        ReentrantLock typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        Lock localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        ReentrantLock typedLocalNamed;

        @Inject
        ReentrantLock typedLocalUnqualified;

        @Inject
        @Remote
        Lock remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        Lock remoteNamed;

        @Inject
        @Name("remote")
        RemoteLock typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteLock typedRemoteNamed;

        @Inject
        RemoteLock typedRemoteUnqualified;

        public Lock getLocal()
            {
            return local;
            }

        public Lock getLocalNamed()
            {
            return localNamed;
            }

        public Lock getRemoteNamed()
            {
            return remoteNamed;
            }

        public Lock getRemote()
            {
            return remote;
            }

        public ReentrantLock getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteLock getTypedRemote()
            {
            return typedRemote;
            }

        public ReentrantLock getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public ReentrantLock getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteLock getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteLock getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
