/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import com.oracle.coherence.common.base.Blocking;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class DefaultCacheServerTest
    {
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
              int  cIterations      = 3;

        List<XmlElement> elements = Arrays.asList(
                createServiceXml(true), createServiceXml(false),
                createServiceXml(true, "DistService2"));

        DefaultConfigurableCacheFactory factory = createDCCF(elements);

        Service service1 = mock(Service.class);
        Service service3 = mock(Service.class);

        when(factory.ensureService(elements.get(0))).thenReturn(service1);
        when(factory.ensureService(elements.get(2))).thenReturn(service3);
        when(service1.isRunning()).thenReturn(true);
        when(service3.isRunning()).thenReturn(true);

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

        ServiceMonitor mon = server.m_serviceMon;
        assertTrue(mon.isMonitoring());
        stopDCS(server);
        assertFalse(mon.isMonitoring());

        t.interrupt();
        verify(service1, atLeast(cIterations)).isRunning();
        verify(service1, atMost(cIterations + 1)).isRunning();
        verify(service3, atLeast(cIterations)).isRunning();
        verify(service3, atMost(cIterations + 1)).isRunning();
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
              int  cIterations      = 3;

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

        Blocking.sleep(cHeartbeatMillis * cIterations + cHeartbeatMillis);

        stopDCS(server);
        t.interrupt();

        verify(eccf).activate();
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
        ServiceMonitor monitor       = server.m_serviceMon;
        Thread         threadMonitor = monitor == null ? null : monitor.getThread();

        server.shutdownServer();

        // the ServiceMonitor thread may be alive briefly after DCS.shutdownServer() returns
        if (threadMonitor != null)
            {
            Eventually.assertDeferred(() -> threadMonitor.isAlive(), is(false));
            }
        }
    }
