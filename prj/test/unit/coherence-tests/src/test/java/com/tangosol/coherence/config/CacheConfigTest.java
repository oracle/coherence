/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.oracle.coherence.testing.CustomClasses;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.DistributedScheme;
import com.tangosol.coherence.config.scheme.ExternalScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.config.ConfigurationException;
import com.tangosol.internal.net.service.grid.PartitionedCacheDependencies;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import com.oracle.coherence.testing.SystemPropertyResource;
import org.junit.Test;
import org.mockito.Mockito;


/**
 * Unit Tests for a {@link CacheConfig}.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class CacheConfigTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test a basic configuration of a cache configuration.
     */
    @Test
    public void testLocal()
        {
        String sXml =
              "<cache-config>"
            +   "<caching-scheme-mapping>"
            +     "<cache-mapping>"
            +       "<cache-name>CustomerCache</cache-name>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +     "</cache-mapping>"
            +   "</caching-scheme-mapping>"
            +   "<caching-schemes>"
            +     "<local-scheme>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +       "<service-name>CustomerService</service-name>"
            +     "</local-scheme>"
            +   "</caching-schemes>"
            + "</cache-config>";

        testSimpleAccess(sXml, LocalScheme.class);
        }

    /**
     * Test a basic configuration of a cache configuration.
     */
    @Test
    public void testExternal()
        {
        String sXml =
              "<cache-config>"
            +   "<caching-scheme-mapping>"
            +     "<cache-mapping>"
            +       "<cache-name>CustomerCache</cache-name>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +     "</cache-mapping>"
            +   "</caching-scheme-mapping>"
            +   "<caching-schemes>"
            +     "<external-scheme>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +       "<service-name>CustomerService</service-name>"
            +       "<custom-store-manager>"
            +         "<class-name>" + CustomClasses.CustomBinaryStoreManager.class.getName() + "</class-name>"
            +       "</custom-store-manager>"
            +     "</external-scheme>"
            +   "</caching-schemes>"
            + "</cache-config>";

        testSimpleAccess(sXml, ExternalScheme.class);
        }

    /**
     * Test that a properly formatted ConfiguratonException is generated when
     * the XML has 2 child elements (external-scheme) children, when it
     * should only have 1.
     */
    @Test(expected = ConfigurationException.class)
    public void testCOH_7151()
        {
        String sXml =
              "<cache-config>"
            +   "<caching-scheme-mapping>"
            +     "<cache-mapping>"
            +       "<cache-name>CustomerCache</cache-name>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +     "</cache-mapping>"
            +   "</caching-scheme-mapping>"
            +   "<caching-schemes>"
            +     "<external-scheme>"
            +       "<scheme-name>foo</scheme-name>"
            +       "<nio-file-manager/>"
            +       "<local-scheme/>"
            +     "</external-scheme>"
            +   "</caching-schemes>"
            + "</cache-config>";

        //attempt to construct an ExtensibleConfigurableCacheFactory based on the provided configuration
        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sXml));
        new ExtensibleConfigurableCacheFactory(dependencies);
        }

    /**
     * Validate that system property matching a Coherence config element no longer overrides value.
     */
    @Test
    public void testCOH_13933()
        {
        String sXml =
            "<cache-config>"
            +   "<caching-scheme-mapping>"
            +     "<cache-mapping>"
            +       "<cache-name>CustomerCache</cache-name>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +     "</cache-mapping>"
            +   "</caching-scheme-mapping>"
            +   "<caching-schemes>"
            +     "<local-scheme>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +       "<service-name>CustomerService</service-name>"
            +       "<autostart>true</autostart>"
            +     "</local-scheme>"
            +   "</caching-schemes>"
            + "</cache-config>";

        // before fix, this test threw a ConfigurationException invalid value for autostart.
        // Removed resolve of each coherence config element with existing system properties.
        try (SystemPropertyResource p = new SystemPropertyResource("autostart", "Y"))
            {
            testSimpleAccess(sXml, LocalScheme.class);
            }
        }

    @Test
    public void testVersion13DistributedSchemeStillParses()
        {
        String sXml =
              "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            +               "xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\" "
            +               "xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config "
            +               "http://xmlns.oracle.com/coherence/coherence-cache-config/1.3/coherence-cache-config.xsd\">"
            +   "<caching-scheme-mapping>"
            +     "<cache-mapping>"
            +       "<cache-name>CustomerCache</cache-name>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +     "</cache-mapping>"
            +   "</caching-scheme-mapping>"
            +   "<caching-schemes>"
            +     "<distributed-scheme>"
            +       "<scheme-name>CustomerScheme</scheme-name>"
            +       "<service-name>CustomerService</service-name>"
            +       "<thread-count-max>16</thread-count-max>"
            +       "<thread-count-min>2</thread-count-min>"
            +       "<backing-map-scheme>"
            +         "<local-scheme/>"
            +       "</backing-map-scheme>"
            +     "</distributed-scheme>"
            +   "</caching-schemes>"
            + "</cache-config>";

        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sXml));
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(dependencies);
        DistributedScheme scheme = (DistributedScheme) eccf.getCacheConfig().findSchemeByCacheName("CustomerCache");
        PartitionedCacheDependencies deps = scheme.getServiceDependencies();

        assertEquals(2, deps.getWorkerThreadCountMin());
        assertEquals(16, deps.getWorkerThreadCountMax());
        assertFalse(deps.isDaemonPoolConfigured());
        assertFalse(deps.isTaskLimitConfigured());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Test a basic configuration of a cache configuration.
     *
     * @param sXml      the XML cach-config element
     * @param clzClass  the expected builder class
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void testSimpleAccess(String sXml, Class<?> clzClass)
        {
        //construct an ExtensibleConfigurableCacheFactory based on the provided configuration
        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sXml));
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(dependencies);

        //grab the produced CacheConfig
        CacheConfig config = eccf.getCacheConfig();
        System.out.println(config);
        System.out.println();

        // find the builder that can build the map
        CachingScheme scheme = config.findSchemeByCacheName("CustomerCache");
        MapBuilder builderMap = scheme;
        System.out.println(builderMap);

        // create the LocalCache and test put/get
        // temporary - check builder type until common realize interface is done
        Map cache = null;

        MapBuilder.Dependencies depMapBuilder =
                new MapBuilder.Dependencies(null, Mockito.mock(BackingMapManagerContext.class),
                                      Base.getContextClassLoader(),
                                      "CustomerCache",
                                      "");  // service type

        // make sure the expected builder was created
        assertTrue(clzClass.isAssignableFrom(builderMap.getClass()));

        // create the cache
        cache = builderMap.realizeMap(config.getDefaultParameterResolver(), depMapBuilder);

        cache.put("key", "value");
        assertEquals(cache.get("key"),"value");
        }

    /**
     * Test a basic configuration of a cache/topic configuration.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCOH_20313()
        {
        String sXml =
            "<cache-config>"
                +   "<caching-scheme-mapping>"
                +     "<cache-mapping>"
                +       "<cache-name>CustomerCache</cache-name>"
                +       "<scheme-name>CustomerScheme</scheme-name>"
                +     "</cache-mapping>"
                +   "</caching-scheme-mapping>"
                +    "<topic-scheme-mapping>"
                +     "<topic-mapping>"
                +      "<topic-name>a-topic</topic-name>"
                +      "<scheme-name>topic-scheme</scheme-name>"
                +      "<value-type>java.lang.String</value-type>"
                +     "</topic-mapping>"
                +   "</topic-scheme-mapping>"
                +   "<caching-schemes>"
                +     "<paged-topic-scheme>"
                +      "<scheme-name>topic-scheme</scheme-name>"
                +      "<autostart>true</autostart>"
                +     "</paged-topic-scheme>"
                +     "<local-scheme>"
                +       "<scheme-name>CustomerScheme</scheme-name>"
                +       "<service-name>CustomerService</service-name>"
                +       "<autostart>true</autostart>"
                +     "</local-scheme>"
                +   "</caching-schemes>"
                + "</cache-config>";

        //construct an ExtensibleConfigurableCacheFactory based on the provided configuration
        Dependencies dependencies = DependenciesHelper.newInstance(XmlHelper.loadXml(sXml));
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(dependencies);

        //grab the produced CacheConfig
        CacheConfig config = eccf.getCacheConfig();

        // find the builder that can build the map
        CacheMappingRegistry registry     = config.getCacheMappingRegistry();
        CacheMapping         cacheMapping = registry.findCacheMapping("CustomerCache");
        assertNotNull("failed to find cache CustomerCache", cacheMapping);
        assertTrue(cacheMapping.isForName("CustomerCache"));

        ResourceMappingRegistry registryResource     = config.getMappingRegistry();
        ResourceMapping         cacheResourceMapping = registryResource.findCacheMapping("CustomerCache");
        assertNotNull("failed to find cache CustomerCache using resource registry", cacheResourceMapping);
        assertTrue(cacheResourceMapping.isForName("CustomerCache"));

        cacheResourceMapping = registryResource.findMapping("CustomerCache", CacheMapping.class);
        assertNotNull("failed to find cache CustomerCache using resource registry", cacheResourceMapping);
        assertTrue(cacheResourceMapping.isForName("CustomerCache"));

        ResourceMapping         topicResourceMapping = registryResource.findMapping("a-topic", TopicMapping.class);
        assertNotNull("failed to find topic a-topic using resource registry", topicResourceMapping);
        assertTrue(topicResourceMapping.isForName("a-topic"));
        }
    }
