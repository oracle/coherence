/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AsyncAtomicReferenceProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AsyncAtomicReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.08
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicReferenceProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicReferenceProducer.class)
                                                          .addBeanClass(AsyncAtomicReferenceBean.class));

    @Inject
    private AsyncAtomicReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo").join();
        assertThat(bean.getLocal().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedLocal().get().join(), is("bar"));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set("foo").join();
        assertThat(bean.getLocalNamed().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedLocalNamed().get().join(), is("bar"));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set("foo").join();
        assertThat(bean.getLocalUnqualified().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedLocalNamed().get().join(), is("bar"));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo").join();
        assertThat(bean.getRemote().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedRemote().get().join(), is("bar"));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set("foo").join();
        assertThat(bean.getRemoteNamed().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedRemoteNamed().get().join(), is("bar"));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set("foo").join();
        assertThat(bean.getTypedRemoteUnqualified().getAndSet("bar").join(), is("foo"));
        assertThat(bean.getTypedRemoteNamed().get().join(), is("bar"));
        }

    @ApplicationScoped
    static class AsyncAtomicReferenceBean
        {
        @Inject
        AsyncAtomicReference<String> local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        AsyncLocalAtomicReference<String> typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AsyncAtomicReference<String> localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        AsyncLocalAtomicReference<String> typedLocalNamed;

        @Inject
        AsyncLocalAtomicReference<String> typedLocalUnqualified;

        @Inject
        @Remote
        AsyncAtomicReference<String> remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AsyncAtomicReference<String> remoteNamed;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicReference<String> typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        AsyncRemoteAtomicReference<String> typedRemoteNamed;

        @Inject
        AsyncRemoteAtomicReference<String> typedRemoteUnqualified;

        public AsyncAtomicReference<String> getLocal()
            {
            return local;
            }

        public AsyncAtomicReference<String> getLocalNamed()
            {
            return localNamed;
            }

        public AsyncAtomicReference<String> getRemoteNamed()
            {
            return remoteNamed;
            }

        public AsyncAtomicReference<String> getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicReference<String> getTypedRemote()
            {
            return typedRemote;
            }

        public AsyncLocalAtomicReference<String> getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public AsyncLocalAtomicReference<String> getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public AsyncRemoteAtomicReference<String> getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public AsyncRemoteAtomicReference<String> getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
