/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.io.pof.reflect.internal.InvocationStrategies.FieldInvocationStrategy;
import com.tangosol.io.pof.reflect.internal.InvocationStrategy;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.NamedEventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link DefaultConfigurableCacheFactory} class
 *
 * @author cf  2011.05.27
 * @author hr  2011.11.15
 *
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class DefaultConfigurableCacheFactoryTest
    {
    @Test
    public void shouldLoadDefaultConfiguration()
        {
        XmlDocument xml = DefaultConfigurableCacheFactory.loadConfig("coherence-cache-config.xml");
        assertThat(xml, is(notNullValue()));
        }

    @Test
    public void testEnsureScopedService()
        {
        String sServiceName = "distributed-service";
        String sScopeName   = "oracle.coherence.myscope";

        XmlElement xmlConfig = XmlHelper.loadFileOrResource("coherence-cache-config.xml", "cache config");
        XmlElement xmlScopeName = new SimpleElement("scope-name", sScopeName);
        xmlConfig.getElementList().add(0, xmlScopeName);

        DefaultConfigurableCacheFactory ccf = new DefaultConfigurableCacheFactory(xmlConfig);

        String actualServiceName = ccf.getScopedServiceName(sServiceName);

        assertEquals(sScopeName + ":" + sServiceName, actualServiceName);
        }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterceptorParsing()
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
                     +  "            <interceptors>\n"
                     +  "                <interceptor>\n"
                     +  "                    <name>Knuth</name>\n"
                     +  "                    <instance>\n"
                     +  "                        <class-name>com.tangosol.net.DefaultConfigurableCacheFactoryTest$AlgoInterceptor</class-name>\n"
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
                     +  "                        <class-name>com.tangosol.net.DefaultConfigurableCacheFactoryTest$AlgoInterceptor</class-name>\n"
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
                     +  "                        <class-name>com.tangosol.net.DefaultConfigurableCacheFactoryTest$AlgoInterceptor</class-name>\n"
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

        ConfigurableCacheFactory ccf = new DefaultConfigurableCacheFactory(new SimpleParser(true).parseXml(sXml));

        InterceptorRegistry registry = ccf.getInterceptorRegistry();

        Map<String, NamedEventInterceptor> mapIncptrsRaw  = getRawInterceptors(registry);
        AlgoInterceptor sedgewick = (AlgoInterceptor) registry.getEventInterceptor("Sedgewick");

        assertNotNull(registry.getEventInterceptor("Knuth"));
        assertNotNull(sedgewick);
        assertNotNull(registry.getEventInterceptor("Algo"));

        NamedEventInterceptor<EntryEvent<?, ?>> incptrNamed = mapIncptrsRaw.get("Sedgewick");

        assertThat("Service name should be either null (DCCF) or the default value of 'DistributedCache' (ECCF)",
                incptrNamed.getServiceName(), anyOf(nullValue(), is("ear:DistributedCache")));
        assertThat(incptrNamed.getEventTypes(), containsInAnyOrder(Type.INSERTED, Type.REMOVED, Type.UPDATED));
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
        
        new DefaultConfigurableCacheFactory(new SimpleParser(true).parseXml(sXml));
        }

    protected Map<String, NamedEventInterceptor> getRawInterceptors(InterceptorRegistry reg)
        {
        Map<String, NamedEventInterceptor> mapIncptrs = new HashMap<String, NamedEventInterceptor>(0);
        try
            {
            InvocationStrategy is = new FieldInvocationStrategy(reg.getClass().getDeclaredField("m_mapInterceptors"));
            mapIncptrs = (Map<String, NamedEventInterceptor>) is.get(reg);
            }
        catch (Exception e) { }
        return Collections.unmodifiableMap(mapIncptrs);
        }


    // ----- inner class: AlgoInterceptor -----------------------------------

    @Interceptor(identifier = "Algo")
    @EntryEvents({Type.INSERTED, Type.REMOVED, Type.UPDATED})
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
