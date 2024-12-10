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

import com.oracle.coherence.concurrent.Semaphore;
import com.oracle.coherence.concurrent.LocalSemaphore;
import com.oracle.coherence.concurrent.RemoteSemaphore;

import com.oracle.coherence.concurrent.cdi.Permits;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.internal.cdi.SemaphoreProducer;
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
 * Tests for {@link SemaphoreProducer}.

 * @author Vaso Putica  2021.12.01
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SemaphoreProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                                .addExtension(new CoherenceExtension())
                                                                .addPackages(CoherenceExtension.class)
                                                                .addPackages(CoherenceServerExtension.class)
                                                                .addBeanClass(SemaphoreProducer.class)
                                                                .addBeanClass(SemaphoreBean.class));

    @Inject
    private SemaphoreBean bean;

    @Test
    void testLocalInjection()
        {
        assertThat(bean.getLocal(), sameInstance(bean.getTypedLocal()));
        assertThat(bean.getLocal().availablePermits(), is(2));
        bean.getLocal().acquireUninterruptibly();
        assertThat(bean.getTypedLocal().availablePermits(), is(1));
        }

    @Test
    void testLocalNamedInjection()
        {
        assertThat(bean.getLocalNamed(), sameInstance(bean.getTypedLocalNamed()));
        assertThat(bean.getLocalNamed(), sameInstance(bean.getLocalUnqualified()));
        assertThat(bean.getLocalNamed().availablePermits(), is(0));
        bean.getLocalNamed().release();
        assertThat(bean.getTypedLocalNamed().availablePermits(), is(1));
        }

    @Test
    void testRemoteInjection()
        {
        assertThat(bean.getRemote(), sameInstance(bean.getTypedRemote()));
        assertThat(bean.getRemote().availablePermits(), is(2));
        bean.getRemote().acquireUninterruptibly();
        assertThat(bean.getTypedRemote().availablePermits(), is(1));
        }

    @Test
    void testRemoteNamedInjection()
        {
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteNamed()));
        assertThat(bean.getRemoteNamed(), sameInstance(bean.getTypedRemoteUnqualified()));
        assertThat(bean.getRemoteNamed().availablePermits(), is(0));
        bean.getRemoteNamed().release();
        assertThat(bean.getTypedRemoteNamed().availablePermits(), is(1));
        }

    // ----- inner class SemaphoreBean ---------------------------------

    @ApplicationScoped
    static class SemaphoreBean
        {
        @Inject
        @Permits(2)
        Semaphore local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        @Permits(2)
        LocalSemaphore typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        Semaphore localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        LocalSemaphore typedLocalNamed;

        @Inject
        LocalSemaphore typedLocalUnqualified;

        @Inject
        @Remote
        @Permits(2)
        Semaphore remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        Semaphore remoteNamed;

        @Inject
        @Name("remote")
        @Permits(2)
        RemoteSemaphore typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteSemaphore typedRemoteNamed;

        @Inject
        RemoteSemaphore typedRemoteUnqualified;

        public Semaphore getLocal()
            {
            return local;
            }

        public Semaphore getLocalNamed()
            {
            return localNamed;
            }

        public Semaphore getRemoteNamed()
            {
            return remoteNamed;
            }

        public Semaphore getRemote()
            {
            return remote;
            }

        public LocalSemaphore getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteSemaphore getTypedRemote()
            {
            return typedRemote;
            }

        public LocalSemaphore getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public LocalSemaphore getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteSemaphore getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteSemaphore getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
