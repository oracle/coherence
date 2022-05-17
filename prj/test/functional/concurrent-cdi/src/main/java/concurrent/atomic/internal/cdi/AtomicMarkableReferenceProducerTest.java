/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicMarkableReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AtomicMarkableReferenceProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AtomicMarkableReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicMarkableReferenceProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AtomicMarkableReferenceProducer.class)
                                                          .addBeanClass(AtomicMarkableReferenceBean.class));

    @Inject
    private AtomicMarkableReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo", true);
        assertThat(bean.getLocal().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedLocal().getReference(), is("bar"));
        assertThat(bean.getTypedLocal().isMarked(), is(false));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set("foo", true);
        assertThat(bean.getLocalNamed().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedLocalNamed().getReference(), is("bar"));
        assertThat(bean.getTypedLocalNamed().isMarked(), is(false));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set("foo", true);
        assertThat(bean.getLocalUnqualified().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedLocalNamed().getReference(), is("bar"));
        assertThat(bean.getTypedLocalNamed().isMarked(), is(false));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo", true);
        assertThat(bean.getRemote().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedRemote().getReference(), is("bar"));
        assertThat(bean.getTypedRemote().isMarked(), is(false));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set("foo", true);
        assertThat(bean.getRemoteNamed().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedRemoteNamed().getReference(), is("bar"));
        assertThat(bean.getTypedRemoteNamed().isMarked(), is(false));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set("foo", true);
        assertThat(bean.getTypedRemoteUnqualified().compareAndSet("foo", "bar", true, false), is(true));
        assertThat(bean.getTypedRemoteNamed().getReference(), is("bar"));
        assertThat(bean.getTypedRemoteNamed().isMarked(), is(false));
        }

    @ApplicationScoped
    static class AtomicMarkableReferenceBean
        {
        @Inject
        AtomicMarkableReference<String> local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        LocalAtomicMarkableReference<String> typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AtomicMarkableReference<String> localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        LocalAtomicMarkableReference<String> typedLocalNamed;

        @Inject
        LocalAtomicMarkableReference<String> typedLocalUnqualified;

        @Inject
        @Remote
        AtomicMarkableReference<String> remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AtomicMarkableReference<String> remoteNamed;

        @Inject
        @Name("remote")
        RemoteAtomicMarkableReference<String> typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        RemoteAtomicMarkableReference<String> typedRemoteNamed;

        @Inject
        RemoteAtomicMarkableReference<String> typedRemoteUnqualified;

        public AtomicMarkableReference<String> getLocal()
            {
            return local;
            }

        public AtomicMarkableReference<String> getLocalNamed()
            {
            return localNamed;
            }

        public AtomicMarkableReference<String> getRemoteNamed()
            {
            return remoteNamed;
            }

        public AtomicMarkableReference<String> getRemote()
            {
            return remote;
            }

        public LocalAtomicMarkableReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public RemoteAtomicMarkableReference<String> getTypedRemote()
            {
            return typedRemote;
            }

        public LocalAtomicMarkableReference<String> getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public LocalAtomicMarkableReference<String> getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public RemoteAtomicMarkableReference<String> getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public RemoteAtomicMarkableReference<String> getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
