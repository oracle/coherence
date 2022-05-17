/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicBoolean;
import com.oracle.coherence.concurrent.atomic.LocalAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicBoolean;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AtomicBooleanProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AtomicBooleanProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicBooleanProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicBooleanProducer.class)
                                                          .addBeanClass(AtomicBooleanBean.class));

    @Inject
    private AtomicBooleanBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(true);
        assertThat(bean.getLocal().getAndSet(false), is(true));
        assertThat(bean.getTypedLocal().get(), is(false));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set(true);
        assertThat(bean.getLocalNamed().getAndSet(false), is(true));
        assertThat(bean.getTypedLocalNamed().get(), is(false));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set(true);
        assertThat(bean.getLocalUnqualified().getAndSet(false), is(true));
        assertThat(bean.getTypedLocalNamed().get(), is(false));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(true);
        assertThat(bean.getRemote().getAndSet(false), is(true));
        assertThat(bean.getTypedRemote().get(), is(false));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set(true);
        assertThat(bean.getRemoteNamed().getAndSet(false), is(true));
        assertThat(bean.getTypedRemoteNamed().get(), is(false));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set(true);
        assertThat(bean.getTypedRemoteUnqualified().getAndSet(false), is(true));
        assertThat(bean.getTypedRemoteNamed().get(), is(false));
        }

    @ApplicationScoped
    static class AtomicBooleanBean
        {
        @Inject
        AtomicBoolean local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        LocalAtomicBoolean typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AtomicBoolean localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        LocalAtomicBoolean typedLocalNamed;

        @Inject
        LocalAtomicBoolean typedLocalUnqualified;

        @Inject
        @Remote
        AtomicBoolean remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AtomicBoolean remoteNamed;

        @Inject
        @Name("remote")
        RemoteAtomicBoolean typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteAtomicBoolean typedRemoteNamed;

        @Inject
        RemoteAtomicBoolean typedRemoteUnqualified;

        public AtomicBoolean getLocal()
            {
            return local;
            }

        public AtomicBoolean getLocalNamed()
            {
            return localNamed;
            }

        public AtomicBoolean getRemoteNamed()
            {
            return remoteNamed;
            }

        public AtomicBoolean getRemote()
            {
            return remote;
            }

        public LocalAtomicBoolean getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicBoolean getTypedRemote()
            {
            return typedRemote;
            }

        public LocalAtomicBoolean getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public LocalAtomicBoolean getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteAtomicBoolean getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteAtomicBoolean getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
