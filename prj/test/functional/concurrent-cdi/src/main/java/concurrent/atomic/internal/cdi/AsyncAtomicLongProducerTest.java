/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicLong;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicLong;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicLong;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AsyncAtomicLongProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AsyncAtomicLongProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicLongProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicLongProducer.class)
                                                          .addBeanClass(AsyncAtomicLongBean.class));

    @Inject
    private AsyncAtomicLongBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0L).join();
        assertThat(bean.getLocal().getAndSet(2L).join(), is(0L));
        assertThat(bean.getTypedLocal().get().join(), is(2L));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set(1L).join();
        assertThat(bean.getLocalNamed().getAndSet(4L).join(), is(1L));
        assertThat(bean.getTypedLocalNamed().get().join(), is(4L));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set(11L).join();
        assertThat(bean.getLocalUnqualified().getAndSet(44L).join(), is(11L));
        assertThat(bean.getTypedLocalNamed().get().join(), is(44L));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(3L).join();
        assertThat(bean.getRemote().getAndSet(6L).join(), is(3L));
        assertThat(bean.getTypedRemote().get().join(), is(6L));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set(5L).join();
        assertThat(bean.getRemoteNamed().getAndSet(8L).join(), is(5L));
        assertThat(bean.getTypedRemoteNamed().get().join(), is(8L));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set(5L).join();
        assertThat(bean.getTypedRemoteUnqualified().getAndSet(8L).join(), is(5L));
        assertThat(bean.getTypedRemoteNamed().get().join(), is(8L));
        }

    @ApplicationScoped
    static class AsyncAtomicLongBean
        {
        @Inject
        AsyncAtomicLong local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        AsyncLocalAtomicLong typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AsyncAtomicLong localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        AsyncLocalAtomicLong typedLocalNamed;

        @Inject
        AsyncLocalAtomicLong typedLocalUnqualified;

        @Inject
        @Remote
        AsyncAtomicLong remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AsyncAtomicLong remoteNamed;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicLong typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        AsyncRemoteAtomicLong typedRemoteNamed;

        @Inject
        AsyncRemoteAtomicLong typedRemoteUnqualified;

        public AsyncAtomicLong getLocal()
            {
            return local;
            }

        public AsyncAtomicLong getLocalNamed()
            {
            return localNamed;
            }

        public AsyncAtomicLong getRemoteNamed()
            {
            return remoteNamed;
            }

        public AsyncAtomicLong getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicLong getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicLong getTypedRemote()
            {
            return typedRemote;
            }

        public AsyncLocalAtomicLong getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public AsyncLocalAtomicLong getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public AsyncRemoteAtomicLong getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public AsyncRemoteAtomicLong getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
