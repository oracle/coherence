/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicInteger;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AsyncAtomicIntegerProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AsyncAtomicIntegerProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicIntegerProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicIntegerProducer.class)
                                                          .addBeanClass(AsyncAtomicIntegerBean.class));

    @Inject
    private AsyncAtomicIntegerBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0).join();
        assertThat(bean.getLocal().getAndSet(2).join(), is(0));
        assertThat(bean.getTypedLocal().get().join(), is(2));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set(1).join();
        assertThat(bean.getLocalNamed().getAndSet(4).join(), is(1));
        assertThat(bean.getTypedLocalNamed().get().join(), is(4));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set(11).join();
        assertThat(bean.getLocalUnqualified().getAndSet(44).join(), is(11));
        assertThat(bean.getTypedLocalNamed().get().join(), is(44));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(3).join();
        assertThat(bean.getRemote().getAndSet(6).join(), is(3));
        assertThat(bean.getTypedRemote().get().join(), is(6));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set(5).join();
        assertThat(bean.getRemoteNamed().getAndSet(8).join(), is(5));
        assertThat(bean.getTypedRemoteNamed().get().join(), is(8));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set(5).join();
        assertThat(bean.getTypedRemoteUnqualified().getAndSet(8).join(), is(5));
        assertThat(bean.getTypedRemoteNamed().get().join(), is(8));
        }

    @ApplicationScoped
    static class AsyncAtomicIntegerBean
        {
        @Inject
        AsyncAtomicInteger local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        AsyncLocalAtomicInteger typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AsyncAtomicInteger localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        AsyncLocalAtomicInteger typedLocalNamed;

        @Inject
        AsyncLocalAtomicInteger typedLocalUnqualified;

        @Inject
        @Remote
        AsyncAtomicInteger remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AsyncAtomicInteger remoteNamed;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicInteger typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        AsyncRemoteAtomicInteger typedRemoteNamed;

        @Inject
        AsyncRemoteAtomicInteger typedRemoteUnqualified;

        public AsyncAtomicInteger getLocal()
            {
            return local;
            }

        public AsyncAtomicInteger getLocalNamed()
            {
            return localNamed;
            }

        public AsyncAtomicInteger getRemoteNamed()
            {
            return remoteNamed;
            }

        public AsyncAtomicInteger getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicInteger getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicInteger getTypedRemote()
            {
            return typedRemote;
            }

        public AsyncLocalAtomicInteger getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public AsyncLocalAtomicInteger getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public AsyncRemoteAtomicInteger getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public AsyncRemoteAtomicInteger getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
