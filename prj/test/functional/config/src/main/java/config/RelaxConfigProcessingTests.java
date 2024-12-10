/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.config.ConfigurationException;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.util.Base;
import com.tangosol.util.WrapperException;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.oracle.coherence.testing.SystemPropertyResource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

/**
 * Regression test for BugDB 19667679 - Serializer defined by an instance element with a factory-class-name fails
 *                     BugDB 19337743 - Failfast checking of non-autostarted service (and never to be started as far as bug report concerned)
 *                     BugDB 20231406 - relax cache config processing to not fail-fast but only when a service is instantiated
 *
 * @version        12.1.3
 * @author         jf 14/11/11
 */
public class RelaxConfigProcessingTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public RelaxConfigProcessingTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.override", "persistence-tangosol-coherence-override.xml");

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------
    @Test
    public void testWorkingSerializerFactory()
            throws Exception
        {
        NamedCache cache = validateNamedCache("distributed-cache-one", CacheService.TYPE_DISTRIBUTED);

        cache.put(1, TimeUnit.DAYS);
        cache.put("Key", "Val");
        Assert.assertEquals("Val", cache.get("Key"));
        cache.release();
        }

    @Test(expected = WrapperException.class)
    public void detectBrokenSerializerFactory()
            throws Exception
        {
        NamedCache cache = validateNamedCache("broken-serializer-cache-one", CacheService.TYPE_DISTRIBUTED);

        try
            {
            cache.put(1, TimeUnit.DAYS);
            }
        finally
            {
            cache.release();
            }
        }

    /**
     * Validate deferred instantiation still throws a classnotfound exception.
     */
    @Test
    public void detectNonExistentMapListener()
        {
        NamedCache cache = null;

        try
            {
            validateNamedCache("nonexistent-maplistener-cache-one", CacheService.TYPE_DISTRIBUTED);
            fail("excepted ClassNotFoundException for non-existent MemberListener");
            }
        catch (WrapperException e)
            {
            assertExceptionContainsCause(e, ClassNotFoundException.class);
            }
        finally
            {
            if (cache != null)
                {
                cache.release();
                }
            }
        }

    /**
     * Validate deferred instantiation still throws a ConfigurationException for
     * invalid partition assignment strategy.
     */
    @Test
    public void detectClassNotFoundPartitionAssignmentStrategy()
        {
        NamedCache cache = null;

        try
            {
            validateNamedCache("invalid-cnf-pas-cache-one", CacheService.TYPE_DISTRIBUTED);
            fail("expected ConfigurationException with ClassNotFoundCause for non-existent custom partition assignment strategy");
            }
        catch (Exception e)
            {
            assertExceptionContainsCause(e, ConfigurationException.class);
            assertExceptionContainsCause(e, ClassNotFoundException.class);
            }
        finally
            {
            if (cache != null)
                {
                cache.release();
                }
            }
        }

    /**
     * Validate deferred instantiation still throws a ConfigurationException for
     * invalid partition assignment strategy.
     */
    @Test
    public void detectInvalidPartitionAssignmentStrategy()
        {
        NamedCache cache = null;

        try
            {
            validateNamedCache("invalid-pas-cache-one", CacheService.TYPE_DISTRIBUTED);
            fail("expected ConfigurationException for non-existent predefined partition assignment strategy");
            }
        catch (Exception e)
            {
            assertExceptionContainsCause(e, ConfigurationException.class);
            }
        finally
            {
            if (cache != null)
                {
                cache.release();
                }
            }
        }

    /**
     * Validate deferred validation does not allow a service referencing a non-existent class to start.
     */
    @Test
    public void regressionBugDB19337743_nofailfastNonExistentClassNonAutoStartedService()
        {
        try
            {
            Service svc = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                              null).ensureService("ProxyService");

            if (svc != null)
                {
                svc.shutdown();
                }

            fail("service refers to non-existent classes so should not start.");
            }
        catch (WrapperException e)
            {
            assertExceptionContainsCause(e, ClassNotFoundException.class);
            }
        }

    /**
     * Test transition from fail-fast to lazy instantiation when service is started.
     * validate that can load a cache config file with references to non existent
     * classes as long as service is not started. Then validate that ClassNotFoundException
     * is thrown when that service is started.
     */
    @Test
    public void regressionBug19337743_noConfigExceptionForNonStartedService()
        {
        ConfigurableCacheFactory ccf =
            CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CLASSNOTFOUND_CACHE_CONFIG, null);

        // assert no fail-fast processing cache config file
        assertNotNull(ccf);

        // assert non-autostarted service referencing non-existent classes does fail when instantiated
        try
            {
            Service svc = ccf.ensureService("AnotherProxyService");

            if (svc != null)
                {
                svc.shutdown();
                }

            fail("service refers to non-existent classes so should not start.");
            }
        catch (WrapperException e)
            {
            assertExceptionContainsCause(e, ClassNotFoundException.class);
            }
        }

    @Test
    public void testLazyEvaluationMissingClassPartitionedQuorumPolicy()
        {
        ConfigurableCacheFactory ccf =
                CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CLASSNOTFOUND_CACHE_CONFIG, null);

        // assert no fail-fast processing cache config file
        assertNotNull(ccf);

        // assert non-autostarted service referencing non-existent classes does fail when instantiated
        try
            {
            Service svc = ccf.ensureService("missingCustomProxyQuorumPolicyService");

            if (svc != null)
                {
                svc.shutdown();
                }

            fail("service refers to non-existent classes so should not start.");
            }
        catch (WrapperException e)
            {
            assertExceptionContainsCause(e, ClassNotFoundException.class);
            }        }

    @Test
    public void testLazyEvaluationMisssingClassProxyQuorumPolicy()
        {                      ConfigurableCacheFactory ccf =
                CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CLASSNOTFOUND_CACHE_CONFIG, null);

        // assert no fail-fast processing cache config file
        assertNotNull(ccf);

        // assert non-autostarted service referencing non-existent classes does fail when instantiated
        try
            {
            Service svc = ccf.ensureService("missingCustomPartitionedQuorumPolicyService");

            if (svc != null)
                {
                svc.shutdown();
                }

            fail("service refers to non-existent classes so should not start.");
            }
        catch (WrapperException e)
            {
            assertExceptionContainsCause(e, ClassNotFoundException.class);
            }

        }

    /**
     * Test detection of invalid value of cluster quorum policy.
     */
   // TODO @Test
    public void testInvalidClusterQuorum()
            throws InterruptedException
        {
        CacheFactory.shutdown();

        try (SystemPropertyResource prop = new SystemPropertyResource("coherence.override", "invalid-quorum-coherence-override.xml"))
            {
            com.tangosol.coherence.component.util.SafeCluster clusterSafe =
                    (com.tangosol.coherence.component.util.SafeCluster) CacheFactory.ensureCluster();
            fail("cluster should not start with invalid cluster-quorum-policy timeout");
            }
        catch (IllegalArgumentException e)
                {
                // expected result
                }
        finally
            {
            CacheFactory.shutdown();
            _startup();
            }
        }

        /**
         * Validate deferred validation does not allow a service referencing a non-existent storage-authorizer to start.
         */
        @Test
        public void detectMissingStorageAuthorizer()
            {
            try
                {
                NamedCache cache =  CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                        null).ensureCache("storage-authorizer-missing-cache", null);
                cache.put(1, 2);
                fail("cache should be inaccessible since referencing non-existent StorageAccessAuthorizer");
                }
            catch (Exception e)
                {
                assertExceptionContainsCause(e, UnsupportedOperationException.class);
                }
        }

        /**
         * Validate deferred validation does not allow a service referencing a non-existent storage-authorizer to start.
         */
        @Test
        public void detectMissingClassStorageAuthorizer()
        {
            try
                {
                ExtensibleConfigurableCacheFactory eccf =
                        (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                        null);
                NamedCache cache = eccf.ensureCache("storage-authorizer-cnf-cache", null);
                cache.put(1, 2);
                fail("cache should be inaccessible since its service refers to non-existent class for a named storage-authorizer.");
                }
            catch (WrapperException e)
                {
                assertExceptionContainsCause(e, UnsupportedOperationException.class);
                }
        }


        /**
         * Validate deferred validation does not allow a service referencing a non-existent class to start.
         */
        @Test
        public void detectInvalidAuthorizedHostsInProxyService()
        {
            try
            {
                Service svc = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                        null).ensureService("invalidAuthorizedHostsProxyService");

                if (svc != null)
                {
                    svc.shutdown();
                }

                fail("service refers to non-existent authorized-host so should not start.");
            }
            catch (WrapperException e)
            {
                assertExceptionContainsCause(e, ClassNotFoundException.class);
            }
        }

        /**
     * Validate deferred validation does not allow a service referencing a non-existent class to start.
     */
    @Test
    public void detectInvalidLoadBalancerProxyService()
        {
        try
            {
            Service svc = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                    null).ensureService("invalidLoadBalancerProxyService");

            if (svc != null)
                {
                svc.shutdown();
                }

            fail("service refers to non-existent ProxyServiceLoadBalancer so should not start.");
            }
        catch (WrapperException e)
            {
            assertExceptionContainsCause(e, ClassNotFoundException.class);
            }
        }

    @Test
    public void validProxyServiceReferenceNamedAddressProviderFactory()
        {
        CacheFactory.shutdown();

        try (SystemPropertyResource prop = new SystemPropertyResource("coherence.override", "addressproviders-coherence-override-dev.xml"))
            {
            Service svc = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                    null).ensureService("existentAddressProviderRefProxyService");
            if (svc != null)
                {
                svc.shutdown();
                }
            }
        finally
            {
            CacheFactory.shutdown();
            _startup();
            }
        }

    @Test
    public void validateDefaultingOfNonexistentAddressProviderReferenceProxyService()
        {
        try
            {
            Service svc = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                    null).ensureService("nonExistentAddressProviderRefProxyService");

            // uses default NS local address with unresolved address-provider reference.

            if (svc != null)
                {
                svc.shutdown();
                }

            }
        catch (WrapperException e)
            {
            assertExceptionContainsCause(e, IllegalArgumentException.class);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Get the cache and do a simple test to ensure that it works.
     *
     * @param sName         the cache name
     * @param sServiceType  the expected service type
     *
     * @return the named cache
     */
    protected NamedCache validateNamedCache(String sName, String sServiceType)
        {
        NamedCache cache = getNamedCache(sName);

        assertNotNull(cache);
        cache.put(1, "One");
        Assert.assertEquals(cache.get(1), "One");

        // validate the service type
        Assert.assertEquals(cache.getCacheService().getInfo().getServiceType(), sServiceType);

        return cache;
        }

    /**
     * Assert that exception cause hierarchy does not contain clzCause.
     *
     * @param e        exception
     * @param clzCause nested exception cause
     */
    private void assertExceptionContainsCause(Exception e, Class clzCause)
        {
        if (e.getClass().isAssignableFrom(clzCause)) {
            return;
        }

        Throwable eCause = e.getCause();

        while (eCause != null)
            {
            if (eCause.getClass().isAssignableFrom(clzCause))
                {
                return;
                }
            else
                {
                eCause = eCause.getCause();
                }
            }

        fail("unexpected that exception did not contain cause " + clzCause.getSimpleName() +
                "\nStack Trace:\n" + Base.printStackTrace(e));
        }

    // ----- nested classes -------------------------------------------------

    /**
     * Broken SerializerFactory. Validate checking for this case.
     * Also shows configuration processing no longer employing fail-fast.
     * Failure only occurs on first usage of serializer which is lazily
     * instantiated.
     *
     * @version        12.1.3
     * @author         jf 14/11/11
     */
    public static class BrokenSerializerFactory
        {
        public static String getSerializer()
            {
            return "broken";
            }
        }

    /**
     * SerializerFactory that does not return Serializer.
     * Relax type checking for instance factory to address bugdb 19667679
     *
     * @version        12.1.3
     * @author         jf 14/11/11
     */
    public static class SerializerFactory
        {
        public static Object getSerializer()
            {
            return new ConfigurablePofContext();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CFG_CACHE = "relax-config-processing-cache-config.xml";

    /**
     * Validate that CNF still thrown when class is non-existent.
     * Relaxed fail fast so not checking for CNF when first processing cache config.
     */
    private static String FILE_CLASSNOTFOUND_CACHE_CONFIG = "classnotfound-cache-config.xml";

    /**
     * Test project.
     */
    public static final String PROJECT = "config";
    }
