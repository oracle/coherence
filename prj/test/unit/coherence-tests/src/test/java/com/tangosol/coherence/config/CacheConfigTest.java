/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.oracle.coherence.testing.CustomClasses;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.ExternalScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.config.ConfigurationException;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;

import static org.junit.Assert.assertEquals;
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
