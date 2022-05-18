/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.ApplicationConsoleBuilder;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.tangosol.net.CacheFactory;

import com.tangosol.coherence.jcache.CoherenceBasedCache;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertThat;

import static com.tangosol.coherence.jcache.Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY;

import static com.oracle.coherence.testing.AbstractFunctionalTest.ensureOutputDir;

import static org.hamcrest.CoreMatchers.is;

import java.util.List;
import java.util.Map;

import javax.cache.Caching;

import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;

/**
 * An implementation class for testing JCache using extend client.
 *
 * Adapted from AbstractCaasServerTest.java by jk.
 *
 * @author jf 2014.05.13
 */
public class ExtendClientTests
        extends AbstractCoherenceCacheTests
    {
    // ----- AbstractCoherenceCacheTest methods -----------------------------

    @Override
    protected <K, V> CompleteConfiguration<K, V> getConfiguration()
        {
        return new MutableConfiguration<>();
        }

    // ----- helpers --------------------------------------------------------

    @BeforeClass
    static public void setup() throws Exception
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();

        String sLocalStorage = System.getProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.distributed.localstorage", sLocalStorage);

        System.setProperty(PROPERTY_POF_ENABLED, "true");

        // start distributed jcache cluster and proxy server.
        try
            {
            startCluster(S_NCCACHESERVERS);
            }
        catch (Exception e)
            {
            e.printStackTrace();

            throw new Error("beforeClassSetup failed to startCluster with unrecoverable error", e);

            }

        // configure JCache client to access coherence via extend client.
        try
            {
            beforeClassSetup();
            }
        catch (Exception e)
            {
            e.printStackTrace();

            throw new Error("beforeClassSetup failed to configure Extend Client System Proeprties with unrecoverable error",
                            e);
            }
        }

    /**
     * Tear everything down
     */
    @AfterClass
    public static void stopCluster()
        {
        // Shut down the JCache cluster
        if (s_cluster != null)
            {
            s_cluster.close();
            s_cluster = null;
            }
        AbstractCoherenceCacheTests.afterClassSetup();
        }

    @Before
    public void setupTest()
        {
        // required to either use cache config with a remote-scheme defined like coherence-jcache-extendclient-cache-config.xml explicitly as URI in call to getCacheManager or
        // set DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY to "remote", "extend" or RemoteCacheConfiguration class name.
        cacheMgr = Caching.getCachingProvider().getCacheManager(null, null, null);

        /*
         * Alternative approach that is less automated.
         * try
         *   {
         *   cacheMgr =
         *       Caching.getCachingProvider().getCacheManager(new URI("coherence-jcache-extendclient-cache-config.xml"),
         *           null, null);
         *   }
         * catch (URISyntaxException e)
         *   {
         *   e.printStackTrace();
         *   }
         */
        super.setupTest();
        lsConfiguration = new MutableConfiguration<>();
        ((MutableConfiguration) lsConfiguration).setTypes(Long.class, String.class);
        spConfiguration = new MutableConfiguration<>();
        ((MutableConfiguration) spConfiguration).setTypes(String.class, Point.class);

        snpConfiguration = new MutableConfiguration<>();
        ((MutableConfiguration) snpConfiguration).setTypes(String.class, NonPofPoint.class);
        iiConfiguration = new MutableConfiguration<>();
        ((MutableConfiguration) iiConfiguration).setTypes(Integer.class, Integer.class);
        ssConfiguration = new MutableConfiguration<>();
        ((MutableConfiguration) ssConfiguration).setTypes(String.class, String.class);

        slConfiguration = new MutableConfiguration<>();
        ((MutableConfiguration) slConfiguration).setTypes(String.class, List.class);
        smConfiguration = new MutableConfiguration<>();
        ((MutableConfiguration) smConfiguration).setTypes(String.class, Map.class);
        }

    @After
    public void cleanupAfterTest()
        {
        super.cleanupAfterTest();
        }

    /**
     * Start the JCache cluster with the specified member count.
     *
     * @param clusterSize the number of members to start.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void startCluster(int clusterSize)
            throws Exception
        {
        startCluster(clusterSize, SERVER_CACHE_CONFIG);
        }

    /**
     * Start the JCache cluster with the specified member count.
     *
     * @param clusterSize the number of members to start.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void startCluster(int clusterSize, String JCacheConfig)
            throws Exception
        {
        ApplicationConsoleBuilder bldrConsole =
            FileWriterApplicationConsole.builder(ensureOutputDir("jcache").getAbsolutePath(),
                ExtendClientTests.class.getSimpleName());

        // Use Bedrock to start a cluster of JCache servers
        OptionsByType           proxyOption    = createExtendProxyOption(JCacheConfig);

        OptionsByType           jCacheOption   = getJCacheOption(JCacheConfig);
        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder();

        clusterBuilder.include(clusterSize, CoherenceCacheServer.class, jCacheOption.asArray());
        clusterBuilder.include(1, CoherenceCacheServer.class, proxyOption.asArray());
        s_cluster = clusterBuilder.build(Console.of(bldrConsole.build("JCache.log")));

        assertThat(invoking(s_cluster).getClusterSize(), is(clusterSize + 1));
        System.out.println("Cluster started with " + s_cluster.getClusterSize() + " members");

        CoherenceClusterMember proxy = s_cluster.get("JCacheProxy-1");

        assertThat(invoking(proxy).isServiceRunning(CoherenceBasedCache.JCACHE_EXTEND_PROXY_SERVICE_NAME), is(true));
        System.out.println("Service JCacheProxy-1 is running");
        }

    /**
     * Configure and set default {@link OptionsByType} that can connect over Extend
     * to the JCache Proxy address and port.
     */
    public static synchronized OptionsByType configureExtendClientSystemProperties(OptionsByType optionsByType)
            throws Exception
        {
        optionsByType.add(SystemProperty.of(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "extend"));

        // extend client system properties
        optionsByType.add(SystemProperty.of("coherence.remote.address", LocalPlatform.get().getLoopbackAddress().getHostAddress()));

        // client system properties
        optionsByType.add(SystemProperty.of(PROPERTY_POF_ENABLED, S_POF_ENABLED));
        optionsByType.add(SystemProperty.of(PROPERTY_POF_CONFIG, POF_CONFIG));

        return optionsByType;
        }

    /**
     * Create a {@link OptionsByType} for a Coherence Extend Proxy server.
     *
     * @param jcacheConfig  a coherence jcache configuration that contains a proxy-scheme that can be enabled.
     * @return extend proxy CoherenceClusterMemberSchema instance
     *
     * @throws Exception
     */
    private static OptionsByType createExtendProxyOption(String jcacheConfig)
            throws Exception
        {
        return configureExtendClientSystemProperties(getJCacheOption(jcacheConfig).addAll(
                DisplayName.of("JCacheProxy"),
                Pof.enabled(Boolean.valueOf(S_POF_ENABLED)),
                LocalStorage.enabled(false),
                SystemProperty.of("coherence.extend.enabled", true)));
        }

    /**
     * Create the {@link OptionsByType} to use to configure the JCacheServer processes.
     *
     * @param JCacheConfig  the location of the optional JCache configuration
     *
     * @return a JCacheServerSchema
     *
     * @throws Exception
     */
    public static OptionsByType getJCacheOption(String JCacheConfig)
                throws Exception
        {
        String localHostName = S_LOCAL_ADDRESS;

        OptionsByType optionsByType = OptionsByType.of(
                DisplayName.of("JCache"),
                Pof.config(POF_CONFIG),
                Pof.enabled(),
                LocalStorage.enabled(),
                JMXManagementMode.ALL,
                JmxProfile.enabled(),
                JmxProfile.hostname(localHostName),
                SystemProperty.of("java.rmi.server.hostname", localHostName),
                IPv4Preferred.yes(),
                LocalHost.only()
        );

        if (JCacheConfig != null && !JCacheConfig.isEmpty())
            {
            optionsByType.add(CacheConfig.of(JCacheConfig));
            }

        return optionsByType;
        }

    // ----- constants ------------------------------------------------------

    private final static String EXTEND_CLIENT_CACHE_CONFIG = "junit-examples-client-extendtcp-cache-config.xml";
    private final static String SERVER_CACHE_CONFIG        = "junit-server-cache-config.xml";
    private final static String POF_CONFIG                 = "coherence-jcache-junit-pof-config.xml";
    private final static int    S_NCCACHESERVERS           = 3;

    private static String S_LOCAL_ADDRESS      = System.getProperty("coherence.localhost", "127.0.0.1");
    private static String PROPERTY_POF_ENABLED = "tangosol.pof.enabled";
    private static String S_POF_ENABLED        = System.getProperty(PROPERTY_POF_ENABLED, "true");
    private static String PROPERTY_POF_CONFIG  = "tangosol.pof.config";

    // ----- data members ---------------------------------------------------

    /** The JCache Cluster */
    protected static CoherenceCluster s_cluster;
    }
