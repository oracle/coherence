/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.io.pof.reflect.internal.InvocationStrategies.FieldInvocationStrategy;
import com.tangosol.io.pof.reflect.internal.InvocationStrategy;

import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;

import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.internal.InterceptorManager;
import com.tangosol.net.events.internal.NamedEventInterceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;


import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests the {@link DefaultConfigurableCacheFactory} class.
 *
 * @author hr  2011.11.15
 * @author bo  2012.05.21
 *
 * @since Coherence 12.1.2
 */
public class ExtensibleConfigurableCacheFactoryTest
    {
    @Test
    public void testEnsureScopedService()
        {
        String     sServiceName = "distributed-service";
        String     sScopeName   = "oracle.coherence.myscope";

        XmlElement xmlConfig    = XmlHelper.loadFileOrResource("net/coherence-cache-config.xml", "cache config");

        // insert a custom scope name into the standard cache config for the test
        XmlElement xmlScopeName = new SimpleElement("scope-name", sScopeName);

        xmlConfig.getElementList().add(0, xmlScopeName);

        Dependencies                       dependencies = DependenciesHelper.newInstance(xmlConfig);
        ExtensibleConfigurableCacheFactory eccf         = new ExtensibleConfigurableCacheFactory(dependencies);

        assertThat(sScopeName, is(eccf.getScopeName()));

        ServiceScheme scheme = eccf.getCacheConfig().findSchemeByServiceName(sServiceName);

        assertThat(scheme, not(nullValue()));

        String actualServiceName = scheme.getScopedServiceName();

        assertEquals(sScopeName + ":" + sServiceName, actualServiceName);
        }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterceptorParsing()
            throws Exception
        {
        PartitionedCacheDispatcher bmd = mock(PartitionedCacheDispatcher.class);
        when(bmd.getServiceName()).thenReturn("ear:DistributedCache");

        String sXml = LOCAL_LOCATION
                     +  "    <scope-name>ear</scope-name>\n"
                     +  "    <caching-scheme-mapping>\n"
                     +  "        <cache-mapping>\n"
                     +  "            <cache-name>dist-*</cache-name>\n"
                     +  "            <scheme-name>example-distributed</scheme-name>\n"
                     +  "            <interceptors>\n"
                     +  "                <interceptor>\n"
                     +  "                    <name>Knuth</name>\n"
                     +  "                    <instance>\n"
                     +  "                        <class-name>com.tangosol.net.ExtensibleConfigurableCacheFactoryTest$AlgoInterceptor</class-name>\n"
                     +  "                    </instance>\n"
                     +  "                </interceptor>\n"
                     +  "            </interceptors>\n"
                     +  "        </cache-mapping>\n"
                     +  "    </caching-scheme-mapping>\n"
                     +  "    <caching-schemes>\n"
                     +  "        <distributed-scheme>\n"
                     +  "            <scheme-name>example-distributed</scheme-name>\n"
                     +  "            <interceptors>\n"
                     +  "                <interceptor>\n"
                     +  "                    <name>Sedgewick</name>\n"
                     +  "                    <instance>\n"
                     +  "                        <class-name>com.tangosol.net.ExtensibleConfigurableCacheFactoryTest$AlgoInterceptor</class-name>\n"
                     +  "                    </instance>\n"
                     +  "                </interceptor>\n"
                     +  "            </interceptors>\n"
                     +  "        </distributed-scheme>\n"
                     +  "        <distributed-scheme>\n"
                     +  "            <scheme-name>another-distributed</scheme-name>\n"
                     +  "            <service-name>AnotherDistributed</service-name>\n"
                     +  "            <interceptors>\n"
                     +  "                <interceptor>\n"
                     +  "                    <instance>\n"
                     +  "                        <class-name>com.tangosol.net.ExtensibleConfigurableCacheFactoryTest$AlgoInterceptor</class-name>\n"
                     +  "                        <init-params>\n"
                     +  "                            <init-param>\n"
                     +  "                                <param-type>int</param-type>\n"
                     +  "                                <param-value>100</param-value>\n"
                     +  "                            </init-param>\n"
                     +  "                        </init-params>\n"
                     +  "                    </instance>\n"
                     +  "                </interceptor>\n"
                     +  "            </interceptors>\n"
                     +  "        </distributed-scheme>\n"
                     +  "    </caching-schemes>\n"
                     +  "</cache-config>";

        Dependencies dependencies = DependenciesHelper.newInstance(new SimpleParser(true).parseXml(sXml));
        ConfigurableCacheFactory ccf = new ExtensibleConfigurableCacheFactory(dependencies);

        InterceptorRegistry registry = ccf.getInterceptorRegistry();
        InterceptorManager  manager  =
            dependencies.getResourceRegistry().getResource(InterceptorManager.class);

        manager.instantiateServiceInterceptors("ear:DistributedCache");
        manager.instantiateServiceInterceptors("ear:AnotherDistributed");
        manager.instantiateCacheInterceptors("dist-1", "ear:DistributedCache");

        Map<String, EventInterceptor> mapIncptrsRaw   = getRawInterceptors(registry);
        AlgoInterceptor sedgewick = (AlgoInterceptor) registry.getEventInterceptor("Sedgewick");

        assertNotNull(registry.getEventInterceptor("Knuth"));
        assertNotNull(sedgewick);
        assertNotNull(registry.getEventInterceptor("Algo"));

        assertThat(sedgewick, notNullValue());

        NamedEventInterceptor<EntryEvent<?, ?>> incptrNamed = (NamedEventInterceptor) mapIncptrsRaw.get("Sedgewick");

        assertThat("Service name should be either null (DCCF) or the default value of 'DistributedCache' (ECCF)",
                   incptrNamed.getServiceName(), anyOf(nullValue(), is("ear:DistributedCache")));
        assertThat(incptrNamed.getEventTypes(), containsInAnyOrder(Type.values()));
        assertThat(incptrNamed.getCacheName(), nullValue());
        assertTrue(incptrNamed.isAcceptable(bmd));
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInterceptorParsingError()
            throws Exception
        {
        ServiceInfo              info   = mock(ServiceInfo.class);
        CacheService             cs     = mock(CacheService.class);
        BackingMapManagerContext ctxMgr = mock(BackingMapManagerContext.class);
        BackingMapContext        ctx    = mock(BackingMapContext.class);
        PartitionedCacheDispatcher bmd    = mock(PartitionedCacheDispatcher.class);

        when(info.getServiceName()).thenReturn("ear:DistributedCache");
        when(cs.getInfo()).thenReturn(info);
        when(ctxMgr.getCacheService()).thenReturn(cs);
        when(ctx.getManagerContext()).thenReturn(ctxMgr);
        when(bmd.getBackingMapContext()).thenReturn(ctx);

        String sXml = LOCAL_LOCATION
                     +  "    <scope-name>ear</scope-name>\n"
                     +  "    <caching-scheme-mapping>\n"
                     +  "        <cache-mapping>\n"
                     +  "            <cache-name>dist-*</cache-name>\n"
                     +  "            <scheme-name>example-distributed</scheme-name>\n"
                     +  "        </cache-mapping>\n"
                     +  "    </caching-scheme-mapping>\n"
                     +  "    <caching-schemes>\n"
                     +  "        <distributed-scheme>\n"
                     +  "            <scheme-name>example-distributed</scheme-name>\n"
                     +  "            <interceptors>\n"
                     +  "                <interceptor>\n"
                     +  "                    <instance>\n"
                     +  "                        <class-name>absent.Class</class-name>\n"
                     +  "                    </instance>\n"
                     +  "                </interceptor>\n"
                     +  "            </interceptors>\n"
                     +  "        </distributed-scheme>\n"
                     +  "    </caching-schemes>\n"
                     +  "</cache-config>";

        Dependencies dependencies = DependenciesHelper.newInstance(new SimpleParser(true).parseXml(sXml));
        new ExtensibleConfigurableCacheFactory(dependencies);

        InterceptorManager manager =
            dependencies.getResourceRegistry().getResource(InterceptorManager.class);
        manager.instantiateServiceInterceptors("ear:DistributedCache");
        }

    @Test
    public void shouldDisposeWithNoServices() throws Exception
        {
        Dependencies                       deps    = DependenciesHelper.newInstance();
        ExtensibleConfigurableCacheFactory eccf    = new ExtensibleConfigurableCacheFactory(deps);

        eccf.m_mapServices.clear();

        eccf.dispose();
        }

    @Test
    public void shouldDisposeWithServiceAlreadyStopped() throws Exception
        {
        Dependencies                       deps    = DependenciesHelper.newInstance();
        ExtensibleConfigurableCacheFactory eccf    = new ExtensibleConfigurableCacheFactory(deps);
        SafeService                        service = new SafeService();
        SafeCluster                        cluster = new SafeCluster();

        service.setSafeCluster(cluster);

        eccf.m_mapServices.put(service, "Foo");

        eccf.dispose();

        assertThat(eccf.isDisposed(), is(true));
        }

    @Test
    public void shouldDisposeWithEmptyServiceReferrersSet() throws Exception
        {
        Dependencies                       deps         = DependenciesHelper.newInstance();
        ExtensibleConfigurableCacheFactory eccf         = new ExtensibleConfigurableCacheFactory(deps);
        SafeService                        service      = new SafeService();
        Service                            serviceInner = mock(Service.class);
        ResourceRegistry                   registry     = service.getResourceRegistry();
        Set<ConfigurableCacheFactory>      setReferrers = new HashSet<>();

        when(serviceInner.getResourceRegistry()).thenReturn(registry);

        registry.registerResource(Set.class, "Referrers", setReferrers);

        service.setInternalService(serviceInner);

        eccf.m_mapServices.put(service, "Foo");

        eccf.dispose();

        assertThat(eccf.isDisposed(), is(true));
        }

    @Test
    public void shouldDisposeWithEccfAsLastReferrer() throws Exception
        {
        Dependencies                       deps         = DependenciesHelper.newInstance();
        ExtensibleConfigurableCacheFactory eccf         = new ExtensibleConfigurableCacheFactory(deps);
        SafeService                        service      = new SafeService();
        SafeCluster                        cluster = new SafeCluster();
        Service                            serviceInner = mock(Service.class);
        ResourceRegistry                   registry     = service.getResourceRegistry();
        Set<ConfigurableCacheFactory>      setReferrers = new HashSet<>();

        when(serviceInner.getResourceRegistry()).thenReturn(registry);

        setReferrers.add(eccf);

        registry.registerResource(Set.class, "Referrers", setReferrers);

        service.setInternalService(serviceInner);
        service.setSafeCluster(cluster);

        eccf.m_mapServices.put(service, "Foo");

        eccf.dispose();

        assertThat(eccf.isDisposed(), is(true));
        verify(serviceInner).shutdown();
        }

    // Regression test for bug 31070472: NPE thrown by ECCF#getParameterResolver when resource mapping not found.
    @Test
    public void testShouldNotThrowNPEWhenResourceNameNotInRegistry()
        {
        BackingMapManagerContext           ctxBMM       = mock(BackingMapManagerContext.class);
        XmlElement                         xmlConfig    = XmlHelper.loadFileOrResource("net/one-cachemapping-cache-config.xml", "cache config");
        Dependencies                       deps         = DependenciesHelper.newInstance(xmlConfig);
        ExtensibleConfigurableCacheFactory eccf         = new ExtensibleConfigurableCacheFactory(deps);
        try
            {
            // ensure no match in registry mapping. default coherence cache config returned a mapping due to wildcard.
            assertNull(eccf.getCacheConfig().getMappingRegistry().findMapping("nonexistent", ResourceMapping.class));
            eccf.getParameterResolver("nonexistent", ResourceMapping.class, this.getClass().getClassLoader(), ctxBMM);
            }
        finally
            {
            eccf.dispose();
            }
        }


    protected Map<String, EventInterceptor> getRawInterceptors(InterceptorRegistry reg)
        {
        Map<String, EventInterceptor> mapIncptrs = new HashMap<String, EventInterceptor>(0);
        try
            {
            InvocationStrategy is = new FieldInvocationStrategy(reg.getClass().getDeclaredField("m_mapInterceptors"));
            mapIncptrs = (Map<String, EventInterceptor>) is.get(reg);
            }
        catch (Exception e) {}
        return Collections.unmodifiableMap(mapIncptrs);
        }

    // ----- inner class: AlgoInterceptor -----------------------------------

    @Interceptor(identifier  = "Algo")
    @EntryEvents
    public static class AlgoInterceptor
            implements EventInterceptor<EntryEvent<?, ?>>
        {
        // ----- constructors -----------------------------------------------

        public AlgoInterceptor()
            {
            }

        public AlgoInterceptor(int nInt)
            {
            }

        // ----- EventInterceptor methods -----------------------------------

        public void onEvent(EntryEvent<?, ?> event)
            {
            }
        }

    // ----- constants ------------------------------------------------------

    private static final String LOCAL_LOCATION =
        "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\" "
        + "xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config "
        + "coherence-cache-config.xsd\">";
    }
