/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.examples;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.ApplicationConsoleBuilder;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.util.Capture;

import com.tangosol.coherence.jcache.CoherenceBasedCache;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.SystemPropertyIsolation;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertThat;

import static com.tangosol.coherence.jcache.Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY;

import static com.oracle.coherence.testing.AbstractFunctionalTest.ensureOutputDir;

import static org.hamcrest.CoreMatchers.is;

import java.net.URI;
import java.net.URISyntaxException;

import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 * Configure and start Cache Servers and Proxy Server and then run JCache Extend Client implementation of JCacheExample.
 *
 * @author  jf 2014.06.04
 */
public class ExtendClientJCacheExampleTests
        extends JCacheExampleTests
    {
    // ----- JCacheExample methods ------------------------------------------

    /**
     * get JCache CacheManager configured for Coherence Extend client access.
     *
     * @return {@link CacheManager} configured to access via Coherence Extend client.
     */
    protected CacheManager getCacheManager()
        {
        URI uri = null;

        try
            {
            uri = new URI(EXTEND_CLIENT_CACHE_CONFIG);
            }
        catch (URISyntaxException e)
            {
            throw new IllegalStateException("unexpected exception configuring JCache CacheManager to access over coherence extend client mode",
                                            e);
            }

        return Caching.getCachingProvider().getCacheManager(uri, null, null);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Method description
     *
     * @throws Exception
     */
    @BeforeClass
    static public void initialize()
            throws Exception
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();

        // Start Oracle bedrock
        AvailablePortIterator ports = LocalPlatform.get().getAvailablePorts();

        // Create a holder for the cluster port of the JCache cluster
        s_nClusterPort = ports.next();

        // get free port for extend proxy
        Capture<Integer> proxyPort = new Capture<Integer>(ports);

        s_nProxyPort    = proxyPort.get();
        s_sProxyAddress = LocalPlatform.get().getLoopbackAddress().getHostAddress();

        // start distributed jcache cluster and proxy server.
        startCluster(S_NCCACHESERVERS, SERVER_CACHE_CONFIG);
        }

    /**
     * Tear everything down
     */
    @AfterClass
    public static void stopCluster()
        {
        // Shut down the JCache cluster
        s_cluster.close();

        Caching.getCachingProvider().close();
        CacheFactory.shutdown();
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
        Base.log("startCluster: clusterPort=" + s_nClusterPort);

        ApplicationConsoleBuilder bldrConsole =
            FileWriterApplicationConsole.builder(ensureOutputDir("jcache").getAbsolutePath(),
                ExtendClientJCacheExampleTests.class.getSimpleName());
        // Use Oracle bedrock to start a cluster of JCache servers
        OptionsByType cacheOption = getJCacheOption(JCacheConfig);
        OptionsByType proxyOption = createExtendProxyOption(JCacheConfig, s_nProxyPort);
        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder();

        clusterBuilder.include(clusterSize, CoherenceCacheServer.class, cacheOption.asArray());

        int nProxyServers = 1;

        clusterBuilder.include(nProxyServers, CoherenceCacheServer.class, proxyOption.asArray());
        s_cluster = clusterBuilder.build(Console.of(bldrConsole.build("JCacheExample.log")));

        // wait for cache servers and proxy server to start.
        assertThat(invoking(s_cluster).getClusterSize(), is(clusterSize + nProxyServers));
        System.out.println("Cluster started with " + s_cluster.getClusterSize() + " members");

        CoherenceClusterMember proxy = s_cluster.get("JCacheProxy-1");
        assertThat(invoking(proxy).isServiceRunning(CoherenceBasedCache.JCACHE_EXTEND_PROXY_SERVICE_NAME), is(true));
        System.out.println("JCacheProxy-1 validated as running");
        }

    /**
     * Configure and set default {@link OptionsByType} that can connect over Extend
     * to the JCache Proxy address and port.
     *
     * @param optionsByType  an {@link OptionsByType} to add the JCache Proxy options to
     *
     * @return an {@link OptionsByType}
     *
     * @throws Exception
     */
    public static synchronized OptionsByType configureExtendClientSystemProperties(OptionsByType optionsByType)
            throws Exception
        {
        optionsByType.add(SystemProperty.of(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "extend"));

        // extend client system properties
        optionsByType.add(SystemProperty.of("coherence.extend.address", s_sProxyAddress));
        optionsByType.add(SystemProperty.of("coherence.extend.port", String.valueOf(s_nProxyPort)));

        // client system properties
        optionsByType.add(SystemProperty.of("coherence.multicastport", String.valueOf(s_nClusterPort)));
        optionsByType.add(SystemProperty.of(PROPERTY_POF_ENABLED, S_POF_ENABLED));
        optionsByType.add(SystemProperty.of(PROPERTY_POF_CONFIG, POF_CONFIG));

        return optionsByType;
        }

    /**
     * Create the {@link OptionsByType} to use to configure the JCacheServer processes.
     *
     * @param JCacheConfig  the location of the optional JCache configuration
     *
     * @return an {@link OptionsByType}
     *
     * @throws Exception
     */
    public static OptionsByType getJCacheOption(String JCacheConfig)
            throws Exception
        {
        String localHostName = S_LOCAL_ADDRESS;

        OptionsByType optionsByType = OptionsByType.of(
                DisplayName.of("JCache"),
                ClusterPort.of(s_nClusterPort),
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

    /**
     * Create an ExtendProxy that uses specified extend proxy address and proxyPort
     *
     * @param jcacheConfig extend enabled cache config uri in string format
     * @param proxyPort  ExtendProxy proxyPort
     *
     * @return extend proxy {@link OptionsByType} instance
     *
     * @throws Exception
     */
    private static OptionsByType createExtendProxyOption(String jcacheConfig, int proxyPort)
            throws Exception
        {
        return configureExtendClientSystemProperties(getJCacheOption(jcacheConfig).addAll(
            DisplayName.of("JCacheProxy"),
            RoleName.of("JCacheProxy"),
            LocalStorage.enabled(false),
            SystemProperty.of("coherence.extend.enabled", true),
            SystemProperty.of("coherence.extend.address", S_LOCAL_ADDRESS),
            SystemProperty.of("coherence.extend.port", String.valueOf(proxyPort)),
            Pof.enabled(Boolean.valueOf(S_POF_ENABLED))));
        }

    // ----- constants ------------------------------------------------------

    private final static String EXTEND_CLIENT_CACHE_CONFIG = "junit-examples-client-extendtcp-cache-config.xml";
    private final static String SERVER_CACHE_CONFIG        = "junit-examples-server-cache-config.xml";
    private final static String POF_CONFIG                 = "coherence-jcache-junit-pof-config.xml";
    private final static int    S_NCCACHESERVERS           = 3;

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();

    // ----- data members ---------------------------------------------------

    private static String S_LOCAL_ADDRESS      = System.getProperty("coherence.localhost", "127.0.0.1");
    private static String PROPERTY_POF_ENABLED = "tangosol.pof.enabled";
    private static String S_POF_ENABLED        = System.getProperty(PROPERTY_POF_ENABLED, "true");
    private static String PROPERTY_POF_CONFIG  = "tangosol.pof.config";

    /** The JCache Cluster */
    protected static CoherenceCluster s_cluster;

    /** The cluster port of the JCache cluster */
    protected static int s_nClusterPort;

    /** JCache Cluster Proxy address */
    protected static String s_sProxyAddress;

    /** JCache Cluster Proxy Port */
    protected static int s_nProxyPort;
    }
