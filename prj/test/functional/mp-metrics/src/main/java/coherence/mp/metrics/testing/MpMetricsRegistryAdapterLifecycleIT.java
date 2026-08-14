/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.mp.metrics.testing;

import com.oracle.coherence.mp.metrics.MpMetricsRegistryAdapter;

import com.tangosol.internal.metrics.BaseMBeanMetric;
import com.tangosol.net.metrics.MBeanMetric;

import jakarta.annotation.Priority;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;

import jakarta.enterprise.event.Observes;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

import jakarta.inject.Inject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import java.util.function.Supplier;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import org.eclipse.microprofile.metrics.annotation.RegistryScope;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests the CDI lifecycle behavior of {@link MpMetricsRegistryAdapter}.
 *
 * @author Vaso Putica  2026.07.24
 * @since 26.1.1.0
 */
@ExtendWith(WeldJunit5Extension.class)
class MpMetricsRegistryAdapterLifecycleIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(MpMetricsRegistryAdapter.class)
                                                          .addBeanClass(RegistryProducer.class)
                                                          .addBeanClass(LifecycleProbe.class));

    @Inject
    private LifecycleProbe probe;

    @Inject
    private RegistryProducer registries;

    @Test
    void shouldDeferRegistryResolutionAndMetricOperations()
        {
        assertThat(probe.getResolutionCountAtPriority1500(), is(0));
        assertThat(probe.getGaugeCountAtPriority1500(), is(0));
        assertThat(probe.getConcurrentFailure(), is(nullValue()));
        assertThat(probe.isConcurrentRegistrationComplete(), is(true));

        assertThat(registries.getVendorResolutionCount(), is(1));
        assertThat(registries.getApplicationResolutionCount(), is(1));
        assertThat(registries.getVendorGaugeNames(),
                containsInAnyOrder("queued-two", "concurrent"));
        assertThat(registries.getVendorOperations(),
                contains("register:queued-one", "remove:queued-one",
                         "register:queued-two", "register:concurrent"));
        }

    // ---- inner class: LifecycleProbe ------------------------------------

    /**
     * Observes application initialization around the adapter's priority-2000
     * lifecycle boundary.
     */
    @ApplicationScoped
    static class LifecycleProbe
        {
        void queueOperations(
                @Observes @Priority(10) @Initialized(ApplicationScoped.class) Object event,
                MpMetricsRegistryAdapter adapter)
            {
            adapter.register(metric("queued-one"));
            adapter.remove(metric("queued-one").getIdentifier());
            adapter.register(metric("queued-two"));
            }

        void beforeAdapterInitialization(
                @Observes @Priority(1500) @Initialized(ApplicationScoped.class) Object event,
                MpMetricsRegistryAdapter adapter,
                RegistryProducer registries)
            {
            m_cResolutionAtPriority1500 = registries.getTotalResolutionCount();
            m_cGaugesAtPriority1500     = registries.getVendorGaugeCount();

            m_thread = new Thread(() ->
                {
                try
                    {
                    if (!registries.awaitVendorResolution())
                        {
                        throw new IllegalStateException("Timed out waiting for vendor registry resolution");
                        }

                    registries.signalConcurrentRegistration();
                    adapter.register(metric("concurrent"));
                    }
                catch (Throwable t)
                    {
                    m_concurrentFailure.set(t);
                    }
                }, "mp-metrics-concurrent-registration");
            m_thread.setDaemon(true);
            m_thread.start();
            }

        void afterAdapterInitialization(
                @Observes @Priority(2001) @Initialized(ApplicationScoped.class) Object event)
            {
            try
                {
                m_thread.join(TimeUnit.SECONDS.toMillis(10));
                m_fConcurrentRegistrationComplete = !m_thread.isAlive();
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                m_concurrentFailure.set(e);
                }
            }

        int getResolutionCountAtPriority1500()
            {
            return m_cResolutionAtPriority1500;
            }

        int getGaugeCountAtPriority1500()
            {
            return m_cGaugesAtPriority1500;
            }

        Throwable getConcurrentFailure()
            {
            return m_concurrentFailure.get();
            }

        boolean isConcurrentRegistrationComplete()
            {
            return m_fConcurrentRegistrationComplete;
            }

        private volatile Thread m_thread;

        private volatile int m_cResolutionAtPriority1500;

        private volatile int m_cGaugesAtPriority1500;

        private volatile boolean m_fConcurrentRegistrationComplete;

        private final AtomicReference<Throwable> m_concurrentFailure = new AtomicReference<>();
        }

    // ---- inner class: RegistryProducer ----------------------------------

    /**
     * Produces recording metric registries and coordinates a registration
     * attempted while the adapter is initializing.
     */
    @ApplicationScoped
    static class RegistryProducer
        {
        @Produces
        @RegistryScope
        MetricRegistry getRegistry(InjectionPoint injectionPoint)
            {
            RegistryScope annotation = injectionPoint.getAnnotated().getAnnotation(RegistryScope.class);
            String        sScope      = annotation.scope();

            if (MetricRegistry.VENDOR_SCOPE.equals(sScope))
                {
                f_cVendorResolutions.incrementAndGet();
                f_vendorResolutionStarted.countDown();
                await(f_concurrentRegistrationStarted);
                return f_vendorRegistry;
                }

            if (MetricRegistry.APPLICATION_SCOPE.equals(sScope))
                {
                f_cApplicationResolutions.incrementAndGet();
                return f_applicationRegistry;
                }

            throw new IllegalArgumentException("Unexpected registry scope " + sScope);
            }

        int getVendorResolutionCount()
            {
            return f_cVendorResolutions.get();
            }

        int getApplicationResolutionCount()
            {
            return f_cApplicationResolutions.get();
            }

        int getTotalResolutionCount()
            {
            return getVendorResolutionCount() + getApplicationResolutionCount();
            }

        int getVendorGaugeCount()
            {
            return f_vendorHandler.getGauges().size();
            }

        Iterable<String> getVendorGaugeNames()
            {
            return f_vendorHandler.getGauges()
                    .keySet()
                    .stream()
                    .map(MetricID::getName)
                    .toList();
            }

        Iterable<String> getVendorOperations()
            {
            return f_vendorHandler.getOperations();
            }

        boolean awaitVendorResolution()
                throws InterruptedException
            {
            return f_vendorResolutionStarted.await(10, TimeUnit.SECONDS);
            }

        void signalConcurrentRegistration()
            {
            f_concurrentRegistrationStarted.countDown();
            }

        private static void await(CountDownLatch latch)
            {
            try
                {
                if (!latch.await(10, TimeUnit.SECONDS))
                    {
                    throw new IllegalStateException("Timed out waiting for concurrent registration");
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting for concurrent registration", e);
                }
            }

        private final RecordingRegistry f_vendorHandler = new RecordingRegistry();

        private final MetricRegistry f_vendorRegistry = f_vendorHandler.createProxy();

        private final MetricRegistry f_applicationRegistry = new RecordingRegistry().createProxy();

        private final AtomicInteger f_cVendorResolutions = new AtomicInteger();

        private final AtomicInteger f_cApplicationResolutions = new AtomicInteger();

        private final CountDownLatch f_vendorResolutionStarted = new CountDownLatch(1);

        private final CountDownLatch f_concurrentRegistrationStarted = new CountDownLatch(1);
        }

    // ---- inner class: RecordingRegistry ---------------------------------

    /**
     * A minimal {@link MetricRegistry} proxy that records gauge mutations.
     */
    static class RecordingRegistry
            implements InvocationHandler
        {
        MetricRegistry createProxy()
            {
            return (MetricRegistry) Proxy.newProxyInstance(
                    MetricRegistry.class.getClassLoader(),
                    new Class<?>[] {MetricRegistry.class},
                    this);
            }

        SortedMap<MetricID, Gauge> getGauges()
            {
            return f_gauges;
            }

        List<String> getOperations()
            {
            return f_operations;
            }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args)
            {
            switch (method.getName())
                {
                case "gauge":
                    Metadata         metadata = (Metadata) args[0];
                    Supplier<Number> supplier = (Supplier<Number>) args[1];
                    Tag[]            tags     = (Tag[]) args[2];
                    Gauge<Number>    gauge    = supplier::get;

                    f_operations.add("register:" + metadata.getName());
                    f_gauges.put(new MetricID(metadata.getName(), tags), gauge);
                    return gauge;

                case "getGauges":
                    return f_gauges;

                case "remove":
                    MetricID id = (MetricID) args[0];
                    f_operations.add("remove:" + id.getName());
                    return f_gauges.remove(id) != null;

                case "toString":
                    return "RecordingRegistry";

                default:
                    throw new UnsupportedOperationException(method.toString());
                }
            }

        private final SortedMap<MetricID, Gauge> f_gauges = new TreeMap<>();

        private final List<String> f_operations = new ArrayList<>();
        }

    // ---- helpers ---------------------------------------------------------

    private static MBeanMetric metric(String sName)
        {
        MBeanMetric.Identifier identifier = new MBeanMetric.Identifier(
                MBeanMetric.Scope.VENDOR, sName, Collections.emptyMap());
        return new TestMetric(identifier);
        }

    // ---- inner class: TestMetric ----------------------------------------

    /**
     * A test MBean metric.
     */
    static class TestMetric
            extends BaseMBeanMetric
        {
        TestMetric(Identifier identifier)
            {
            super(identifier, "Coherence:type=Test", "test metric");
            }

        @Override
        public Object getValue()
            {
            return 1;
            }
        }
    }
