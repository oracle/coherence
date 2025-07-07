/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.io.IOException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;
import static org.hamcrest.CoreMatchers.is;

@RunWith(Parameterized.class)
public class PofEnabledTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ----------------------------------------------------

    public PofEnabledTests(String sDescription, String sCacheName)
        {
        this.sDescription = sDescription;
        this.sCacheName   = sCacheName;
        }

    // ----- test lifecycle --------------------------------------------------

    static
        {
        System.setProperty("coherence.serializer", "pof");
        System.setProperty("coherence.pof.enabled", "true");
        System.setProperty("coherence.pof.config", "enabled-pof-config.xml");
        }

    @BeforeClass
    public static void _startup()
        {
        CoherenceCluster cluster = s_cluster.getCluster();

        Eventually.assertDeferred(cluster::getClusterSize, is(1));

        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        }

    @Before
    public void before()
        {
        s_ccf = s_cluster.createSession(SessionBuilders.storageDisabledMember());
        }

    @After
    public void after()
        {
        CacheFactory.shutdown();
        }

    /**
     * Provide the parameters for the tests.
     * <p>
     * This method returns arrays of parameter pairs that will be passed to
     * the constructor of this class. The first parameter is a descriptive
     * name of the type of cache being tested, the second is the name of
     * the cache.
     *
     * @return parameters for the test
     */
    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data()
        {
        return Arrays.asList(
                new Object[] {"Distributed Cache", "example-dist"},
                new Object[] {"Remote Cache", "remote"}
        );
        }

    /**
     * Return the cache used by the tests.
     *
     * @param <K>  the cache key type
     * @param <V>  the cache value type
     *
     * @return the cache used by the tests
     */
    protected <K, V> NamedCache<K,V> getNamedCache()
        {
        NamedCache cache;
        if (sCacheName.contains("remote") || sCacheName.contains("extend"))
            {
            ConfigurableCacheFactory session = s_cluster.createSession(
                    SessionBuilders.extendClient("client-pof-enabled-tests.xml",
                                                 SystemProperty.of("coherence.pof.enabled", "true"),
                                                 SystemProperty.of("coherence.serializer", "pof")));
            cache = session.ensureCache(sCacheName, null);
            }
        else
            {
            cache = s_ccf.ensureTypedCache(sCacheName, null, withoutTypeChecking());
            cache.clear();
            }
        return cache;
        }

    // ----- test ------------------------------------------------------------

    /**
     * Verify pof serialization occurs when coherence.pof.enabled is set to true.
     */
    @Test
    public void testSerialization()
        {
        NamedCache cache = getNamedCache();

        for (int i = 0; i < 10; i++)
            {
            cache.put(i,  new MyObject(i));

            Eventually.assertDeferred(MyObject.SERIALIZATION_COUNTER::get, is(i + 1));
            }

        MyObject.reset();
        Eventually.assertDeferred(MyObject.DESERIALIZATION_COUNTER::get, is(0));
        Eventually.assertDeferred(MyObject.SERIALIZATION_COUNTER::get, is(0));

        // force deserialization
        for (int i = 0; i < 10; i++)
            {
            MyObject temp = (MyObject) cache.get(i);
            }
        Eventually.assertDeferred(MyObject.DESERIALIZATION_COUNTER::get, is(10));

        MyObject.reset();
        Eventually.assertDeferred(MyObject.DESERIALIZATION_COUNTER::get, is(0));
        Eventually.assertDeferred(MyObject.SERIALIZATION_COUNTER::get, is(0));
        }

    // ----- inner class: MyObject -------------------------------------------

    public static class MyObject
            implements PortableObject
        {

        // ----- constructors ------------------------------------------------

        public MyObject()
            {
            }

        public MyObject(int value)
            {
            m_nValue = value;
            }

        // ----- PortableObject interface ------------------------------------

        public void readExternal(PofReader in) throws IOException
            {
            m_nValue = in.readInt(0);
            DESERIALIZATION_COUNTER.incrementAndGet();
            }

        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_nValue);
            SERIALIZATION_COUNTER.incrementAndGet();
            }

        // ----- helpers -----------------------------------------------------

        public static void reset()
            {
            DESERIALIZATION_COUNTER.set(0);
            SERIALIZATION_COUNTER.set(0);
            }

        // ----- constants  --------------------------------------------------

        public static final AtomicInteger DESERIALIZATION_COUNTER = new AtomicInteger();

        public static final AtomicInteger SERIALIZATION_COUNTER = new AtomicInteger();

        // ----- data members -------------------------------------------------

        protected int m_nValue;
        }

    // ----- constants  ------------------------------------------------------

    protected static ConfigurableCacheFactory s_ccf;

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(PofEnabledTests.class);

    @ClassRule
    public static CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .with(ClusterName.of(PofEnabledTests.class.getName() + "Cluster"),
                  CacheConfig.of("coherence-cache-config.xml"),
                  Logging.atMax(),
                  Pof.config("enabled-pof-config.xml"),
                  SystemProperty.of("coherence.extend.serializer", "pof"),
                  SystemProperty.of("coherence.extend.enabled", "true"))
            .include(1, CoherenceClusterMember.class,
                     LocalStorage.enabled(),
                     IPv4Preferred.autoDetect(),
                     DisplayName.of("storage"),
                     RoleName.of("storage"),
                     s_testLogs);

    // ----- data members ----------------------------------------------------

    private String sDescription;

    private String sCacheName;
    }