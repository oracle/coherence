/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicLong;
import com.oracle.coherence.concurrent.atomic.LocalAtomicLong;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicLong;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AtomicLongProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AtomicLongProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicLongProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicLongProducer.class)
                                                          .addBeanClass(AtomicLongBean.class));

    @Inject
    private AtomicLongBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(0L);
        assertThat(bean.getLocal().getAndSet(2L), is(0L));
        assertThat(bean.getTypedLocal().get(), is(2L));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set(1L);
        assertThat(bean.getLocalNamed().getAndSet(4L), is(1L));
        assertThat(bean.getTypedLocalNamed().get(), is(4L));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set(11L);
        assertThat(bean.getLocalUnqualified().getAndSet(44L), is(11L));
        assertThat(bean.getTypedLocalNamed().get(), is(44L));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(3L);
        assertThat(bean.getRemote().getAndSet(6L), is(3L));
        assertThat(bean.getTypedRemote().get(), is(6L));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set(5L);
        assertThat(bean.getRemoteNamed().getAndSet(8L), is(5L));
        assertThat(bean.getTypedRemoteNamed().get(), is(8L));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set(5L);
        assertThat(bean.getTypedRemoteUnqualified().getAndSet(8L), is(5L));
        assertThat(bean.getTypedRemoteNamed().get(), is(8L));
        }

    @ApplicationScoped
    static class AtomicLongBean
        {
        @Inject
        AtomicLong local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        LocalAtomicLong typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AtomicLong localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        LocalAtomicLong typedLocalNamed;

        @Inject
        LocalAtomicLong typedLocalUnqualified;

        @Inject
        @Remote
        AtomicLong remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AtomicLong remoteNamed;

        @Inject
        @Name("remote")
        RemoteAtomicLong typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteAtomicLong typedRemoteNamed;

        @Inject
        RemoteAtomicLong typedRemoteUnqualified;

        public AtomicLong getLocal()
            {
            return local;
            }

        public AtomicLong getLocalNamed()
            {
            return localNamed;
            }

        public AtomicLong getRemoteNamed()
            {
            return remoteNamed;
            }

        public AtomicLong getRemote()
            {
            return remote;
            }

        public LocalAtomicLong getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicLong getTypedRemote()
            {
            return typedRemote;
            }

        public LocalAtomicLong getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public LocalAtomicLong getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteAtomicLong getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteAtomicLong getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
