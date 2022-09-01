/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.ssl;

import com.oracle.bedrock.OptionsByType ;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.MemberName;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.io.FileHelper;
import com.tangosol.net.CacheFactory;

import com.tangosol.net.NamedCache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Abstract test implementation.
 *
 * @author Tim Middleton 2022.06.15
 */
public abstract class AbstractSSLExampleTest {
    protected static AvailablePortIterator availablePortIterator;
    protected static CoherenceCacheServer  member1 = null;
    protected static CoherenceCacheServer  member2 = null;
    protected static int                   extendPort;
    protected static String                hostName;

    protected static KeyTool.KeyAndCert serverCACert;
    protected static KeyTool.KeyAndCert serverKeyAndCert;
    protected static KeyTool.KeyAndCert clientCACert;
    protected static KeyTool.KeyAndCert clientKeyAndCert;
    protected static File               tmpDir;

    private static final String OVERRIDE            = "tangosol-coherence-ssl.xml";
    private static final String SERVER_CACHE_CONFIG = "server-cache-config.xml";
    private static final String EXTEND_CACHE_CONFIG = "extend-cache-config.xml";

    /**
     * Start the cluster using the given socket provider.
     *
     * @param socketProvider  socket provider to use.
     */
    public static void _startup(String socketProvider) {
        LocalPlatform platform = LocalPlatform.get();
        availablePortIterator = platform.getAvailablePorts();
        availablePortIterator.next();

        int clusterPort = availablePortIterator.next();
        extendPort = availablePortIterator.next();
        hostName = LocalPlatform.get().getLoopbackAddress().getHostAddress();

        OptionsByType extendOptions = createCacheServerOptions(clusterPort, extendPort, socketProvider, socketProvider + "-proxy");
        OptionsByType storageOptions = createCacheServerOptions(clusterPort, -1, socketProvider, socketProvider + "-storage");

        // member1 has storage and extend proxy
        member1 = platform.launch(CoherenceCacheServer.class, extendOptions.asArray());
        member2 = platform.launch(CoherenceCacheServer.class, storageOptions.asArray());

        Eventually.assertDeferred(() -> member1.getClusterSize(), is(2));
        Eventually.assertDeferred(() -> member2.isSafe(), is(true));
        Eventually.assertDeferred(() -> member1.isSafe(), is(true));
        Eventually.assertDeferred(() -> member1.getServiceStatus("Proxy"), is(ServiceStatus.RUNNING));
    }

   // #tag::certs[]
    /**
     * Create the required certificates.
     *
     * @throws Exception if any errors creating certificates.
     */
    @BeforeAll
    public static void setupSSL() throws Exception {
        // only initialize once
        if (tmpDir == null) {
            KeyTool.assertCanCreateKeys();
            
            tmpDir = FileHelper.createTempDir();

            serverCACert = KeyTool.createCACert(tmpDir, "server-ca", "PKCS12");
            serverKeyAndCert = KeyTool.createKeyCertPair(tmpDir, serverCACert, "server");
            clientCACert = KeyTool.createCACert(tmpDir, "client-ca", "PKCS12");
            clientKeyAndCert = KeyTool.createKeyCertPair(tmpDir, clientCACert, "client");
        }
    }
    // #end::certs[]

    /**
     * Shutdown all cache servers.
     */
    @AfterAll
    protected static void _shutdown() {
        CacheFactory.shutdown();
        destroyMember(member1);
        destroyMember(member2);
        member1 = null;
        member2 = null;
    }

    // #tag::test[]
    /**
     * Run a simple test using Coherence*Extend with the given socket-provider to validate
     * that SSL communications for the cluster and proxy are working.
     *
     * @param socketProvider socket provider to use
     */
    protected void runTest(String socketProvider) {
        _startup(socketProvider);

        NamedCache<Integer, String> cache = getCache(socketProvider);
        cache.clear();
        cache.put(1, "one");
        assertEquals("one", cache.get(1));
    }
    // #end::test[]

    // #tag::options[]
    /**
     * Create options to start cache servers.
     *
     * @param clusterPort     cluster port
     * @param proxyPort       proxy port
     * @param socketProvider  socket provider to use
     * @param memberName      member name
     *
     * @return new {@link OptionsByType}
     */
    protected static OptionsByType createCacheServerOptions(int clusterPort, int proxyPort, String socketProvider, String memberName) {
        OptionsByType optionsByType = OptionsByType.empty();

        optionsByType.addAll(JMXManagementMode.ALL,
                JmxProfile.enabled(),
                LocalStorage.enabled(),
                WellKnownAddress.of(hostName),
                Multicast.ttl(0),
                CacheConfig.of(SERVER_CACHE_CONFIG),
                OperationalOverride.of(OVERRIDE),
                Logging.at(6),
                ClusterName.of("ssl-cluster"),
                MemberName.of(memberName),
                SystemProperty.of("test.socket.provider", socketProvider),
                SystemProperty.of("test.server.keystore", serverKeyAndCert.getKeystoreURI()),
                SystemProperty.of("test.trust.keystore", serverCACert.getKeystoreURI()),
                SystemProperty.of("test.server.keystore.password", serverKeyAndCert.storePasswordString()),
                SystemProperty.of("test.server.key.password", serverKeyAndCert.keyPasswordString()),
                SystemProperty.of("test.trust.keystore.password", serverCACert.storePasswordString()),

                SystemProperty.of("test.client.ca.cert", clientCACert.getCertURI()),
                SystemProperty.of("test.server.key", serverKeyAndCert.getKeyPEMNoPassURI()),
                SystemProperty.of("test.server.cert", serverKeyAndCert.getCertURI()),
                SystemProperty.of("test.server.ca.cert", serverCACert.getCertURI()),

                ClusterPort.of(clusterPort));

        // enable proxy server if a proxy port is not -1
        if (proxyPort != -1) {
            optionsByType.addAll(SystemProperty.of("test.extend.address", hostName),
                    SystemProperty.of("test.extend.port", proxyPort),
                    SystemProperty.of("test.proxy.enabled", "true")
            );
        }

        return optionsByType;
    }
    // #end::options[]

    /**
     * Return {@link NamedCache} using Coherence*Extend by setting system properties and
     * using extend-cache-config.xml.
     *
     * @param socketProvider socket provider to use
     * @return a {@link NamedCache}
     */
    protected static NamedCache<Integer, String> getCache(String socketProvider) {
        System.setProperty("coherence.tcmpenabled", "false");
        System.setProperty("coherence.cacheconfig", EXTEND_CACHE_CONFIG);
        System.setProperty("coherence.override", OVERRIDE);
        
        System.setProperty("test.socket.provider", socketProvider);
        System.setProperty("test.extend.address", hostName);
        System.setProperty("test.extend.port", Integer.toString(extendPort));

        System.setProperty("test.server.keystore", serverKeyAndCert.getKeystoreURI());
        System.setProperty("test.trust.keystore", serverCACert.getKeystoreURI());
        System.setProperty("test.server.keystore.password", serverKeyAndCert.storePasswordString());
        System.setProperty("test.server.key.password", serverKeyAndCert.keyPasswordString());
        System.setProperty("test.trust.keystore.password", serverCACert.storePasswordString());
        
        System.setProperty("test.client.ca.cert", clientCACert.getCertURI());
        System.setProperty("test.server.key", serverKeyAndCert.getKeyPEMNoPassURI());
        System.setProperty("test.server.cert", serverKeyAndCert.getCertURI());
        System.setProperty("test.server.ca.cert", serverCACert.getCertURI());

        return CacheFactory.getCache("test-cache");
    }

    /**
     * Destroy/ shutdown a {@link CoherenceClusterMember}.
     *
     * @param member {@link CoherenceClusterMember} to destroy
     */
    protected static void destroyMember(CoherenceClusterMember member) {
        try {
            if (member != null) {
                member.close();
            }
        }
        catch (Throwable thrown) {
            // ignored
        }
    }
}
