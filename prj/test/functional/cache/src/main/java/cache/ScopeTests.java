/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import static org.junit.Assert.*;

/**
 * Tests for scoped service used by the ConfigurableCacheFactory.  Note that
 * ECCF is tested by default, which is true for all Coherence tests.  To test
 * DCCF, run the test with:
 *      -Dtangosol.coherence.cachefactory=com.tangosol.net.DefaultConfigurableCacheFactory
 */
public class ScopeTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }


    // ----- helpers --------------------------------------------------------

    /**
     * @return default cache configuration XML
     */
    private XmlElement getDefaultConfig()
        {
        return  XmlHelper.loadFileOrResource(FILE_CFG_CACHE, "cache configuration", null);
        }

    /**
     * @return classloader for this test
     */
    private ClassLoader getClassLoader()
        {
        return this.getClass().getClassLoader();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Simple test of service creation without a scope name.
     */
    @Test
    public void testEnsureService()
        {
        ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder()
            .getConfigurableCacheFactory(FILE_CFG_CACHE, null);

        String  sServiceName = "DistributedCache";
        Service service      = ccf.ensureService(sServiceName);

        assertServiceName(sServiceName, null, service);
        }

    /**
     * Test creation of a service name when providing CCF with a scope name.
     */
    @Test
    public void testEnsureScopedService()
        {
        String sServiceName  = "DistributedCache";
        String sScopeName    = "oracle.coherence.myscope";

        XmlElement xmlConfig    = getDefaultConfig();
        XmlElement xmlScopeName = new SimpleElement("scope-name", sScopeName);
        xmlConfig.getElementList().add(0, xmlScopeName);

        ConfigurableCacheFactory ccf     = TestHelper.instantiateCacheFactory(xmlConfig, getClassLoader());
        Service                  service = ccf.ensureService(sServiceName);

        assertServiceName(sServiceName, sScopeName, service);
        }

    /**
     * Test creation of a cache when providing DCCF with a scope name.
     */
    @Test
    public void testEnsureScopedCache()
        {
        String sServiceName = "DistributedCache";
        String sScopeName   = "oracle.coherence.myscope";
        String sCacheName   = "test-cache";

        XmlElement xmlConfig    = getDefaultConfig();
        XmlElement xmlScopeName = new SimpleElement("scope-name", sScopeName);
        xmlConfig.getElementList().add(0,xmlScopeName);

        ConfigurableCacheFactory ccf     = TestHelper.instantiateCacheFactory(xmlConfig, getClassLoader());
        NamedCache               cache   = ccf.ensureCache(sCacheName, this.getClass().getClassLoader());
        Service                  service = cache.getCacheService();

        assertServiceName(sServiceName, sScopeName, service);
        }

    /**
     * Assert that scope name cannot be changed on DCCF after a service
     * has been started.
     */
    @Test
    public void testEnsureScopeReadOnly()
        {
        String sServiceName = "DistributedCache";
        String sScopeName   = "oracle.coherence.myscope";
        String sCacheName   = "test-cache";

        XmlElement xmlConfig    = getDefaultConfig();
        XmlElement xmlScopeName = new SimpleElement("scope-name", sScopeName);
        xmlConfig.getElementList().add(0, xmlScopeName);

        ConfigurableCacheFactory ccf     = TestHelper.instantiateCacheFactory(xmlConfig, null);
        NamedCache               cache   = ccf.ensureCache(sCacheName, getClass().getClassLoader());
        Service                  service = cache.getCacheService();

        assertServiceName(sServiceName, sScopeName, service);
        }

    /**
     * Test creation of multiple DCCF instances with different scopes;
     * assert that caches are isolated.
     */
    @Test
    public void testMultipleScopes()
        {
        String sScopeName1 = "oracle.coherence.myscope1";
        String sScopeName2 = "oracle.coherence.myscope2";
        String sCacheName  = "test-cache";
        String sKey        = "key";
        String sValue1     = "value1";
        String sValue2     = "value2";

        XmlElement xmlConfig = getDefaultConfig();
        XmlElement xmlScopeName = new SimpleElement("scope-name", sScopeName1);
        xmlConfig.getElementList().add(0, xmlScopeName);

        ConfigurableCacheFactory ccf1   = TestHelper.instantiateCacheFactory(xmlConfig, getClassLoader());
        NamedCache               cache1 = ccf1.ensureCache(sCacheName, getClassLoader());

        XmlElement xmlConfig2 = getDefaultConfig();
        xmlScopeName = new SimpleElement("scope-name", sScopeName2);
        xmlConfig2.getElementList().add(0,xmlScopeName);

        ConfigurableCacheFactory ccf2   = TestHelper.instantiateCacheFactory(xmlConfig2, getClassLoader());
        NamedCache               cache2 = ccf2.ensureCache(sCacheName, getClassLoader());

        cache1.put(sKey, sValue1);
        assertEquals(sValue1, cache1.get(sKey));
        assertNull(cache2.get(sKey));

        cache2.put(sKey, sValue2);
        assertEquals(sValue1, cache1.get(sKey));
        assertEquals(sValue2, cache2.get(sKey));
        }

    // ----- constants and data members -------------------------------------

    /**
     * The path to the cache configuration.
     */
    public static final String FILE_CFG_CACHE = "coherence-cache-config.xml";
    }
