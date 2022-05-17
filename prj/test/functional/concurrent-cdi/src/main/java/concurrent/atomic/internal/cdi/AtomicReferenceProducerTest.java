/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AtomicReferenceProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AtomicReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.08
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicReferenceProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicReferenceProducer.class)
                                                          .addBeanClass(AtomicReferenceBean.class));

    @Inject
    private AtomicReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo");
        assertThat(bean.getLocal().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedLocal().get(), is("bar"));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set("foo");
        assertThat(bean.getLocalNamed().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedLocalNamed().get(), is("bar"));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set("foo");
        assertThat(bean.getLocalUnqualified().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedLocalNamed().get(), is("bar"));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo");
        assertThat(bean.getRemote().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedRemote().get(), is("bar"));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set("foo");
        assertThat(bean.getRemoteNamed().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedRemoteNamed().get(), is("bar"));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set("foo");
        assertThat(bean.getTypedRemoteUnqualified().getAndSet("bar"), is("foo"));
        assertThat(bean.getTypedRemoteNamed().get(), is("bar"));
        }

    @ApplicationScoped
    static class AtomicReferenceBean
        {
        @Inject
        AtomicReference<String> local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        LocalAtomicReference<String> typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AtomicReference<String> localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        LocalAtomicReference<String> typedLocalNamed;

        @Inject
        LocalAtomicReference<String> typedLocalUnqualified;

        @Inject
        @Remote
        AtomicReference<String> remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AtomicReference<String> remoteNamed;

        @Inject
        @Name("remote")
        RemoteAtomicReference<String> typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteAtomicReference<String> typedRemoteNamed;

        @Inject
        RemoteAtomicReference<String> typedRemoteUnqualified;

        public AtomicReference<String> getLocal()
            {
            return local;
            }

        public AtomicReference<String> getLocalNamed()
            {
            return localNamed;
            }

        public AtomicReference<String> getRemoteNamed()
            {
            return remoteNamed;
            }

        public AtomicReference<String> getRemote()
            {
            return remote;
            }

        public LocalAtomicReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicReference<String> getTypedRemote()
            {
            return typedRemote;
            }

        public LocalAtomicReference<String> getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public LocalAtomicReference<String> getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteAtomicReference<String> getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteAtomicReference<String> getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
