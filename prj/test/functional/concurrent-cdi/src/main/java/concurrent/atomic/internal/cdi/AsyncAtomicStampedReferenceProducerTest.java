/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicStampedReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AsyncAtomicStampedReferenceProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AsyncAtomicStampedReferenceProducer}.
 *
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicStampedReferenceProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicStampedReferenceProducer.class)
                                                          .addBeanClass(AsyncAtomicStampedReferenceBean.class));

    @Inject
    private AsyncAtomicStampedReferenceBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set("foo", 1).join();
        assertThat(bean.getLocal().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedLocal().getReference().join(), is("bar"));
        assertThat(bean.getTypedLocal().getStamp().join(), is(2));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set("foo", 1).join();
        assertThat(bean.getLocalNamed().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedLocalNamed().getReference().join(), is("bar"));
        assertThat(bean.getTypedLocalNamed().getStamp().join(), is(2));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set("foo", 1).join();
        assertThat(bean.getLocalUnqualified().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedLocalNamed().getReference().join(), is("bar"));
        assertThat(bean.getTypedLocalNamed().getStamp().join(), is(2));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set("foo", 1).join();
        assertThat(bean.getRemote().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedRemote().getReference().join(), is("bar"));
        assertThat(bean.getTypedRemote().getStamp().join(), is(2));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set("foo", 1).join();
        assertThat(bean.getRemoteNamed().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedRemoteNamed().getReference().join(), is("bar"));
        assertThat(bean.getTypedRemoteNamed().getStamp().join(), is(2));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set("foo", 1).join();
        assertThat(bean.getTypedRemoteUnqualified().compareAndSet("foo", "bar", 1, 2).join(), is(true));
        assertThat(bean.getTypedRemoteNamed().getReference().join(), is("bar"));
        assertThat(bean.getTypedRemoteNamed().getStamp().join(), is(2));
        }

    @ApplicationScoped
    static class AsyncAtomicStampedReferenceBean
        {
        @Inject
        AsyncAtomicStampedReference<String> local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        AsyncLocalAtomicStampedReference<String> typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AsyncAtomicStampedReference<String> localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        AsyncLocalAtomicStampedReference<String> typedLocalNamed;

        @Inject
        AsyncLocalAtomicStampedReference<String> typedLocalUnqualified;

        @Inject
        @Remote
        AsyncAtomicStampedReference<String> remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AsyncAtomicStampedReference<String> remoteNamed;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicStampedReference<String> typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        AsyncRemoteAtomicStampedReference<String> typedRemoteNamed;

        @Inject
        AsyncRemoteAtomicStampedReference<String> typedRemoteUnqualified;

        public AsyncAtomicStampedReference<String> getLocal()
            {
            return local;
            }

        public AsyncAtomicStampedReference<String> getLocalNamed()
            {
            return localNamed;
            }

        public AsyncAtomicStampedReference<String> getRemoteNamed()
            {
            return remoteNamed;
            }

        public AsyncAtomicStampedReference<String> getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicStampedReference<String> getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicStampedReference<String> getTypedRemote()
            {
            return typedRemote;
            }

        public AsyncLocalAtomicStampedReference<String> getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public AsyncLocalAtomicStampedReference<String> getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public AsyncRemoteAtomicStampedReference<String> getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public AsyncRemoteAtomicStampedReference<String> getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
