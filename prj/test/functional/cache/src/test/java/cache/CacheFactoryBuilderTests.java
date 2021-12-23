/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheFactoryBuilder;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ScopedCacheFactoryBuilder;
import com.tangosol.net.ScopeResolver;
import com.tangosol.net.SingletonCacheFactoryBuilder;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Service;

import common.AbstractFunctionalTest;
import common.TestHelper;

import org.junit.Test;
import util.GetExtendPort;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.*;


public class CacheFactoryBuilderTests
        extends AbstractFunctionalTest
    {
    // ----- test methods -------------------------------------------------

    /**
    * Test the singleton factory builder.
    */
    @Test
    public void testSingletonFactoryBuilder()
        {
        Base.out("CacheFactoryBuilderTests.testSingletonFactoryBuilder()");

        CacheFactoryBuilder      cfb     = new SingletonCacheFactoryBuilder();
        ClassLoader              loader0 = getLoader(0);
        ClassLoader              loader1 = getLoader(1);
        ConfigurableCacheFactory ccf0;
        ConfigurableCacheFactory ccf1;
        NamedCache               cache0;
        NamedCache               cache1;

        // test canonical configuration
        ccf0 = cfb.getConfigurableCacheFactory(loader0);
        ccf1 = cfb.getConfigurableCacheFactory(loader1);
        cache0 = ccf0.ensureCache("test-dist", loader0);
        cache1 = ccf0.ensureCache("test-dist", loader1);
        assertTrue(ccf0 == ccf1);
        cache0.getCacheService().shutdown();

        // test URI-specific CCFs
        ccf0 = cfb.getConfigurableCacheFactory(loader1);
        ccf1 = cfb.getConfigurableCacheFactory("coherence-cache-config.xml", loader1);
        assertTrue(ccf0 != ccf1);

        // test an externally set config
        cfb.setCacheConfiguration(loader1, TestHelper.parseXmlString(CACHE_CFG_1A));

        ccf0 = cfb.getConfigurableCacheFactory(loader0);
        ccf1 = cfb.getConfigurableCacheFactory(loader1);
        assertTrue(ccf0 == ccf1);

        cache0 = ccf0.ensureCache("test-dist", loader1);
        cache1 = ccf1.ensureCache("test-dist", loader1);
        assertTrue(cache0 == cache1);
        cache0.getCacheService().shutdown();

        // test an externally set config
        cfb.setCacheConfiguration("foo", loader0, TestHelper.parseXmlString(CACHE_CFG_1A));
        cfb.setCacheConfiguration("foo", loader1, TestHelper.parseXmlString(CACHE_CFG_1A));

        ccf0 = cfb.getConfigurableCacheFactory("foo", loader0);
        ccf1 = cfb.getConfigurableCacheFactory("foo", loader1);
        assertTrue(ccf0 != ccf1);

        cache0 = ccf0.ensureCache("test-dist", loader0);
        cache1 = ccf0.ensureCache("test-dist", loader1);
        assertTrue(cache0 != cache1);
        cache0.getCacheService().shutdown();

        // test a resource URI config
        ccf0 = cfb.getConfigurableCacheFactory("coherence-cache-config.xml", loader0);
        ccf1 = cfb.getConfigurableCacheFactory("coherence-cache-config.xml", loader1);
        assertTrue(ccf0 != ccf1);

        cache0 = ccf0.ensureCache("test-dist", loader0);
        cache1 = ccf0.ensureCache("test-dist", loader1);
        assertTrue(cache0 != cache1);
        cache0.getCacheService().shutdown();

        cache0 = ccf0.ensureCache("test-dist", loader0);
        cache1 = ccf1.ensureCache("test-dist", loader1);
        assertTrue(cache0 != cache1);
        cache0.getCacheService().shutdown();
        }

    /**
    * Assert that the scope of a CCF was correctly configured by an extension
    * of DCFB
    */
    @Test
    public void testScopeResolution()
        {
        final String sScope = "oracle.coherence.DCFBScope";
        ScopeResolver resolver = new ScopeResolver()
            {
            public String resolveScopeName(String sConfigURI, ClassLoader loader, String sScopeName)
                {
                return sScope;
                }
            };

        CacheFactoryBuilder      cfb      = new ScopedCacheFactoryBuilder(resolver);
        ClassLoader              loader   = getLoader(0);
        String                   sService = "DistributedCache";
        ConfigurableCacheFactory ccf      =
                cfb.getConfigurableCacheFactory("coherence-cache-config.xml", loader);
        NamedCache               cache    = ccf.ensureCache("test-dist", loader);
        Service                  service  = cache.getCacheService();

        assertServiceName(sService, sScope, service);
        }

    /**
     * Ensure that releaseAll releases the CCF and creates a new instance
     * after releasing.
     */
    @Test
    public void testReleaseAllScopedBuilder()
        {
        testCfbReleaseAll(new ScopedCacheFactoryBuilder());
        }

    /**
     * Ensure that release releases the CCF and creates a new instance
     * after releasing.
     */
    @Test
    public void testReleaseScopedBuilder()
        {
        testCfbRelease(new ScopedCacheFactoryBuilder());
        }

    /**
     * Ensure that releaseAll releases the CCF and creates a new instance
     * after releasing.
     */
    @Test
    public void testReleaseAllDCCF()
        {
        testCfbReleaseAll(new DefaultCacheFactoryBuilder());
        }

    /**
     * Ensure that release releases the CCF and creates a new instance
     * after releasing.
     */
    @Test
    public void testReleaseDCCF()
        {
        testCfbRelease(new DefaultCacheFactoryBuilder());
        }

    /**
     * Assert that an RuntimeException thrown by a scope resolver will
     * propagate to the caller attempting to construct a CCF.
     */
     @Test
     public void testScopeResolutionException()
         {
         ScopeResolver resolver = new ScopeResolver()
             {
             public String resolveScopeName(String sConfigURI, ClassLoader loader, String sScopeName)
                 {
                 throw new IllegalArgumentException();
                 }
             };

         CacheFactoryBuilder cfb      = new ScopedCacheFactoryBuilder(resolver);
         ClassLoader         loader   = getLoader(0);

         try
             {
             ConfigurableCacheFactory ccf = cfb.getConfigurableCacheFactory("coherence-cache-config.xml", loader);
             ccf.ensureService("test");
             fail("Expected WrapperException");
             }
         catch (RuntimeException e)
             {
             }
         }

    /**
     * Test scope name definition via XML.
     */
    @Test
    public void testScopeNameXml()
        {
        String sServiceName = "PartitionedService";
        String sScopeName   = "com.company.Accounts";

        String xml =
            "<?xml version=\"1.0\"?>\n" +
            "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            " xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\" " +
            " xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\"> \n" +
            "  <scope-name>" + sScopeName + "</scope-name>\n" +
            "  <caching-scheme-mapping>\n" +
            "    <cache-mapping>\n" +
            "      <cache-name>*</cache-name>\n" +
            "      <scheme-name>partitioned</scheme-name>\n" +
            "    </cache-mapping>\n" +
            "  </caching-scheme-mapping>\n" +
            "  <caching-schemes>\n" +
            "    <distributed-scheme>\n" +
            "      <scheme-name>partitioned</scheme-name>\n" +
            "      <service-name>" + sServiceName + "</service-name>" +
            "      <backing-map-scheme>\n" +
            "        <local-scheme />\n" +
            "      </backing-map-scheme>\n" +
            "    </distributed-scheme>\n" +
            "  </caching-schemes>\n" +
            "</cache-config>";

        String              sURI   = "cache-config.xml";
        CacheFactoryBuilder cfb    = new ScopedCacheFactoryBuilder();
        ClassLoader         loader = getLoader(0);
        cfb.setCacheConfiguration(sURI, loader, XmlHelper.loadXml(xml));

        ConfigurableCacheFactory ccf = cfb.getConfigurableCacheFactory(sURI, loader);
        Service service = ccf.ensureService(sServiceName);

        assertServiceName(sServiceName, sScopeName, service);
        }

    /**
     * Test scope name when using default static CacheFactory methods;
     * assumes the use of prj/tests/cache/scope-cache-config.xml
     */
    @Test
    public void testDefaultBuilderScope()
        {
        System.setProperty("coherence.cacheconfig", "scope-cache-config.xml");
        CacheFactory.shutdown();
        CacheFactory.ensureCluster();

        String     sServiceName = "DistributedCache";
        String     sScopeName   = "scope";
        NamedCache cache        = CacheFactory.getCache("test");
        Service    service      = cache.getCacheService();

        assertServiceName(sServiceName, sScopeName, service);

        System.getProperties().remove("coherence.cacheconfig");
        CacheFactory.shutdown();
        }

    /**
     * Test the operational override configuration of scope resolver
     * (see prj/tests/cache/scope-override.xml)
     */
    @Test
    public void testOverrideConfiguration()
        {
        System.setProperty("coherence.override", "scope-override.xml");
        CacheFactory.shutdown();
        CacheFactory.ensureCluster();

        NamedCache cache = CacheFactory.getCache("test", getLoader(0));
        assertServiceName("DistributedCache", "Scope_0", cache.getCacheService());

        System.getProperties().remove("coherence.override");
        CacheFactory.shutdown();
        }

    /**
    * Test the (discouraged) mixed-usage case of setCCF()/setCFB()
    */
    @Test
    public void testMixedUsage()
        {
        Base.out("CacheFactoryBuilderTests.testMixedUsage()");
        CacheFactoryBuilder cfb0 = CacheFactory.getCacheFactoryBuilder();
        NamedCache  cache0;
        NamedCache  cache1;
        ClassLoader loader0 = getLoader(0);
        ClassLoader loader1 = getLoader(1);

        cfb0.setCacheConfiguration(loader0, TestHelper.parseXmlString(CACHE_CFG_1A));
        ConfigurableCacheFactory ccf0 = CacheFactory.getConfigurableCacheFactory(loader0);
        ConfigurableCacheFactory ccf1 = TestHelper.instantiateCacheFactory(TestHelper.parseXmlString(CACHE_CFG_1B));

        cache0 = ccf0.ensureCache("test-dist", null);
        try
            {
            // getting the cache from different ccf's should result in
            // a runtime exception while starting the service
            cache1 = ccf1.ensureCache("test-dist", null);
            fail("expected exception");
            }
        catch (RuntimeException e)
            {
            }

        // Note: this should produce a warning message of:
        //   "Mixed usage of getCacheFactoryBuilder() and
        //    setConfigurableCacheFactory() is not recommended."
        CacheFactory.setConfigurableCacheFactory(ccf1);

        try
            {
            // getting the cache from different ccf's should result in
            // a runtime exception while starting the service
            cache1 = CacheFactory.getCache("test-dist");
            fail("expected exception");
            }
        catch (RuntimeException e)
            {
            }

        cache0.getCacheService().shutdown();

        // test that setCCF() didn't change the builder we get from
        // getCFB().
        CacheFactoryBuilder cfb1 = CacheFactory.getCacheFactoryBuilder();
        assertTrue(cfb1 == cfb0);

        // test that the ccf we set is used by CF.getCache(); otherwise
        // we may get a conflicting cache config
        cache0 = ccf1.ensureCache("test-dist", null);
        cache1 = CacheFactory.getCache("test-dist", loader0);
        assertTrue(cache0.getCacheService() == cache1.getCacheService());

        // make sure that after setCCF(), getCCF() returns the
        // singleton, for any loader.
        assertTrue(ccf1 == CacheFactory.getConfigurableCacheFactory());
        assertTrue(ccf1 == CacheFactory.getConfigurableCacheFactory(loader0));
        assertTrue(ccf1 == CacheFactory.getConfigurableCacheFactory(loader1));
        assertTrue(ccf1 != cfb1.getConfigurableCacheFactory(null));
        assertTrue(ccf1 != cfb1.getConfigurableCacheFactory(loader0));
        assertTrue(ccf1 != cfb1.getConfigurableCacheFactory(loader1));

        cache0.getCacheService().shutdown();
        }

    /**
    * Regression test for COH-2634.
    */
    @Test
    public void testCoh2634()
        {
        Base.out("CacheFactoryBuilderTests.testCoh2634");

        Properties    props = new Properties();
        String        port  = String.valueOf(LocalPlatform.get().getAvailablePorts().next());

        props.setProperty("test.extend.port", port);

        CoherenceClusterMember member = startCacheApplication("CacheFactoryBuilderTests",
                                                              "cache.CacheFactoryBuilderTests$Coh2634CacheServer",
                                                              "cache", null, props);

        Eventually.assertThat(invoking(member).isServiceRunning("TcpProxyService"), is(true));

        Integer actualPort = member.invoke(new GetExtendPort("TcpProxyService"));

        System.setProperty("test.extend.port", String.valueOf(actualPort));
        try
            {
            NamedCache cache = TestHelper.instantiateCacheFactory(
                    TestHelper.parseXmlString(CACHE_CFG_COH2634_CLIENT)).ensureCache("dist-extend", null);
            cache.destroy();
            }
        finally
            {
            stopCacheApplication("CacheFactoryBuilderTests");
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Construct a new ScopedLoader for the specified scope index.
    *
    * @param nScope  the scope index to create a loader for
    *
    * @return a ScopedLoader instance
    */
    protected static ScopedLoader getLoader(int nScope)
        {
        return new ScopedLoader("Scope_" + nScope, CacheFactoryBuilder.class.getClassLoader());
        }

    /**
     * Ensure that releaseAll releases the CCF and creates a new instance
     * after releasing.
     */
    protected void testCfbReleaseAll(CacheFactoryBuilder cfb)
        {
        ClassLoader loader = getLoader(0);

        ConfigurableCacheFactory ccf1 = cfb.getConfigurableCacheFactory(loader);
        ConfigurableCacheFactory ccf2 = cfb.getConfigurableCacheFactory(loader);

        assertTrue("Expected the same instance of ConfigurableCacheFactory", ccf1 == ccf2);

        cfb.releaseAll(loader);

        ConfigurableCacheFactory ccf3 = cfb.getConfigurableCacheFactory(loader);
        assertFalse("Expected a different instance of ConfigurableCacheFactory", ccf1 == ccf3);
        }

    /**
     * Ensure that release releases the CCF and creates a new instance
     * after releasing.
     */
    protected void testCfbRelease(CacheFactoryBuilder cfb)
        {
        ClassLoader loader = getLoader(0);

        ConfigurableCacheFactory ccf1 = cfb.getConfigurableCacheFactory(loader);

        cfb.release(ccf1);

        ConfigurableCacheFactory ccf2 = cfb.getConfigurableCacheFactory(loader);
        assertFalse("Expected a different instance of ConfigurableCacheFactory", ccf1 == ccf2);
        }

    // ----- inner class: Coh2634 -------------------------------------------

    /**
    * CacheServer driver for COH-2634 test.
    */
    public static class Coh2634CacheServer
        {
        public static void main(String[] args)
            {
            XmlElement xmlConfig = TestHelper.parseXmlString(CACHE_CFG_COH2634_SERVER);
            XmlHelper.replaceSystemProperties(xmlConfig, "system-property");
            System.out.println("************************");
            System.out.println(xmlConfig);
            System.out.println("************************");
            System.out.println(System.getProperty("test.extend.port"));
            System.out.println("************************");
            ConfigurableCacheFactory ccf = TestHelper.instantiateCacheFactory(xmlConfig);
            CacheFactory.setConfigurableCacheFactory(ccf);

            new DefaultCacheServer(ccf).startAndMonitor(5);
            }
        }


    // ----- inner class: SimpleScopeResolver -------------------------------

    public static class ScopedLoaderResolver
            implements ScopeResolver
        {
        public String resolveScopeName(String sConfigURI, ClassLoader loader, String sScopeName)
            {
            return ((ScopedLoader) loader).getScopeName();
            }
        }


    // ----- constants ----------------------------------------------------

    public static final String COHERENCE_CACHE_CFG = "coherence-cache-config.xml";

    public static final String CACHE_CFG_1A =
        "<cache-config>" +
        "  <caching-scheme-mapping>" +
        "    <cache-mapping>" +
        "      <cache-name>test*</cache-name>" +
        "      <scheme-name>cache-cfg-1</scheme-name>" +
        "    </cache-mapping>" +
        "  </caching-scheme-mapping>" +
        "  <caching-schemes>" +
        "    <distributed-scheme>" +
        "      <scheme-name>cache-cfg-1</scheme-name>" +
        "      <service-name>MyDistCache1</service-name>" +
        "      <backing-map-scheme>" +
        "        <local-scheme>" +
        "          <eviction-policy>HYBRID</eviction-policy>" +
        "          <high-units>{back-size-limit 0}</high-units>" +
        "          <unit-calculator>BINARY</unit-calculator>" +
        "          <expiry-delay>{back-expiry 1h}</expiry-delay>" +
        "        </local-scheme>" +
        "      </backing-map-scheme>" +
        "      <partition-count>257</partition-count>" +
        "      <autostart>true</autostart>" +
        "    </distributed-scheme>" +
        "  </caching-schemes>" +
        "</cache-config>";

    public static final String CACHE_CFG_1B =
        "<cache-config>" +
        "  <caching-scheme-mapping>" +
        "    <cache-mapping>" +
        "      <cache-name>test*</cache-name>" +
        "      <scheme-name>cache-cfg-1</scheme-name>" +
        "    </cache-mapping>" +
        "  </caching-scheme-mapping>" +
        "  <caching-schemes>" +
        "    <distributed-scheme>" +
        "      <scheme-name>cache-cfg-1</scheme-name>" +
        "      <service-name>MyDistCache1</service-name>" +
        "      <backing-map-scheme>" +
        "        <read-write-backing-map-scheme>" +
        "          <scheme-ref>example-read-write</scheme-ref>" +
        "        </read-write-backing-map-scheme>" +
        "      </backing-map-scheme>" +
        "      <partition-count>2039</partition-count>" +
        "      <autostart>true</autostart>" +
        "    </distributed-scheme>" +
        "    <read-write-backing-map-scheme>" +
        "      <scheme-name>example-read-write</scheme-name>" +
        "      <internal-cache-scheme>" +
        "        <local-scheme>" +
        "          <scheme-ref>example-binary-backing-map</scheme-ref>" +
        "        </local-scheme>" +
        "      </internal-cache-scheme>" +
        "      <read-only>false</read-only>" +
        "      <write-delay></write-delay>" +
        "    </read-write-backing-map-scheme>" +
        "    <local-scheme>" +
        "      <scheme-name>example-binary-backing-map</scheme-name>" +
        "      <eviction-policy>HYBRID</eviction-policy>" +
        "      <high-units>{back-size-limit 0}</high-units>" +
        "      <unit-calculator>BINARY</unit-calculator>" +
        "      <expiry-delay>{back-expiry 1h}</expiry-delay>" +
        "      <cachestore-scheme></cachestore-scheme>" +
        "    </local-scheme>" +
        "  </caching-schemes>" +
        "</cache-config>";

    public static final String CACHE_CFG_2A =
        "<cache-config>" +
        "  <caching-scheme-mapping>" +
        "    <cache-mapping>" +
        "      <cache-name>test*</cache-name>" +
        "      <scheme-name>cache-cfg-2</scheme-name>" +
        "    </cache-mapping>" +
        "  </caching-scheme-mapping>" +
        "  <caching-schemes>" +
        "    <distributed-scheme>" +
        "      <scheme-name>cache-cfg-2</scheme-name>" +
        "      <service-name>MyDistCache2</service-name>" +
        "      <backing-map-scheme>" +
        "        <local-scheme>" +
        "          <eviction-policy>HYBRID</eviction-policy>" +
        "          <high-units>{back-size-limit 0}</high-units>" +
        "          <unit-calculator>BINARY</unit-calculator>" +
        "          <expiry-delay>{back-expiry 1h}</expiry-delay>" +
        "        </local-scheme>" +
        "      </backing-map-scheme>" +
        "      <partition-count>257</partition-count>" +
        "      <autostart>true</autostart>" +
        "    </distributed-scheme>" +
        "  </caching-schemes>" +
        "</cache-config>";

    public static final String CACHE_CFG_COH2634_CLIENT =
        "<cache-config>" +
        "  <caching-scheme-mapping>" +
        "    <cache-mapping>" +
        "      <cache-name>dist-extend</cache-name>" +
        "      <scheme-name>extend-dist</scheme-name>" +
        "    </cache-mapping>" +
        "  </caching-scheme-mapping>" +
        "  <caching-schemes>" +
        "    <remote-cache-scheme>" +
        "      <scheme-name>extend-dist</scheme-name>" +
        "      <service-name>ExtendTcpCacheService</service-name>" +
        "      <initiator-config>" +
        "        <tcp-initiator>" +
        "          <remote-addresses>" +
        "            <socket-address>" +
        "              <address system-property=\"test.extend.address.remote\">" + LocalPlatform.get().getLoopbackAddress().getHostAddress() + "</address>" +
        "              <port    system-property=\"test.extend.port\">9999</port>" +
        "            </socket-address>" +
        "          </remote-addresses>" +
        "        </tcp-initiator>" +
        "        <connect-timeout>10s</connect-timeout>" +
        "      </initiator-config>" +
        "    </remote-cache-scheme>" +
        "  </caching-schemes>" +
        "</cache-config>";

    public static final String CACHE_CFG_COH2634_SERVER =
        "<cache-config>" +
        "  <caching-scheme-mapping>" +
        "    <cache-mapping>" +
        "      <cache-name>*</cache-name>" +
        "      <scheme-name>example-distributed</scheme-name>" +
        "    </cache-mapping>" +
        "  </caching-scheme-mapping>" +
        "  <caching-schemes>" +
        "    <local-scheme>" +
        "      <scheme-name>example-binary-backing-map</scheme-name>" +
        "      <eviction-policy>HYBRID</eviction-policy>" +
        "      <high-units>{back-size-limit 0}</high-units>" +
        "      <unit-calculator>BINARY</unit-calculator>" +
        "      <expiry-delay>{back-expiry 1h}</expiry-delay>" +
        "      <cachestore-scheme></cachestore-scheme>" +
        "    </local-scheme>" +
        "    <distributed-scheme>" +
        "      <scheme-name>example-distributed</scheme-name>" +
        "      <service-name>DistributedCache</service-name>" +
        "      <backing-map-scheme>" +
        "        <local-scheme>" +
        "          <scheme-ref>example-binary-backing-map</scheme-ref>" +
        "        </local-scheme>" +
        "      </backing-map-scheme>" +
        "      <autostart>true</autostart>" +
        "    </distributed-scheme>" +
        "    <proxy-scheme>" +
        "      <scheme-name>example-proxy</scheme-name>" +
        "      <service-name>TcpProxyService</service-name>" +
        "      <acceptor-config>" +
        "        <tcp-acceptor>" +
        "          <local-address>" +
        "            <address system-property=\"test.extend.address.local\">" + LocalPlatform.get().getLoopbackAddress().getHostAddress() + "</address>" +
        "            <port    system-property=\"test.extend.port\">9999</port>" +
        "          </local-address>" +
        "        </tcp-acceptor>" +
        "      </acceptor-config>" +
        "      <autostart>true</autostart>" +
        "    </proxy-scheme>" +
        "  </caching-schemes>" +
        "</cache-config>";
    }