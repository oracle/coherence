/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package net;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.testing.util.ThreadHelper;
import com.tangosol.application.Context;
import com.tangosol.application.LifecycleListener;
import com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultCacheServerServiceMonitorTests
    {
    /**
     * COH-21718 - Ensure the DCS ServiceMonitor thread has stopped.
     */
    @After
    public void cleanup()
        {
        Eventually.assertDeferred(() -> ThreadHelper.getThreadsByPrefix("ServiceMonitor").isEmpty(), is(true));
        }

    /**
     * Ensure a new {@link DefaultCacheServer} instance with a null
     * {@link ConfigurableCacheFactory} instance fails and passes with a
     * non-null CCF.
     */
    @Test(expected = IllegalStateException.class)
    public void testConstructor()
        {
        // setting null DefaultConfigurableCacheFactory
        new DefaultCacheServer(null).startServices();
        }

    /**
     * we have 3 services in the configuration,
     * service 1 has autostart=true
     * service 2 has autostart=false
     * service 3 has autostart=true
     * expectation services 1,3 are started, service 2 is not
     * startServices returns a list with service 1, service 3
     */
    @Test
    public void testStartServices()
            throws Exception
        {
        List<XmlElement> elements = Arrays.asList(
            createServiceXml(true), createServiceXml(false),
            createServiceXml(true, "DistService2"));

        DefaultConfigurableCacheFactory factory = createDCCF(elements);
        Service service1 = mock(Service.class);
        Service service3 = mock(Service.class);

        Map<String, Service> mapServices  = new HashMap();
        mapServices.put("s1", service1);
        mapServices.put("s3", service3);

        when(factory.ensureService(elements.get(0))).thenReturn(service1);
        when(factory.ensureService(elements.get(2))).thenReturn(service3);

        DefaultCacheServer server = new DefaultCacheServer(factory);
        List<Service> list = server.startServices();
        assertEquals(2, list.size());
        assertEquals(service1, list.get(0));
        assertEquals(service3, list.get(1));
        }

    /**
     * we have 2 services in the configuration, all with autostart=true
     * 2nd one throws an exception on start
     * expectation 1st one is started, 2nd has start method invoked
     * startServices throws the exception thrown by server 2
     */
    @Test
    public void testStartServicesException()
            throws Exception
        {
        List<XmlElement> elements = Arrays.asList(
            createServiceXml(true),
            createServiceXml(true, false, "DistService2"));

        DefaultConfigurableCacheFactory factory = createDCCF(elements);

        Service service1 = mock(Service.class);
        @SuppressWarnings({"ThrowableInstanceNeverThrown"}) Throwable
                myException = new RuntimeException("for fun");

        when(factory.ensureService(elements.get(0))).thenReturn(service1);
        when(factory.ensureService(elements.get(1))).thenThrow(myException);

        DefaultCacheServer server = new DefaultCacheServer(factory);
        try
            {
            server.startServices();
            fail();
            }
        catch (RuntimeException e)
            {
            assertSame(myException, e);
            }
        }

    /*
     * we have 3 services in the configuration, all with autostart=true
     * 2nd one throws an exception on start.
     * We override, handleEnsureServiceException
     * expectation 1st one is started, 2nd has start method invoked, and
     * exception is caught by overridden method.
     * startServices returns a list with service 1, service 3
     */
    @Test
    public void testStartServicesExceptionWithOverride()
            throws Exception
        {
        List<XmlElement> elements = Arrays.asList(
            createServiceXml(true), createServiceXml(true, false, "DistService2"),
            createServiceXml(true, "DistService3"));

        DefaultConfigurableCacheFactory factory = createDCCF(elements);

        Service service1 = mock(Service.class);
        Service service3 = mock(Service.class);
        @SuppressWarnings({"ThrowableInstanceNeverThrown"}) final Throwable
            myException = new RuntimeException("for fun");

        when(factory.ensureService(elements.get(0))).thenReturn(service1);
        when(factory.ensureService(elements.get(1))).thenThrow(myException);
        when(factory.ensureService(elements.get(2))).thenReturn(service3);

        DefaultCacheServer server = new DefaultCacheServer(factory)
            {
            @Override protected void handleEnsureServiceException(RuntimeException e)
                {
                assertSame(myException, e);
                }
            };
        List<Service> list = server.startServices();
        assertEquals(2, list.size());
        assertEquals(service1, list.get(0));
        assertEquals(service3, list.get(1));
        }

    /**
     * Verify the ServiceMonitor calls service.isRunning for all autostart
     * services 3 times.
     *
     * @throws InterruptedException
     */
    @Test
    public void testStartAndMonitor()
            throws InterruptedException
        {
        final long cHeartbeatMillis = 1000L;
        final int  cIterations      = 3;

        List<XmlElement> elements = Arrays.asList(
                createServiceXml(true), createServiceXml(false),
                createServiceXml(true, "DistService2"));

        DefaultConfigurableCacheFactory factory = createDCCF(elements);

        Service service1 = mock(Service.class);
        Service service2 = mock(Service.class);

        AtomicInteger cRunning1 = new AtomicInteger();
        AtomicInteger cRunning2 = new AtomicInteger();

        when(service1.isRunning()).thenAnswer(a -> {
            cRunning1.getAndSet(cIterations);
            return true;
        }).thenReturn(true);

        when(service2.isRunning()).thenAnswer(a -> {
            cRunning2.getAndSet(cIterations);
            return true;
        }).thenReturn(true);

        when(factory.ensureService(elements.get(0))).thenReturn(service1);
        when(factory.ensureService(elements.get(2))).thenReturn(service2);

        final DefaultCacheServer server = new DefaultCacheServer(factory);
        Thread t = Base.makeThread(null, new Runnable()
            {
            @Override public void run()
                {
                server.startAndMonitor(cHeartbeatMillis);
                }
            }, "DCS");
        t.start();

        Blocking.sleep(cHeartbeatMillis * cIterations + cHeartbeatMillis);

        server.waitForServiceStart();
        Eventually.assertDeferred(() -> server.isMonitoringServices(), is(true));

        Eventually.assertDeferred(() -> cRunning1.get(), greaterThanOrEqualTo(cIterations));
        Eventually.assertDeferred(() -> cRunning2.get(), greaterThanOrEqualTo(cIterations));

        stopDCS(server);
        Eventually.assertDeferred(() -> server.isMonitoringServices(), is(false));

        t.interrupt();
        verify(service1, atLeast(cIterations)).isRunning();
        verify(service1, atMost(cIterations + 1)).isRunning();
        verify(service2, atLeast(cIterations)).isRunning();
        verify(service2, atMost(cIterations + 1)).isRunning();
        }


    // ----- ECCF tests -----------------------------------------------------

    /**
     * Verify the ServiceMonitor is set on the ECCF instance followed by
     * activate and startServices calls. It is the responsibility of ECCF
     * to register the services it started with the ServiceMonitor.
     *
     * @throws InterruptedException
     */
    @Test
    public void testStartAndMonitorECCF()
            throws InterruptedException
        {
        final long cHeartbeatMillis = 1000L;

        ExtensibleConfigurableCacheFactory eccf = mock(ExtensibleConfigurableCacheFactory.class);

        when(eccf.getServiceMap()).thenReturn(new HashMap<Service, String>()
            {{
            put(mock(Service.class), CacheService.TYPE_DISTRIBUTED);
            put(mock(Service.class), "DistService2");
            }});

        final DefaultCacheServer server = new DefaultCacheServer(eccf);

        Thread t = Base.makeThread(null, new Runnable()
            {
            @Override public void run()
                {
                server.startAndMonitor(cHeartbeatMillis);
                }
            }, "DCS");
        t.start();

        server.waitForServiceStart();

        stopDCS(server);
        t.interrupt();

        verify(eccf).activate();
        }

    /*
     * Test startDaemon() monitors and restarts services.
     */
    @Test
    public void testStartDaemonMonitorsServices()
        {
        ClassLoader              cl           = new ClassLoader(this.getClass().getClassLoader()) {};
        ConfigurableCacheFactory cacheFactory =
                CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory("coherence-cache-config.xml", cl);

        // this will ensure the services are monitored
        DefaultCacheServer cacheServer = new DefaultCacheServer(cacheFactory);
        cacheServer.startDaemon(DefaultCacheServer.DEFAULT_WAIT_MILLIS);
        Eventually.assertThat(invoking(cacheServer).isMonitorStopped(), is(false));

        NamedCache cache = cacheFactory.ensureCache("dist-cache", cl);

        cache.put("A", 1);
        assertEquals(1, cache.get("A"));

        SafeDistributedCacheService service        = (SafeDistributedCacheService) cache.getCacheService();
        com.tangosol.util.Service   runningService = service.getRunningService();

        runningService.stop();
        Eventually.assertThat(invoking(service).isRunning(), is(false));

        assertEquals(null, cache.get("A"));

        Eventually.assertThat(invoking(service).getRunningService().isRunning(), is(true));
        Service serviceAfterRestart = service.getRunningCacheService();
        Assert.assertNotEquals(runningService, serviceAfterRestart);

        cache = ((CacheService) serviceAfterRestart).ensureCache("dist-cache", cl);
        cache.put("A", 1);
        assertEquals(1, cache.get("A"));

        stopDCS(cacheServer);
        }

    @Test
    public void shouldReceiveLifecycleEvents() throws Exception
        {
        ExtensibleConfigurableCacheFactory eccf = mock(ExtensibleConfigurableCacheFactory.class);
        when(eccf.getServiceMap()).thenReturn(Collections.emptyMap());

        LifecycleListener l1 = mock(LifecycleListener.class);
        LifecycleListener l2 = mock(LifecycleListener.class);
        LifecycleListener l3 = mock(LifecycleListener.class);

        DefaultCacheServer server = new DefaultCacheServer(eccf);
        server.addLifecycleListener(l1);
        server.addLifecycleListener(l2);
        server.addLifecycleListener(l3);
        // l3 is now removed so should not receive any events
        server.removeLifecycleListener(l3);

        // start DCS - l1 and l2 should get events
        server.startDaemon(5000L);

        // l3 is now removed so should not receive any stop events
        server.removeLifecycleListener(l2);

        // stop DCS - l1 should get events
        server.shutdownServer();

        InOrder inOrder = inOrder(l1, l2, l3);
        inOrder.verify(l1).preStart(any(Context.class));
        inOrder.verify(l2).preStart(any(Context.class));
        inOrder.verify(l1).postStart(any(Context.class));
        inOrder.verify(l2).postStart(any(Context.class));
        inOrder.verify(l1).preStop(any(Context.class));
        inOrder.verify(l1).postStop(any(Context.class));
        verifyNoInteractions(l3);
        verifyNoMoreInteractions(l1, l2);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create an XmlElement that is populated sufficiently for DCS.
     *
     * @param fAutoStart  the value of autostart in the service XML
     *
     * @return an XmlElement that is populated sufficiently for DCS
     */
    protected XmlElement createServiceXml(boolean fAutoStart)
        {
        return createServiceXml(fAutoStart, true, "");
        }

    /**
     * Create an XmlElement that is populated sufficiently for DCS.
     *
     * @param fAutoStart    the value of autostart in the service XML
     * @param sServiceName  the value of service-name in the service XML
     *
     * @return an XmlElement that is populated sufficiently for DCS
     */
    protected XmlElement createServiceXml(boolean fAutoStart, String sServiceName)
        {
        return createServiceXml(fAutoStart, true, sServiceName);
        }

    /**
     * Create an XmlElement that is populated sufficiently for DCS.
     *
     * @param fAutoStart          the value of autostart in the service XML
     * @param fExpectServiceName  whether "service-name" element will be called for
     * @param sServiceName        the value of service-name in the service XML
     *
     * @return an XmlElement that is populated sufficiently for DCS
     */
    protected XmlElement createServiceXml(boolean fAutoStart, boolean fExpectServiceName, String sServiceName)
        {
        XmlElement xmlElement     = mock(XmlElement.class);
        XmlElement xmlAutoStart   = mock(XmlElement.class);
        when(xmlElement.getSafeElement("autostart")).thenReturn(xmlAutoStart);
        when(xmlAutoStart.getBoolean()).thenReturn(fAutoStart);

        if (fAutoStart && fExpectServiceName)
            {
            XmlElement xmlServiceName = mock(XmlElement.class);

            when(xmlElement.getName()).thenReturn("distributed-scheme");
            when(xmlElement.getSafeElement("service-name")).thenReturn(xmlServiceName);
            when(xmlServiceName.getString()).thenReturn(sServiceName);
            }
        return xmlElement;
        }

    /**
     * Create a DefaultConfigurableCacheFactory instance with the
     * cache-schemes referring to the list of XmlElements provided.
     *
     * @param elements  list of schemes
     *
     * @return a DefaultConfigurableCacheFactory instance referring to the
     *         given schemes
     */
    protected DefaultConfigurableCacheFactory createDCCF(List elements)
        {
        XmlElement schemes = mock(XmlElement.class);
        XmlElement config  = mock(XmlElement.class);

        DefaultConfigurableCacheFactory factory = mock(DefaultConfigurableCacheFactory.class);

        when(factory.getConfig()).thenReturn(config);
        when(config.getSafeElement("caching-schemes")).thenReturn(schemes);
        when(schemes.getElementList()).thenReturn(elements);

        return factory;
        }

    /**
     * Stop DefaultCacheServer ensuring that the ServiceMonitor is fully stopped.
     *
     * @param server  the DefaultCacheServer
     */
    protected void stopDCS(DefaultCacheServer server)
        {
        server.shutdownServer();
        Eventually.assertDeferred(server::isMonitorStopped, is(true));
        }
    }
