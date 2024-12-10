/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicInteger;
import com.oracle.coherence.concurrent.atomic.LocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicInteger;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AtomicIntegerProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AtomicIntegerProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicIntegerProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicIntegerProducer.class)
                                                          .addBeanClass(AtomicIntegerBean.class));

    @Inject
    private AtomicIntegerBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0);
        assertThat(bean.getLocal().getAndSet(2), is(0));
        assertThat(bean.getTypedLocal().get(), is(2));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set(1);
        assertThat(bean.getLocalNamed().getAndSet(4), is(1));
        assertThat(bean.getTypedLocalNamed().get(), is(4));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set(11);
        assertThat(bean.getLocalUnqualified().getAndSet(44), is(11));
        assertThat(bean.getTypedLocalNamed().get(), is(44));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(3);
        assertThat(bean.getRemote().getAndSet(6), is(3));
        assertThat(bean.getTypedRemote().get(), is(6));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set(5);
        assertThat(bean.getRemoteNamed().getAndSet(8), is(5));
        assertThat(bean.getTypedRemoteNamed().get(), is(8));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set(5);
        assertThat(bean.getTypedRemoteUnqualified().getAndSet(8), is(5));
        assertThat(bean.getTypedRemoteNamed().get(), is(8));
        }

    @ApplicationScoped
    static class AtomicIntegerBean
        {
        @Inject
        AtomicInteger local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        LocalAtomicInteger typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AtomicInteger localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        LocalAtomicInteger typedLocalNamed;

        @Inject
        LocalAtomicInteger typedLocalUnqualified;

        @Inject
        @Remote
        AtomicInteger remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AtomicInteger remoteNamed;

        @Inject
        @Name("remote")
        RemoteAtomicInteger typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteAtomicInteger typedRemoteNamed;

        @Inject
        RemoteAtomicInteger typedRemoteUnqualified;

        public AtomicInteger getLocal()
            {
            return local;
            }

        public AtomicInteger getLocalNamed()
            {
            return localNamed;
            }

        public AtomicInteger getRemoteNamed()
            {
            return remoteNamed;
            }

        public AtomicInteger getRemote()
            {
            return remote;
            }

        public LocalAtomicInteger getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicInteger getTypedRemote()
            {
            return typedRemote;
            }

        public LocalAtomicInteger getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public LocalAtomicInteger getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteAtomicInteger getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteAtomicInteger getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
