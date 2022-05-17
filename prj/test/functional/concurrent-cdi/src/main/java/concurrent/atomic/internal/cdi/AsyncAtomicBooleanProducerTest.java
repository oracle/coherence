/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicBoolean;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import com.oracle.coherence.concurrent.atomic.internal.cdi.AsyncAtomicBooleanProducer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link AsyncAtomicBooleanProducer}.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncAtomicBooleanProducerTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicBooleanProducer.class)
                                                          .addBeanClass(AsyncAtomicBooleanBean.class));

    @Inject
    private AsyncAtomicBooleanBean bean;

    @Test
    void testLocalInjection()
        {
        bean.getLocal().set(true).join();
        assertThat(bean.getLocal().getAndSet(false).join(), is(true));
        assertThat(bean.getTypedLocal().get().join(), is(false));
        }

    @Test
    void testLocalNamedInjection()
        {
        bean.getLocalNamed().set(true).join();
        assertThat(bean.getLocalNamed().getAndSet(false).join(), is(true));
        assertThat(bean.getTypedLocalNamed().get().join(), is(false));
        }

    @Test
    void testLocalUnqualifiedInjection()
        {
        bean.getLocalUnqualified().set(true).join();
        assertThat(bean.getLocalUnqualified().getAndSet(false).join(), is(true));
        assertThat(bean.getTypedLocalNamed().get().join(), is(false));
        }

    @Test
    void testRemoteInjection()
        {
        bean.getRemote().set(true).join();
        assertThat(bean.getRemote().getAndSet(false).join(), is(true));
        assertThat(bean.getTypedRemote().get().join(), is(false));
        }

    @Test
    void testRemoteNamedInjection()
        {
        bean.getRemoteNamed().set(true).join();
        assertThat(bean.getRemoteNamed().getAndSet(false).join(), is(true));
        assertThat(bean.getTypedRemoteNamed().get().join(), is(false));
        }

    @Test
    void testRemoteUnqualifiedInjection()
        {
        bean.getTypedRemoteUnqualified().set(true).join();
        assertThat(bean.getTypedRemoteUnqualified().getAndSet(false).join(), is(true));
        assertThat(bean.getTypedRemoteNamed().get().join(), is(false));
        }

    @ApplicationScoped
    static class AsyncAtomicBooleanBean
        {
        @Inject
        AsyncAtomicBoolean local;  // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("local")
        AsyncLocalAtomicBoolean typedLocal;

        @Inject
        @Name("typedLocalUnqualified")
        AsyncAtomicBoolean localNamed; // IntelliJ highlights this as an ambiguous dependency, but it is not as the test passes

        @Inject
        @Name("typedLocalUnqualified")
        AsyncLocalAtomicBoolean typedLocalNamed;

        @Inject
        AsyncLocalAtomicBoolean typedLocalUnqualified;

        @Inject
        @Remote
        AsyncAtomicBoolean remote;

        @Inject
        @Name("typedRemoteUnqualified")
        @Remote
        AsyncAtomicBoolean remoteNamed;

        @Inject
        @Name("remote")
        AsyncRemoteAtomicBoolean typedRemote;

        @Inject
        @Name("typedRemoteUnqualified")
        AsyncRemoteAtomicBoolean typedRemoteNamed;

        @Inject
        AsyncRemoteAtomicBoolean typedRemoteUnqualified;

        public AsyncAtomicBoolean getLocal()
            {
            return local;
            }

        public AsyncAtomicBoolean getLocalNamed()
            {
            return localNamed;
            }

        public AsyncAtomicBoolean getRemoteNamed()
            {
            return remoteNamed;
            }

        public AsyncAtomicBoolean getRemote()
            {
            return remote;
            }

        public AsyncLocalAtomicBoolean getTypedLocal()
            {
            return typedLocal;
            }

        public AsyncRemoteAtomicBoolean getTypedRemote()
            {
            return typedRemote;
            }

        public AsyncLocalAtomicBoolean getLocalUnqualified()
            {
            return typedLocalUnqualified;
            }

        public AsyncLocalAtomicBoolean getTypedLocalNamed()
            {
            return typedLocalNamed;
            }

        public AsyncRemoteAtomicBoolean getTypedRemoteNamed()
            {
            return typedRemoteNamed;
            }

        public AsyncRemoteAtomicBoolean getTypedRemoteUnqualified()
            {
            return typedRemoteUnqualified;
            }
        }
    }
