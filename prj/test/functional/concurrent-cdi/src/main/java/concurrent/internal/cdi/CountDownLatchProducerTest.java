/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Name;

import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.CountDownLatch;
import com.oracle.coherence.concurrent.LocalCountDownLatch;
import com.oracle.coherence.concurrent.RemoteCountDownLatch;

import com.oracle.coherence.concurrent.cdi.Count;

import com.oracle.coherence.concurrent.internal.cdi.CountDownLatchProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link CountDownLatchProducer}.
 *
 * @author lh  2021.11.30
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CountDownLatchProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(CountDownLatchProducer.class)
                                                          .addBeanClass(CountDownLatchBean.class));

    @Inject
    private CountDownLatchBean bean;

    @Test
    void testLocalInjection()
        {
        assertThat(bean.getLocal(), sameInstance(bean.getTypedLocal()));
        assertThat(bean.getLocal().getCount(), is(2L));
        bean.getLocal().countDown();
        assertThat(bean.getTypedLocal().getCount(), is(1L));
        }

    @Test
    void testLocalNamedInjection()
        {
        assertThat(bean.getLocalNamed(), sameInstance(bean.getTypedLocalNamed()));
        assertThat(bean.getLocalNamed(), sameInstance(bean.getLocalUnqualified()));
        assertThat(bean.getLocalNamed().getCount(), is(1L));
        bean.getLocalNamed().countDown();
        assertThat(bean.getTypedLocalNamed().getCount(), is(0L));
        }

    @Test
    void testRemoteInjection()
        {
        assertThat(bean.getRemote(), sameInstance(bean.getTypedRemote()));
        assertThat(bean.getRemote().getCount(), is(2L));
        bean.getRemote().countDown();
        assertThat(bean.getTypedRemote().getCount(), is(1L));
        }

    @Test
    void testRemoteNamedInjection()
        {
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteNamed()));
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteUnqualified()));
        assertThat(bean.getRemoteNamed().getCount(), is(1L));
        bean.getRemoteNamed().countDown();
        assertThat(bean.getTypedRemoteNamed().getCount(), is(0L));
        }

    // ----- inner class CountDownLatchBean ---------------------------------

    @ApplicationScoped
    static class CountDownLatchBean
        {
        @Inject
        @Count(2)
        CountDownLatch local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        @Count(2)
        LocalCountDownLatch typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        CountDownLatch localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        LocalCountDownLatch typedLocalNamed;

        @Inject
        LocalCountDownLatch typedLocalUnqualified;

        @Inject
        @Remote
        @Count(2)
        CountDownLatch remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        CountDownLatch remoteNamed;

        @Inject
        @Name("remote")
        @Count(2)
        RemoteCountDownLatch typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteCountDownLatch typedRemoteNamed;

        @Inject
        RemoteCountDownLatch typedRemoteUnqualified;

        public CountDownLatch getLocal()
            {
            return local;
            }

        public CountDownLatch getLocalNamed()
            {
            return localNamed;
            }

        public CountDownLatch getRemoteNamed()
            {
            return remoteNamed;
            }

        public CountDownLatch getRemote()
            {
            return remote;
            }

        public LocalCountDownLatch getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteCountDownLatch getTypedRemote()
            {
            return typedRemote;
            }

        public LocalCountDownLatch getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public LocalCountDownLatch getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteCountDownLatch getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteCountDownLatch getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
